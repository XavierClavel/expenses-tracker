package com.xavierclavel.services

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals

class RecurringExpenseOccurrenceTest {

    @Test
    fun `occurrence is clamped to the last day of shorter months`() {
        assertEquals(LocalDate.parse("2026-01-31"), occurrenceIn(YearMonth.parse("2026-01"), 31))
        assertEquals(LocalDate.parse("2026-02-28"), occurrenceIn(YearMonth.parse("2026-02"), 31))
        assertEquals(LocalDate.parse("2028-02-29"), occurrenceIn(YearMonth.parse("2028-02"), 30))
        assertEquals(LocalDate.parse("2026-04-30"), occurrenceIn(YearMonth.parse("2026-04"), 31))
        assertEquals(LocalDate.parse("2026-04-15"), occurrenceIn(YearMonth.parse("2026-04"), 15))
    }

    @Test
    fun `next occurrence includes the starting day`() {
        assertEquals(LocalDate.parse("2026-03-10"), nextOccurrence(10, LocalDate.parse("2026-03-10")))
    }

    @Test
    fun `next occurrence is later this month when the day is still ahead`() {
        assertEquals(LocalDate.parse("2026-03-20"), nextOccurrence(20, LocalDate.parse("2026-03-10")))
    }

    @Test
    fun `next occurrence is next month when the day has passed`() {
        assertEquals(LocalDate.parse("2026-04-05"), nextOccurrence(5, LocalDate.parse("2026-03-10")))
        assertEquals(LocalDate.parse("2027-01-05"), nextOccurrence(5, LocalDate.parse("2026-12-10")))
    }

    @Test
    fun `next occurrence of a late day lands on the end of a shorter month`() {
        assertEquals(LocalDate.parse("2026-02-28"), nextOccurrence(31, LocalDate.parse("2026-02-01")))
        assertEquals(LocalDate.parse("2026-02-28"), nextOccurrence(31, LocalDate.parse("2026-02-28")))
        assertEquals(LocalDate.parse("2026-03-31"), nextOccurrence(31, LocalDate.parse("2026-03-01")))
    }
}
