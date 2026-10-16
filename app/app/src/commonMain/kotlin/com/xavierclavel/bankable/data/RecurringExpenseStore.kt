package com.xavierclavel.bankable.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.xavierclavel.bankable.data.db.SelectWithTags
import com.xavierclavel.bankable.model.RecurringExpenseOut
import com.xavierclavel.bankable.util.formatCents
import com.xavierclavel.bankable.util.todayIsoDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The recurring expenses held locally. The server creates the expenses they have due, which come
 * back with the next pull; the app only computes a template's next date until the server's does.
 */
object RecurringExpenseStore {

    /** Every recurring expense, by day of month. Emits again whenever they change. */
    fun observe(): Flow<List<RecurringExpenseOut>> =
        database.recurringExpenseQueries.selectWithTags()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toOut() } }

    /**
     * Creates a recurring expense ([id] null) or updates one.
     *
     * @return true when a new one is due today: the server creates its expense once it syncs.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun save(
        id: String?,
        title: String,
        amountCents: Long,
        currency: String,
        type: String,
        subcategoryId: String?,
        tagIds: Collection<String>,
        dayOfMonth: Int,
    ): Boolean {
        val today = LocalDate.parse(todayIsoDate())
        val dueToday = writeLocal {
            val stored = id?.let { recurringExpenseQueries.selectById(it).executeAsOneOrNull() }
            val nextDate = when {
                stored == null -> firstOccurrence(dayOfMonth, today)
                stored.day_of_month.toInt() == dayOfMonth -> LocalDate.parse(stored.next_date)
                else -> movedOccurrence(dayOfMonth, today, stored.last_generated_date?.let { LocalDate.parse(it) })
            }
            val recurringId = id ?: Uuid.random().toString()
            upsertRecurringExpense(
                id = recurringId,
                serverId = null,
                subcategoryId = subcategoryId,
                title = title,
                amountCents = amountCents,
                currency = currency,
                type = type,
                dayOfMonth = dayOfMonth,
                nextDate = nextDate.toString(),
                lastGeneratedDate = stored?.last_generated_date,
                tagIds = tagIds,
            )
            markPending(SyncType.RECURRING_EXPENSE, recurringId)
            stored == null && nextDate <= today
        }
        Sync.requestSync()
        return dueToday
    }

    /**
     * Makes each expense in [expenseIds] recurring, on its day of month, with its title, amount,
     * category and tags. Each counts as its month's occurrence, so that month gets no second one.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun createFromExpenses(expenseIds: Collection<String>) {
        val today = LocalDate.parse(todayIsoDate())
        writeLocal {
            expenseQueries.selectByIds(expenseIds).executeAsList().forEach { expense ->
                val date = LocalDate.parse(expense.date)
                val recurringId = Uuid.random().toString()
                upsertRecurringExpense(
                    id = recurringId,
                    serverId = null,
                    subcategoryId = expense.subcategory_id,
                    title = expense.title,
                    amountCents = expense.amount_cents,
                    currency = expense.currency,
                    type = expense.type,
                    dayOfMonth = date.day,
                    nextDate = firstOccurrence(date.day, today, madeFrom = date).toString(),
                    lastGeneratedDate = expense.date,
                    tagIds = expenseQueries.selectTagIds(expense.id).executeAsList(),
                )
                markPending(SyncType.RECURRING_EXPENSE, recurringId)
            }
        }
        Sync.requestSync()
    }

    /** Deletes a recurring expense. The expenses it created are kept. */
    suspend fun delete(id: String) {
        writeLocal {
            deleteRecurringExpense(id)
            markPending(SyncType.RECURRING_EXPENSE, id, deleted = true)
        }
        Sync.requestSync()
    }

    private fun SelectWithTags.toOut() = RecurringExpenseOut(
        id = id,
        title = title,
        amount = formatCents(amount_cents),
        currency = currency,
        categoryId = subcategory_id,
        type = type,
        tagIds = tag_ids?.split(",").orEmpty(),
        dayOfMonth = day_of_month.toInt(),
        nextDate = next_date,
        lastGeneratedDate = last_generated_date,
    )
}
