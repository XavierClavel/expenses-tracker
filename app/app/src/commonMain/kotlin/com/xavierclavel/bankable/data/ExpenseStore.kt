package com.xavierclavel.bankable.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.xavierclavel.bankable.data.db.Search
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.util.formatCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** The expenses held locally. Every change is recorded for the next push and requests a sync. */
object ExpenseStore {

    /**
     * The expenses matching every non-null filter, newest first, at most [limit] of them. Emits
     * again whenever they change.
     *
     * @param categoryId Keeps the expenses of this category's subcategories.
     * @param query Keeps the expenses whose title contains it, ignoring case.
     */
    fun observe(
        categoryId: String? = null,
        subcategoryId: String? = null,
        type: String? = null,
        from: String? = null,
        to: String? = null,
        query: String? = null,
        minCents: Long? = null,
        maxCents: Long? = null,
        tagId: String? = null,
        limit: Int,
    ): Flow<List<ExpenseOut>> =
        database.expenseQueries.search(
            category_id = categoryId,
            subcategory_id = subcategoryId,
            type = type,
            from_date = from,
            to_date = to,
            title_pattern = query?.trim()?.takeIf { it.isNotEmpty() }?.let { "%${it.escapeLike()}%" },
            min_cents = minCents,
            max_cents = maxCents,
            tag_id = tagId,
            limit = limit.toLong(),
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toOut() } }

    /** Creates an expense ([id] null) or updates one. An expense's type can't change once created. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun save(
        id: String?,
        title: String,
        amountCents: Long,
        currency: String,
        date: String,
        subcategoryId: String?,
        type: String,
        tagIds: Collection<String>,
    ) {
        writeLocal {
            val expenseId = id ?: Uuid.random().toString()
            val storedType = id?.let { expenseQueries.selectById(it).executeAsOneOrNull()?.type }
            upsertExpense(
                id = expenseId,
                serverId = null,
                subcategoryId = subcategoryId,
                title = title,
                amountCents = amountCents,
                currency = currency,
                date = date,
                type = storedType ?: type,
                tagIds = tagIds,
            )
            markPending(SyncType.EXPENSE, expenseId)
        }
        Sync.requestSync()
    }

    suspend fun delete(ids: Collection<String>) {
        writeLocal {
            ids.forEach {
                deleteExpense(it)
                markPending(SyncType.EXPENSE, it, deleted = true)
            }
        }
        Sync.requestSync()
    }

    /** Adds ([add] true) or removes [tagId] on every expense in [ids]. */
    suspend fun setTag(ids: Collection<String>, tagId: String, add: Boolean) {
        writeLocal {
            ids.forEach {
                if (add) expenseQueries.addTag(it, tagId) else expenseQueries.removeTag(it, tagId)
                markPending(SyncType.EXPENSE, it)
            }
        }
        Sync.requestSync()
    }

    /** Copies every expense in [ids] onto [date] ("yyyy-MM-dd"), with its title, amount, category and tags. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun duplicate(ids: Collection<String>, date: String) {
        writeLocal {
            expenseQueries.selectByIds(ids).executeAsList().forEach { expense ->
                val copyId = Uuid.random().toString()
                upsertExpense(
                    id = copyId,
                    serverId = null,
                    subcategoryId = expense.subcategory_id,
                    title = expense.title,
                    amountCents = expense.amount_cents,
                    currency = expense.currency,
                    date = date,
                    type = expense.type,
                    tagIds = expenseQueries.selectTagIds(expense.id).executeAsList(),
                )
                markPending(SyncType.EXPENSE, copyId)
            }
        }
        Sync.requestSync()
    }

    private fun Search.toOut() = ExpenseOut(
        id = id,
        serverId = server_id?.toInt(),
        title = title,
        amount = formatCents(amount_cents),
        currency = currency,
        date = date,
        categoryId = subcategory_id,
        type = type,
        tagIds = tag_ids?.split(",").orEmpty(),
    )

    /** Escapes LIKE's wildcards, so that "50%" searches for a literal "50%". */
    private fun String.escapeLike() = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
