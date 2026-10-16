package com.xavierclavel.services

import com.xavierclavel.dtos.sync.SyncCategory
import com.xavierclavel.dtos.sync.SyncChanges
import com.xavierclavel.dtos.sync.SyncExpense
import com.xavierclavel.dtos.sync.SyncPullOut
import com.xavierclavel.dtos.sync.SyncRecurringExpense
import com.xavierclavel.dtos.sync.SyncSubcategory
import com.xavierclavel.dtos.sync.SyncTag
import com.xavierclavel.enums.SyncEntityType
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Category
import com.xavierclavel.models.Expense
import com.xavierclavel.models.RecurringExpense
import com.xavierclavel.models.Subcategory
import com.xavierclavel.models.SyncTombstone
import com.xavierclavel.models.Tag
import com.xavierclavel.models.User
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QRecurringExpense
import com.xavierclavel.models.query.QSubcategory
import com.xavierclavel.models.query.QSyncTombstone
import com.xavierclavel.models.query.QTag
import com.xavierclavel.models.query.QUser
import io.ebean.DB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import java.util.UUID

/**
 * Keeps the copies devices hold of a user's categories, subcategories, tags, expenses and
 * recurring expenses in sync with the server.
 *
 * Every change to those rows, whichever endpoint or job makes it, takes the next value of the
 * user's revision counter (set by database triggers, see migrations 1.12 and 1.13), and deleting a row
 * leaves a [SyncTombstone]. A device pulls what changed after the last revision it received, and
 * pushes the changes it made locally. The server has the last word: whenever it keeps a version
 * of a row other than the one a device pushed, that row gets a new revision, so that the device
 * pulls the server's version on its next pull.
 */
class SyncService: KoinComponent {
    private val expenseService: ExpenseService by inject()
    private val tagService: TagService by inject()
    private val recurringExpenseService: RecurringExpenseService by inject()

    companion object {
        /** Most changes a single pull returns. */
        const val MAX_PULL_SIZE = 1000
    }

    /**
     * Return the changes made after revision [since], oldest first, at most [limit] of them:
     * the current version of every changed row, and the rows deleted.
     */
    fun pull(userId: Long, since: Long, limit: Int = MAX_PULL_SIZE): SyncPullOut {
        val size = limit.coerceIn(1, MAX_PULL_SIZE)

        // Revisions up to the committed counter all belong to committed changes: a later change
        // waits for the user's row lock before taking a revision, so it can't commit below it.
        val upTo = QUser().select(QUser.Alias.syncRevision).id.eq(userId).findSingleAttribute<Long>()
            ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)

        // Fetch one more row than the page holds from each table, to know whether there are more.
        val categories = QCategory()
            .user.id.eq(userId)
            .revision.gt(since)
            .revision.le(upTo)
            .orderBy().revision.asc()
            .setMaxRows(size + 1)
            .findList()
        val subcategories = QSubcategory()
            .parentCategory.fetch(QCategory.Alias.uuid)
            .user.id.eq(userId)
            .revision.gt(since)
            .revision.le(upTo)
            .orderBy().revision.asc()
            .setMaxRows(size + 1)
            .findList()
        val tags = QTag()
            .user.id.eq(userId)
            .revision.gt(since)
            .revision.le(upTo)
            .orderBy().revision.asc()
            .setMaxRows(size + 1)
            .findList()
        val expenses = QExpense()
            .category.fetch(QSubcategory.Alias.uuid)
            .tags.fetch(QTag.Alias.uuid)
            .user.id.eq(userId)
            .revision.gt(since)
            .revision.le(upTo)
            .orderBy().revision.asc()
            .setMaxRows(size + 1)
            .findList()
        val recurringExpenses = QRecurringExpense()
            .category.fetch(QSubcategory.Alias.uuid)
            .tags.fetch(QTag.Alias.uuid)
            .user.id.eq(userId)
            .revision.gt(since)
            .revision.le(upTo)
            .orderBy().revision.asc()
            .setMaxRows(size + 1)
            .findList()
        val tombstones = QSyncTombstone()
            .user.id.eq(userId)
            .revision.gt(since)
            .revision.le(upTo)
            .orderBy().revision.asc()
            .setMaxRows(size + 1)
            .findList()

        // A user's revisions are unique across tables, so the page can end on any of them.
        val revisions = (
            categories.map { it.revision } +
                subcategories.map { it.revision } +
                tags.map { it.revision } +
                expenses.map { it.revision } +
                recurringExpenses.map { it.revision } +
                tombstones.map { it.revision }
            ).sorted()
        val hasMore = revisions.size > size
        val revision = if (hasMore) revisions[size - 1] else upTo

        return SyncPullOut(
            revision = revision,
            hasMore = hasMore,
            changes = SyncChanges(
                categories = categories.filter { it.revision <= revision }.map { it.toSync() },
                subcategories = subcategories.filter { it.revision <= revision }.map { it.toSync() },
                tags = tags.filter { it.revision <= revision }.map { it.toSync() },
                expenses = expenses.filter { it.revision <= revision }.map { it.toSync() },
                recurringExpenses = recurringExpenses.filter { it.revision <= revision }.map { it.toSync() },
                deletions = tombstones.filter { it.revision <= revision }.map { it.toSync() },
            ),
        )
    }

    /**
     * Apply the changes made on a device, all or nothing. Rows are created or updated parents
     * first, then deleted children first, so a push only needs each row's latest state, in any
     * order. Pushing the same changes again changes nothing.
     *
     * Conflicts are settled here rather than rejected, so that a device's push never fails for
     * good: a deleted row stays deleted, a row can't be deleted while expenses use it, an expense
     * whose subcategory is gone becomes uncategorized and loses the tags that are gone.
     *
     * Recurring expenses create the expenses they have due by [today] as they are pushed.
     *
     * @throws ForbiddenException when a row belongs to another user.
     */
    fun push(userId: Long, changes: SyncChanges, today: LocalDate = LocalDate.now()) {
        DB.beginTransaction().use { tx ->
            // The revision triggers lock the user on every change to their synced rows: locking it
            // first makes concurrent changes wait for the push to end, so what the push checks
            // (is this subcategory used?) still holds when it commits.
            val user = QUser().id.eq(userId).forUpdate().findOne()
                ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)

            val pushedDefaults = changes.subcategories.filter { it.isDefault }.map { it.categoryId }.toSet()
            changes.categories.forEach { upsertCategory(user, it, withDefaultSubcategory = it.id !in pushedDefaults) }
            changes.subcategories.forEach { upsertSubcategory(user, it) }
            changes.tags.forEach { upsertTag(user, it) }
            changes.expenses.forEach { upsertExpense(user, it) }
            changes.recurringExpenses.forEach { upsertRecurringExpense(user, it, today) }

            val deletions = changes.deletions.groupBy({ it.type }, { it.id })
            deletions[SyncEntityType.EXPENSE]?.forEach { deleteExpense(userId, it) }
            deletions[SyncEntityType.RECURRING_EXPENSE]?.forEach { deleteRecurringExpense(userId, it) }
            deletions[SyncEntityType.TAG]?.forEach { deleteTag(userId, it) }
            deletions[SyncEntityType.SUBCATEGORY]?.forEach { deleteSubcategory(userId, it) }
            deletions[SyncEntityType.CATEGORY]?.forEach { deleteCategory(userId, it) }
            tx.commit()
        }
    }

    /**
     * Create or update a category. A new category also gets its default subcategory, unless the
     * push holds one ([withDefaultSubcategory] false).
     */
    private fun upsertCategory(user: User, pushed: SyncCategory, withDefaultSubcategory: Boolean) {
        val category = QCategory().uuid.eq(pushed.id).findOne()
        if (category == null) {
            if (isDeleted(user.id, pushed.id, SyncEntityType.CATEGORY)) return
            val created = Category(
                user = user,
                name = pushed.name,
                color = pushed.color,
                icon = pushed.icon,
                type = pushed.type,
                uuid = pushed.id,
            ).apply { insert() }
            if (withDefaultSubcategory) {
                Subcategory(
                    user = user,
                    name = pushed.name,
                    icon = pushed.icon,
                    type = pushed.type,
                    parentCategory = created,
                    isDefault = true,
                ).insert()
            }
            return
        }
        checkOwner(category.user, user.id, SyncEntityType.CATEGORY)
        category.apply {
            name = pushed.name
            color = pushed.color
            icon = pushed.icon
            update()
        }
        touchIfNot(pushed.copy(serverId = category.id), category.toSync(), SyncEntityType.CATEGORY, category.id)
    }

    /**
     * Create or update a subcategory. A new subcategory is refused when its category is gone, or
     * when it is a second default subcategory for its category. Moving a subcategory to a
     * category that is gone leaves it where it is.
     */
    private fun upsertSubcategory(user: User, pushed: SyncSubcategory) {
        val parent = QCategory().uuid.eq(pushed.categoryId).findOne()
            ?.also { checkOwner(it.user, user.id, SyncEntityType.CATEGORY) }
        val subcategory = QSubcategory().uuid.eq(pushed.id).findOne()
        if (subcategory == null) {
            if (isDeleted(user.id, pushed.id, SyncEntityType.SUBCATEGORY)) return
            if (parent == null || (pushed.isDefault && hasDefaultSubcategory(parent))) {
                refuse(user, SyncEntityType.SUBCATEGORY, pushed.id)
                return
            }
            Subcategory(
                user = user,
                name = pushed.name,
                icon = pushed.icon,
                isDefault = pushed.isDefault,
                type = pushed.type,
                parentCategory = parent,
                uuid = pushed.id,
            ).insert()
            return
        }
        checkOwner(subcategory.user, user.id, SyncEntityType.SUBCATEGORY)
        subcategory.apply {
            name = pushed.name
            icon = pushed.icon
            if (parent != null) parentCategory = parent
            update()
        }
        touchIfNot(pushed.copy(serverId = subcategory.id), subcategory.toSync(), SyncEntityType.SUBCATEGORY, subcategory.id)
    }

    private fun upsertTag(user: User, pushed: SyncTag) {
        val tag = QTag().uuid.eq(pushed.id).findOne()
        if (tag == null) {
            if (isDeleted(user.id, pushed.id, SyncEntityType.TAG)) return
            Tag(user = user, label = pushed.label, uuid = pushed.id).insert()
            return
        }
        checkOwner(tag.user, user.id, SyncEntityType.TAG)
        tag.apply {
            label = pushed.label
            update()
        }
        touchIfNot(pushed.copy(serverId = tag.id), tag.toSync(), SyncEntityType.TAG, tag.id)
    }

    /**
     * Create or update an expense. A subcategory or tags that are gone are dropped from it.
     */
    private fun upsertExpense(user: User, pushed: SyncExpense) {
        val subcategory = existingSubcategory(user.id, pushed.subcategoryId)
        val tags = existingTags(user.id, pushed.tagIds)

        val expense = QExpense().uuid.eq(pushed.id).findOne()
        if (expense == null) {
            if (isDeleted(user.id, pushed.id, SyncEntityType.EXPENSE)) return
            Expense(
                user = user,
                category = subcategory,
                title = pushed.title,
                amount = pushed.amount,
                currency = pushed.currency,
                date = pushed.date,
                type = pushed.type,
                tags = tags,
                uuid = pushed.id,
            ).insert()
            return
        }
        checkOwner(expense.user, user.id, SyncEntityType.EXPENSE)
        expense.apply {
            title = pushed.title
            category = subcategory
            // 12.5 and 12.50 are the same amount: don't count it as a change.
            if (amount.compareTo(pushed.amount) != 0) amount = pushed.amount
            currency = pushed.currency
            date = pushed.date
            if (this.tags.map { it.id }.toSet() != tags.map { it.id }.toSet()) this.tags = tags
            update()
        }
        val stored = expense.toSync()
        touchIfNot(pushed.comparableTo(stored), stored, SyncEntityType.EXPENSE, expense.id)
    }

    /**
     * Create or update a recurring expense, then create the expenses it has due by [today]. A new
     * one starts on the pushed next date: when created offline, its first occurrences may have
     * passed since. Like an expense, it loses a subcategory or tags that are gone. A day of month
     * outside 1..31 refuses a new recurring expense, and is ignored for an existing one.
     */
    private fun upsertRecurringExpense(user: User, pushed: SyncRecurringExpense, today: LocalDate) {
        val subcategory = existingSubcategory(user.id, pushed.subcategoryId)
        val tags = existingTags(user.id, pushed.tagIds)
        val validDay = pushed.dayOfMonth in 1..31

        // Locked like the job creating the expenses due, so that both can't create the same one.
        val recurringExpense = QRecurringExpense().uuid.eq(pushed.id).forUpdate().findOne()
        if (recurringExpense == null) {
            if (isDeleted(user.id, pushed.id, SyncEntityType.RECURRING_EXPENSE)) return
            if (!validDay) {
                refuse(user, SyncEntityType.RECURRING_EXPENSE, pushed.id)
                return
            }
            RecurringExpense(
                user = user,
                category = subcategory,
                title = pushed.title,
                amount = pushed.amount,
                currency = pushed.currency,
                type = pushed.type,
                dayOfMonth = pushed.dayOfMonth,
                nextDate = pushed.nextDate ?: firstOccurrence(pushed.dayOfMonth, today, madeFrom = pushed.lastGeneratedDate),
                lastGeneratedDate = pushed.lastGeneratedDate,
                tags = tags,
                uuid = pushed.id,
            ).apply {
                insert()
                generateUntil(today)
            }
            return
        }
        checkOwner(recurringExpense.user, user.id, SyncEntityType.RECURRING_EXPENSE)
        recurringExpense.apply {
            if (validDay) moveToDay(pushed.dayOfMonth, today)
            title = pushed.title
            category = subcategory
            if (amount.compareTo(pushed.amount) != 0) amount = pushed.amount
            currency = pushed.currency
            type = pushed.type
            if (this.tags.map { it.id }.toSet() != tags.map { it.id }.toSet()) this.tags = tags
            update()
            generateUntil(today)
        }
        val stored = recurringExpense.toSync()
        touchIfNot(pushed.comparableTo(stored), stored, SyncEntityType.RECURRING_EXPENSE, recurringExpense.id)
    }

    /** The subcategory [id] refers to, or null when there is none or it is gone. */
    private fun existingSubcategory(userId: Long, id: UUID?): Subcategory? =
        id?.let { QSubcategory().uuid.eq(it).findOne() }
            ?.also { checkOwner(it.user, userId, SyncEntityType.SUBCATEGORY) }

    /** The tags [ids] refer to, leaving out those that are gone. */
    private fun existingTags(userId: Long, ids: List<UUID>): MutableList<Tag> =
        if (ids.isEmpty()) mutableListOf() else QTag()
            .uuid.isIn(ids.distinct())
            .findList()
            .onEach { checkOwner(it.user, userId, SyncEntityType.TAG) }
            .toMutableList()

    private fun deleteRecurringExpense(userId: Long, uuid: UUID) {
        val recurringExpense = QRecurringExpense().uuid.eq(uuid).findOne() ?: return
        recurringExpenseService.delete(userId = userId, recurringExpenseId = recurringExpense.id)
    }

    private fun deleteExpense(userId: Long, uuid: UUID) {
        val expense = QExpense().uuid.eq(uuid).findOne() ?: return
        expenseService.delete(userId = userId, expenseId = expense.id)
    }

    private fun deleteTag(userId: Long, uuid: UUID) {
        val tag = QTag().uuid.eq(uuid).findOne() ?: return
        tagService.delete(userId = userId, tagId = tag.id)
    }

    /** Delete a subcategory, unless expenses or recurring expenses still use it. */
    private fun deleteSubcategory(userId: Long, uuid: UUID) {
        val subcategory = QSubcategory().uuid.eq(uuid).findOne() ?: return
        checkOwner(subcategory.user, userId, SyncEntityType.SUBCATEGORY)
        if (isUsed(subcategory)) {
            touch(SyncEntityType.SUBCATEGORY, subcategory.id)
            return
        }
        subcategory.delete()
    }

    /** Delete a category and its subcategories, unless expenses or recurring expenses use one. */
    private fun deleteCategory(userId: Long, uuid: UUID) {
        val category = QCategory().uuid.eq(uuid).findOne() ?: return
        checkOwner(category.user, userId, SyncEntityType.CATEGORY)
        val subcategories = QSubcategory().parentCategory.id.eq(category.id).findList()
        if (subcategories.any { isUsed(it) }) {
            touch(SyncEntityType.CATEGORY, category.id)
            subcategories.forEach { touch(SyncEntityType.SUBCATEGORY, it.id) }
            return
        }
        subcategories.forEach { it.delete() }
        category.delete()
    }

    private fun isUsed(subcategory: Subcategory): Boolean =
        QExpense().category.id.eq(subcategory.id).exists() ||
            QRecurringExpense().category.id.eq(subcategory.id).exists()

    private fun hasDefaultSubcategory(category: Category): Boolean =
        QSubcategory().parentCategory.id.eq(category.id).isDefault.eq(true).exists()

    /**
     * True when [uuid] identifies a deleted row: the deletion wins over the pushed change. The
     * tombstone gets a new revision, so that the device pulls the deletion again even if it
     * skipped it while its own change was pending.
     */
    private fun isDeleted(userId: Long, uuid: UUID, type: SyncEntityType): Boolean {
        val tombstone = QSyncTombstone().uuid.eq(uuid).findOne() ?: return false
        checkOwner(tombstone.user, userId, type)
        DB.sqlUpdate("update sync_tombstones set revision = revision where uuid = :uuid")
            .setParameter("uuid", uuid)
            .execute()
        return true
    }

    /** Refuse a row a device created: the device deletes it on its next pull. */
    private fun refuse(user: User, type: SyncEntityType, uuid: UUID) {
        SyncTombstone(user = user, type = type, uuid = uuid).insert()
    }

    /** Give a row a new revision when the server kept a version other than the [pushed] one. */
    private fun <T> touchIfNot(pushed: T, stored: T, type: SyncEntityType, id: Long) {
        if (pushed != stored) touch(type, id)
    }

    /** Give a row a new revision, so that devices pull its current version again. */
    private fun touch(type: SyncEntityType, id: Long) {
        DB.sqlUpdate("update ${type.table} set revision = revision where id = :id")
            .setParameter("id", id)
            .execute()
    }

    private fun checkOwner(owner: User, userId: Long, type: SyncEntityType) {
        if (owner.id == userId) return
        throw ForbiddenException(
            when (type) {
                SyncEntityType.CATEGORY, SyncEntityType.SUBCATEGORY -> ForbiddenCause.MUST_OWN_CATEGORY
                SyncEntityType.TAG -> ForbiddenCause.MUST_OWN_TAG
                SyncEntityType.EXPENSE -> ForbiddenCause.MUST_OWN_EXPENSE
                SyncEntityType.RECURRING_EXPENSE -> ForbiddenCause.MUST_OWN_RECURRING_EXPENSE
            }
        )
    }

    /**
     * The pushed expense as [Expense.toSync] describes it when stored unchanged: with its server
     * id, tags listed once and sorted, the amount at the [stored] scale.
     */
    /** The pushed recurring expense as [RecurringExpense.toSync] describes it when stored unchanged. */
    private fun SyncRecurringExpense.comparableTo(stored: SyncRecurringExpense) = copy(
        serverId = stored.serverId,
        amount = if (amount.compareTo(stored.amount) == 0) stored.amount else amount,
        tagIds = tagIds.distinct().sorted(),
        nextDate = stored.nextDate,
        lastGeneratedDate = stored.lastGeneratedDate,
    )

    private fun SyncExpense.comparableTo(stored: SyncExpense) = copy(
        serverId = stored.serverId,
        amount = if (amount.compareTo(stored.amount) == 0) stored.amount else amount,
        tagIds = tagIds.distinct().sorted(),
    )
}
