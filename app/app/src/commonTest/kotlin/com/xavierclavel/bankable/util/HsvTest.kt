package com.xavierclavel.bankable.util

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The HSV helpers replaced android.graphics.Color.colorToHSV / HSVToColor, which
 * the category and account palettes are built on.
 */
class HsvTest {

    @Test
    fun parsesPaletteHexColors() {
        assertEquals(Color(0xFF009FFF), colorFromHex("#009FFF"))
        assertEquals(Color(0xFF009FFF), colorFromHex("#FF009FFF"))
        assertEquals(Color.LightGray, colorFromHex("not-a-color"))
    }

    @Test
    fun convertsKnownColorsToHsv() {
        assertHsv(floatArrayOf(0f, 1f, 1f), Color.Red.toHsv())
        assertHsv(floatArrayOf(120f, 1f, 1f), Color.Green.toHsv())
        assertHsv(floatArrayOf(240f, 1f, 1f), Color.Blue.toHsv())
        assertHsv(floatArrayOf(0f, 0f, 1f), Color.White.toHsv())
        assertHsv(floatArrayOf(0f, 0f, 0f), Color.Black.toHsv())
    }

    @Test
    fun roundTripsThroughHsv() {
        for (hex in listOf("#009FFF", "#B4F1A7", "#E3A663", "#1A237E", "#9E9E9E")) {
            val original = colorFromHex(hex)
            val roundTripped = hsvToColor(original.toHsv())
            assertTrue(
                abs(original.red - roundTripped.red) < 0.01f &&
                    abs(original.green - roundTripped.green) < 0.01f &&
                    abs(original.blue - roundTripped.blue) < 0.01f,
                "$hex did not survive the HSV round trip (got $roundTripped)",
            )
        }
    }

    @Test
    fun clampsOutOfRangeInput() {
        assertEquals(Color.White, hsvToColor(floatArrayOf(0f, -1f, 2f)))
        // Hue wraps rather than clamping, so 480° is 120° (green).
        assertHsv(floatArrayOf(120f, 1f, 1f), hsvToColor(floatArrayOf(480f, 1f, 1f)).toHsv())
    }

    private fun assertHsv(expected: FloatArray, actual: FloatArray) {
        assertEquals(expected[0], actual[0], absoluteTolerance = 0.5f)
        assertEquals(expected[1], actual[1], absoluteTolerance = 0.01f)
        assertEquals(expected[2], actual[2], absoluteTolerance = 0.01f)
    }
}
