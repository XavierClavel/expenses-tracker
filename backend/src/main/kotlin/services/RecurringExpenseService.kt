package com.xavierclavel.services

import com.xavierclavel.dtos.RecurringExpenseIn
import com.xavierclavel.dtos.RecurringExpenseOut
import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.RecurringExpense
import com.xavierclavel.models.query.QRecurringExpense
import com.xavierclavel.models.query.QUser
import io.ebean.DB
import org.koin.core.component.KoinComponent
import java.time.LocalDate
import java.time.YearMonth

/** The date [dayOfMonth] falls on in [month], clamped to the month's last day. */
internal fun occurrenceIn(month: YearMonth, dayOfMonth: Int): LocalDate =
    month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))

/** The first date on or after [from] that falls on [dayOfMonth] (clamped to the month's last day). */
internal fun nextOccurrence(dayOfMonth: Int, from: LocalDate): LocalDate {
    val month = YearMonth.from(from)
    val candidate = occurrenceIn(month, dayOfMonth)
    return if (candidate.isBefore(from)) occurrenceIn(month.plusMonths(1), dayOfMonth) else candidate
}

class RecurringExpenseService: KoinComponent {

    private fun getById(id: Long): RecurringExpense =
        QRecurringExpense().id.eq(id).findOne()
            ?: throw NotFoundException(NotFoundCause.RECURRING_EXPENSE_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify a recurring expense he does not own
     */
    private fun RecurringExpense.checkRights(userId: Long): RecurringExpense {
        if (this.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_RECURRING_EXPENSE)
        }
        return this
    }

    private fun validate(dto: RecurringExpenseIn) {
        if (dto.dayOfMonth !in 1..31) {
            throw BadRequestException(BadRequestCause.INVALID_DAY_OF_MONTH)
        }
    }

    /**
     * Create an expense for every occurrence up to [today], advancing [RecurringExpense.nextDate]
     * past it. Several occurrences are created at once when some were missed (e.g. downtime).
     * Must run inside a transaction holding a lock on this row, so that two concurrent runs
     * cannot both create the same occurrence.
     */
    private fun RecurringExpense.generateUntil(today: LocalDate): Int {
        var created = 0
        while (!nextDate.isAfter(today)) {
            toExpense(nextDate).insert()
            lastGeneratedDate = nextDate
            nextDate = occurrenceIn(YearMonth.from(nextDate).plusMonths(1), dayOfMonth)
            created++
        }
        if (created > 0) update()
        return created
    }

    /**
     * Create the expenses of every recurring expense, of every user, that came due on or
     * before [today]. Safe to call repeatedly: an occurrence is only ever created once.
     *
     * @return the number of expenses created.
     */
    fun generateDueExpenses(today: LocalDate = LocalDate.now()): Int =
        DB.beginTransaction().use { tx ->
            val created = QRecurringExpense()
                .nextDate.le(today)
                .orderBy().id.asc()
                .forUpdate()
                .findList()
                .sumOf { it.generateUntil(today) }
            tx.commit()
            created
        }

    fun export(userId: Long, recurringExpenseId: Long): RecurringExpenseOut =
        getById(recurringExpenseId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long): List<RecurringExpenseOut> =
        QRecurringExpense()
            .user.id.eq(userId)
            .orderBy().dayOfMonth.asc()
            .orderBy().title.asc()
            .findList()
            .map { it.toOutput() }

    /**
     * Create a recurring expense. Its first occurrence is the next [RecurringExpenseIn.dayOfMonth]
     * on or after [today]; when that is [today] itself, the expense is created right away.
     */
    fun create(dto: RecurringExpenseIn, userId: Long, today: LocalDate = LocalDate.now()): RecurringExpenseOut {
        validate(dto)
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val category = resolveOwnedSubcategory(dto.categoryId, userId)
        val tags = resolveOwnedTags(dto.tagIds, userId)

        val recurringExpense = RecurringExpense(
            user = user,
            category = category,
            title = dto.title,
            amount = dto.amount,
            currency = dto.currency,
            type = dto.type,
            dayOfMonth = dto.dayOfMonth,
            nextDate = nextOccurrence(dto.dayOfMonth, today),
            tags = tags,
        )
        DB.beginTransaction().use { tx ->
            recurringExpense.insert()
            recurringExpense.generateUntil(today)
            tx.commit()
        }
        return recurringExpense.toOutput()
    }

    /**
     * Update a recurring expense. Expenses it already created are left untouched. When the
     * day of month changes, the next occurrence moves to the new day, without ever creating
     * a second expense in a month that already got one.
     */
    fun update(
        userId: Long,
        recurringExpenseId: Long,
        dto: RecurringExpenseIn,
        today: LocalDate = LocalDate.now(),
    ): RecurringExpenseOut {
        validate(dto)
        val category = resolveOwnedSubcategory(dto.categoryId, userId)
        val tags = resolveOwnedTags(dto.tagIds, userId)

        val recurringExpense = DB.beginTransaction().use { tx ->
            val recurringExpense = QRecurringExpense()
                .id.eq(recurringExpenseId)
                .forUpdate()
                .findOne()
                ?.checkRights(userId)
                ?: throw NotFoundException(NotFoundCause.RECURRING_EXPENSE_NOT_FOUND)
            recurringExpense.apply {
                if (dto.dayOfMonth != dayOfMonth) {
                    val firstAllowed = lastGeneratedDate
                        ?.let { YearMonth.from(it).plusMonths(1).atDay(1) }
                        ?.takeIf { it.isAfter(today) }
                        ?: today
                    nextDate = nextOccurrence(dto.dayOfMonth, firstAllowed)
                }
                title = dto.title
                this.category = category
                amount = dto.amount
                currency = dto.currency
                type = dto.type
                dayOfMonth = dto.dayOfMonth
                this.tags = tags
                update()
                generateUntil(today)
            }
            tx.commit()
            recurringExpense
        }
        return recurringExpense.toOutput()
    }

    /**
     * Delete a recurring expense. The expenses it already created are kept.
     */
    fun delete(userId: Long, recurringExpenseId: Long) {
        val result = getById(recurringExpenseId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete recurring expense $recurringExpenseId")
        }
    }
}
