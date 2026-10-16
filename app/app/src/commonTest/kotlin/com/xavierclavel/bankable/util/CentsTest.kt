package com.xavierclavel.bankable.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Amounts are stored in cents locally and exchanged with the backend as decimal strings. */
class CentsTest {

    @Test
    fun parsesDecimalStrings() {
        assertEquals(1250L, parseCents("12.5"))
        assertEquals(1250L, parseCents("12,50"))
        assertEquals(1200L, parseCents("12"))
        assertEquals(5L, parseCents(".05"))
        assertEquals(-320L, parseCents("-3.20"))
        assertEquals(268L, parseCents("2.675"))
        assertEquals(267L, parseCents("2.674"))
    }

    @Test
    fun rejectsWhatIsNotANumber() {
        assertNull(parseCents(""))
        assertNull(parseCents("."))
        assertNull(parseCents("12a"))
        assertNull(parseCents("1+2"))
    }

    @Test
    fun formatsAsTheBackendDoes() {
        assertEquals("12.50", formatCents(1250))
        assertEquals("0.05", formatCents(5))
        assertEquals("-3.20", formatCents(-320))
        assertEquals("0.00", formatCents(0))
    }
}
