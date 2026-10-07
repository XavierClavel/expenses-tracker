package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.BatchTagAction
import com.xavierclavel.dtos.ExpenseBatchIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Expense
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QUser
import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.models.query.QTag
import io.ebean.Paging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ExpenseService: KoinComponent {
    val configuration: Configuration by inject()

    private fun getById(id: Long): Expense =
        QExpense().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.EXPENSE_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify an expense he does not own
     */
    private fun Expense.checkRights(userId: Long): Expense {
        if (this.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_EXPENSE)
        }
        return this
    }

    fun export(userId: Long, expenseId: Long): ExpenseOut =
        getById(expenseId)
            .checkRights(userId)
            .toOutput()

    fun list(
        userId: Long,
        paging: Paging,
        categoryId: Long?,
        subcategoryId: Long?,
        expenseType: ExpenseType?,
        from: LocalDate?,
        to: LocalDate?,
        query: String?,
        minAmount: BigDecimal?,
        maxAmount: BigDecimal?,
        tagId: Long?,
    ): List<ExpenseOut> {
        return QExpense()
            .user.id.eq(userId)
            .apply{
                if (subcategoryId != null) {
                    this.category.id.eq(subcategoryId)
                }
                if (categoryId != null) {
                    this.category.parentCategory.id.eq(categoryId)
                }
                if (tagId != null) {
                    this.tags.id.eq(tagId)
                }
                if (expenseType != null) {
                    this.type.eq(expenseType)
                }
                if (from != null) {
                    this.date.ge(from)
                }
                if (to != null) {
                    this.date.le(to)
                }
                if (!query.isNullOrBlank()) {
                    this.title.icontains(query.trim())
                }
                if (minAmount != null) {
                    this.amount.ge(minAmount)
                }
                if (maxAmount != null) {
                    this.amount.le(maxAmount)
                }
            }
            .setPaging(paging)
            .orderBy().date.desc()
            .findList()
            .map { it.toOutput() }
    }

    fun getOldestActivity(
        userId: Long,
    ): LocalDate? =
        QExpense()
            .user.id.eq(userId)
            .orderBy().date.asc()
            .setMaxRows(1)
            .findList()
            .firstOrNull()
            ?.toOutput()
            ?.date


    fun create(expenseDto: ExpenseIn, userId: Long): ExpenseOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = resolveOwnedSubcategory(expenseDto.categoryId, userId)
        val tags = resolveOwnedTags(expenseDto.tagIds, userId)

        val expense = Expense(
            user = user,
            title = expenseDto.title,
            category = category,
            date = expenseDto.date,
            amount = expenseDto.amount,
            currency = expenseDto.currency,
            type = expenseDto.type,
            tags = tags,
        )
        expense.insert()
        return expense.toOutput()
    }

    fun update(userId: Long, expenseId: Long, expenseDto: ExpenseIn): ExpenseOut {
        val category = resolveOwnedSubcategory(expenseDto.categoryId, userId)
        val tags = resolveOwnedTags(expenseDto.tagIds, userId)

        return getById(expenseId)
            .checkRights(userId)
            .apply {
                this.title = expenseDto.title
                this.category = category
                this.date = expenseDto.date
                this.amount = expenseDto.amount
                this.currency = expenseDto.currency
                this.tags = tags
            }
            .apply { this.update() }
            .toOutput()
    }


    /**
     * Apply a batch update to expenses. Every referenced tag and expense must belong to the
     * user; ownership is validated for the whole request before any change is applied, so a
     * bad operation rejects everything. Extend [ExpenseBatchIn] with more operation lists to
     * grow batch editing without adding endpoints.
     */
    fun batchUpdate(userId: Long, dto: ExpenseBatchIn) {
        val ops = dto.tagOperations.filter { it.expenseIds.isNotEmpty() }
        if (ops.isEmpty()) return

        // Resolve and ownership-check every referenced expense and tag up front.
        val expensesById = QExpense()
            .id.`in`(ops.flatMap { it.expenseIds }.distinct())
            .findList()
            .onEach {
                if (it.user.id != userId) throw ForbiddenException(ForbiddenCause.MUST_OWN_EXPENSE)
            }
            .associateBy { it.id }

        val tagIds = ops.map { it.tagId }.distinct()
        val tagsById = QTag()
            .id.`in`(tagIds)
            .findList()
            .onEach {
                if (it.user.id != userId) throw ForbiddenException(ForbiddenCause.MUST_OWN_TAG)
            }
            .associateBy { it.id }
        if (tagsById.size != tagIds.size) throw NotFoundException(NotFoundCause.TAG_NOT_FOUND)

        // Apply each operation to its expenses.
        ops.forEach { op ->
            val tag = tagsById.getValue(op.tagId)
            op.expenseIds.distinct().forEach expense@{ expenseId ->
                val expense = expensesById[expenseId] ?: return@expense
                when (op.operation) {
                    BatchTagAction.ADD ->
                        if (expense.tags.none { it.id == tag.id }) {
                            expense.tags.add(tag)
                            expense.update()
                        }
                    BatchTagAction.REMOVE ->
                        if (expense.tags.any { it.id == tag.id }) {
                            expense.tags.removeIf { it.id == tag.id }
                            expense.update()
                        }
                }
            }
        }
    }

    /**
     * Copy several expenses onto [targetDate]. Every expense must belong to the user; each
     * copy keeps its title, amount, category and tags.
     */
    fun batchDuplicate(userId: Long, ids: List<Long>, targetDate: LocalDate): List<ExpenseOut> =
        batchDuplicate(userId, ids) { targetDate }

    /**
     * Copy several expenses into [targetMonth]. Every expense must belong to the user; each
     * copy keeps its title, amount, category and tags, and its day of month (clamped to the
     * length of the target month).
     */
    fun batchDuplicate(userId: Long, ids: List<Long>, targetMonth: YearMonth): List<ExpenseOut> =
        batchDuplicate(userId, ids) { targetMonth.atDay(minOf(it.dayOfMonth, targetMonth.lengthOfMonth())) }

    /** Copy several expenses, dating each copy with [copyDate] applied to its original's date. */
    private fun batchDuplicate(userId: Long, ids: List<Long>, copyDate: (LocalDate) -> LocalDate): List<ExpenseOut> {
        if (ids.isEmpty()) return emptyList()
        val expenses = QExpense().id.`in`(ids.distinct()).findList()
        expenses.forEach {
            if (it.user.id != userId) {
                throw ForbiddenException(ForbiddenCause.MUST_OWN_EXPENSE)
            }
        }
        return expenses
            .sortedBy { it.date }
            .map {
                Expense(
                    user = it.user,
                    title = it.title,
                    category = it.category,
                    date = copyDate(it.date),
                    amount = it.amount,
                    currency = it.currency,
                    type = it.type,
                    tags = it.tags.toMutableList(),
                )
                    .apply { insert() }
                    .toOutput()
            }
    }

    //TODO: prevent deletion if expense used
    fun delete(userId: Long, expenseId: Long) {
        val result = getById(expenseId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete expense $expenseId")
        }
    }

    /**
     * Delete several expenses at once. Every expense must belong to the user.
     * The many-to-many tag links are removed by the single-entity delete.
     */
    fun batchDelete(userId: Long, ids: List<Long>) {
        if (ids.isEmpty()) return
        val expenses = QExpense().id.`in`(ids.distinct()).findList()
        expenses.forEach {
            if (it.user.id != userId) {
                throw ForbiddenException(ForbiddenCause.MUST_OWN_EXPENSE)
            }
        }
        expenses.forEach { it.delete() }
    }
}