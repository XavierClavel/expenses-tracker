package com.xavierclavel.bankable.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers [ExpressionEvaluator.formatAmount], which was reimplemented without
 * java.math.BigDecimal during the multiplatform migration and has to keep the
 * same half-up rounding and trailing-zero stripping.
 */
class ExpressionEvaluatorTest {

    @Test
    fun formatsWholeAmountsWithoutDecimals() {
        assertEquals("12", ExpressionEvaluator.formatAmount(12.0))
        assertEquals("0", ExpressionEvaluator.formatAmount(0.0))
        assertEquals("36", ExpressionEvaluator.formatAmount(40 * 0.9))
    }

    @Test
    fun stripsOnlyTrailingZeros() {
        assertEquals("12.5", ExpressionEvaluator.formatAmount(12.50))
        assertEquals("0.05", ExpressionEvaluator.formatAmount(0.05))
        assertEquals("1234.56", ExpressionEvaluator.formatAmount(1234.56))
    }

    @Test
    fun roundsHalfUpOnMagnitude() {
        assertEquals("12.35", ExpressionEvaluator.formatAmount(12.345))
        assertEquals("-12.35", ExpressionEvaluator.formatAmount(-12.345))
        // Rounding is applied to the scaled value, not the binary expansion, so
        // a literal half-cent rounds up the way it reads.
        assertEquals("2.68", ExpressionEvaluator.formatAmount(2.675))
        assertEquals("-2.68", ExpressionEvaluator.formatAmount(-2.675))
    }

    @Test
    fun negativeZeroHasNoSign() {
        assertEquals("0", ExpressionEvaluator.formatAmount(-0.001))
    }

    @Test
    fun evaluatesArithmetic() {
        assertEquals(40.0, ExpressionEvaluator.evaluate("120/3"))
        assertEquals(20.5, ExpressionEvaluator.evaluate("12,50+8"))
        assertEquals(36.0, ExpressionEvaluator.evaluate("40*0.9"))
        assertNull(ExpressionEvaluator.evaluate("12+"))
        assertNull(ExpressionEvaluator.evaluate("1/0"))
    }
}
