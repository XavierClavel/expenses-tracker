package com.xavierclavel.bankable.util

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * HSV conversions, replacing the android.graphics.Color helpers the palette code
 * used to call. Hue is 0..360, saturation and value 0..1 — the same convention
 * android.graphics uses, so the palette math is unchanged.
 */

/** [hue (0..360), saturation (0..1), value (0..1)] for an opaque color. */
fun Color.toHsv(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min

    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    val saturation = if (max == 0f) 0f else delta / max
    return floatArrayOf(hue, saturation, max)
}

/** Opaque color for [hue (0..360), saturation (0..1), value (0..1)]. */
fun hsvToColor(hsv: FloatArray): Color {
    val h = hsv[0].mod(360f)
    val s = hsv[1].coerceIn(0f, 1f)
    val v = hsv[2].coerceIn(0f, 1f)

    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(red = r + m, green = g + m, blue = b + m, alpha = 1f)
}

/** Parses "#RRGGBB" / "#AARRGGBB", the format the palette stores. */
fun colorFromHex(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return Color.LightGray
    return when (cleaned.length) {
        6 -> Color(value or 0xFF000000L)
        8 -> Color(value)
        else -> Color.LightGray
    }
}
