package com.xavierclavel.bankable.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The date helpers replaced per-screen SimpleDateFormat/Calendar code. The
 * round-trip through UTC millis is what Material's DatePicker relies on.
 */
class DatesTest {

    @Test
    fun convertsIsoDateToUtcMidnightAndBack() {
        val millis = isoDateToUtcMillis("2026-08-19")
        assertEquals(0L, millis % 86_400_000L, "should land on UTC midnight")
        assertEquals("2026-08-19", utcMillisToIsoDate(millis))
    }

    @Test
    fun roundTripsAcrossTheEpochAndLeapDays() {
        for (date in listOf("1970-01-01", "1969-12-31", "2024-02-29", "2000-02-29", "2100-03-01")) {
            assertEquals(date, utcMillisToIsoDate(isoDateToUtcMillis(date)))
        }
    }

    @Test
    fun mapsMidDayMillisOntoTheRightDay() {
        val noon = isoDateToUtcMillis("2026-08-19") + 43_200_000L
        assertEquals("2026-08-19", utcMillisToIsoDate(noon))
    }

    @Test
    fun knowsMonthLengths() {
        assertEquals(31, lastDayOfMonth(2026, 1))
        assertEquals(28, lastDayOfMonth(2026, 2))
        assertEquals(29, lastDayOfMonth(2024, 2))
        assertEquals(28, lastDayOfMonth(2100, 2))
        assertEquals(30, lastDayOfMonth(2026, 4))
        assertEquals(31, lastDayOfMonth(2026, 12))
    }

    @Test
    fun padsToFixedWidth() {
        assertEquals("2026", 2026.padZeros(4))
        assertEquals("07", 7.padZeros(2))
        assertEquals("12", 12.padZeros(2))
    }
}
