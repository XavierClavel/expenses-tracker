package com.xavierclavel.bankable.data

import com.xavierclavel.bankable.util.lastDayOfMonth
import kotlinx.datetime.LocalDate

// The schedule of a recurring expense, as the backend's RecurringExpenseService computes it. The
// app only uses it for the templates it creates or edits, until the server's dates come back.

/** The date [dayOfMonth] falls on in [year]/[month], clamped to the month's last day. */
internal fun occurrenceIn(year: Int, month: Int, dayOfMonth: Int): LocalDate =
    LocalDate(year, month, minOf(dayOfMonth, lastDayOfMonth(year, month)))

/** The first date on or after [from] that falls on [dayOfMonth] (clamped to the month's last day). */
internal fun nextOccurrence(dayOfMonth: Int, from: LocalDate): LocalDate {
    val candidate = occurrenceIn(from.year, from.month.ordinal + 1, dayOfMonth)
    return if (candidate < from) occurrenceInMonthAfter(from, dayOfMonth) else candidate
}

/**
 * The first occurrence of a new recurring expense: the next [dayOfMonth] on or after [today].
 * One made from an expense dated [madeFrom] counts that expense as its month's occurrence, so it
 * never adds a second expense in that month.
 */
internal fun firstOccurrence(dayOfMonth: Int, today: LocalDate, madeFrom: LocalDate? = null): LocalDate {
    val monthAfter = madeFrom?.let { occurrenceInMonthAfter(it, 1) }
    return nextOccurrence(dayOfMonth, if (monthAfter != null && monthAfter > today) monthAfter else today)
}

/**
 * The next occurrence once the day of month changes to [dayOfMonth], without ever adding a second
 * expense in a month that already got one ([lastGenerated]).
 */
internal fun movedOccurrence(dayOfMonth: Int, today: LocalDate, lastGenerated: LocalDate?): LocalDate {
    val firstAllowed = lastGenerated?.let { occurrenceInMonthAfter(it, 1) }?.takeIf { it > today } ?: today
    return nextOccurrence(dayOfMonth, firstAllowed)
}

private fun occurrenceInMonthAfter(date: LocalDate, dayOfMonth: Int): LocalDate {
    val month = date.month.ordinal + 1
    return if (month == 12) occurrenceIn(date.year + 1, 1, dayOfMonth) else occurrenceIn(date.year, month + 1, dayOfMonth)
}
