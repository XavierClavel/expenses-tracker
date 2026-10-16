package com.xavierclavel.bankable.data

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** The same schedule as the backend's (RecurringExpenseOccurrenceTest), plus the app's own uses. */
class RecurrenceTest {

    private fun date(iso: String) = LocalDate.parse(iso)

    @Test
    fun occurrenceIsClampedToTheLastDayOfShorterMonths() {
        assertEquals(date("2026-01-31"), occurrenceIn(2026, 1, 31))
        assertEquals(date("2026-02-28"), occurrenceIn(2026, 2, 31))
        assertEquals(date("2028-02-29"), occurrenceIn(2028, 2, 30))
        assertEquals(date("2026-04-30"), occurrenceIn(2026, 4, 31))
        assertEquals(date("2026-04-15"), occurrenceIn(2026, 4, 15))
    }

    @Test
    fun nextOccurrenceIncludesTheStartingDay() {
        assertEquals(date("2026-03-10"), nextOccurrence(10, date("2026-03-10")))
    }

    @Test
    fun nextOccurrenceIsLaterThisMonthWhenTheDayIsStillAhead() {
        assertEquals(date("2026-03-20"), nextOccurrence(20, date("2026-03-10")))
    }

    @Test
    fun nextOccurrenceIsNextMonthWhenTheDayHasPassed() {
        assertEquals(date("2026-04-05"), nextOccurrence(5, date("2026-03-10")))
        assertEquals(date("2027-01-05"), nextOccurrence(5, date("2026-12-10")))
    }

    @Test
    fun nextOccurrenceOfALateDayLandsOnTheEndOfAShorterMonth() {
        assertEquals(date("2026-02-28"), nextOccurrence(31, date("2026-02-01")))
        assertEquals(date("2026-02-28"), nextOccurrence(31, date("2026-02-28")))
        assertEquals(date("2026-03-31"), nextOccurrence(31, date("2026-03-01")))
    }

    @Test
    fun aRecurringExpenseMadeFromAnExpenseStartsTheMonthAfter() {
        // Made on the 10th from an expense of the 25th: this month already has its expense.
        assertEquals(date("2026-11-25"), firstOccurrence(25, date("2026-10-10"), madeFrom = date("2026-10-25")))
        // From an old expense, it simply starts at the next occurrence.
        assertEquals(date("2026-10-25"), firstOccurrence(25, date("2026-10-10"), madeFrom = date("2026-01-25")))
        assertEquals(date("2026-10-25"), firstOccurrence(25, date("2026-10-10")))
    }

    @Test
    fun movingTheDayNeverAddsASecondExpenseInAMonth() {
        // This month's expense was created on the 5th: moving to the 20th waits for next month.
        assertEquals(date("2026-11-20"), movedOccurrence(20, date("2026-10-10"), lastGenerated = date("2026-10-05")))
        // Nothing created this month yet: the 20th is still ahead.
        assertEquals(date("2026-10-20"), movedOccurrence(20, date("2026-10-10"), lastGenerated = date("2026-09-05")))
    }
}
