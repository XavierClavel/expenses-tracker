package com.xavierclavel.bankable.constants

import androidx.compose.ui.graphics.Color
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.color_amber
import com.xavierclavel.bankable.resources.color_beige
import com.xavierclavel.bankable.resources.color_blue
import com.xavierclavel.bankable.resources.color_brown
import com.xavierclavel.bankable.resources.color_coral
import com.xavierclavel.bankable.resources.color_crimson
import com.xavierclavel.bankable.resources.color_cyan
import com.xavierclavel.bankable.resources.color_gray
import com.xavierclavel.bankable.resources.color_green
import com.xavierclavel.bankable.resources.color_indigo
import com.xavierclavel.bankable.resources.color_lightblue
import com.xavierclavel.bankable.resources.color_lightgreen
import com.xavierclavel.bankable.resources.color_lime
import com.xavierclavel.bankable.resources.color_magenta
import com.xavierclavel.bankable.resources.color_navy
import com.xavierclavel.bankable.resources.color_orange
import com.xavierclavel.bankable.resources.color_pink
import com.xavierclavel.bankable.resources.color_purple
import com.xavierclavel.bankable.resources.color_red
import com.xavierclavel.bankable.resources.color_teal
import com.xavierclavel.bankable.resources.color_violet
import com.xavierclavel.bankable.resources.color_yellow
import com.xavierclavel.bankable.util.colorFromHex
import com.xavierclavel.bankable.util.hsvToColor
import com.xavierclavel.bankable.util.toHsv
import org.jetbrains.compose.resources.StringResource

// `label` is the stable identifier persisted with each category — never change
// existing labels or stored data breaks. `nameRes` is the localized display name.
data class AppColor(val label: String, val hex: String, val nameRes: StringResource) {
    val color: Color get() = colorFromHex(hex)

    // [hue (0..360), saturation (0..1), value (0..1)]
    val hsv: FloatArray get() = color.toHsv()
}

val appColors = listOf(
    // Original palette (pastel)
    AppColor("blue", "#009FFF", Res.string.color_blue),
    AppColor("lightblue", "#93FCF8", Res.string.color_lightblue),
    AppColor("purple", "#BDB2FA", Res.string.color_purple),
    AppColor("red", "#F6698A", Res.string.color_red),
    AppColor("yellow", "#E1D481", Res.string.color_yellow),
    AppColor("green", "#B4F1A7", Res.string.color_green),
    AppColor("magenta", "#E193D9", Res.string.color_magenta),
    AppColor("lightgreen", "#EFFFA5", Res.string.color_lightgreen),
    AppColor("brown", "#BCB8A5", Res.string.color_brown),
    AppColor("pink", "#FFA5BA", Res.string.color_pink),
    AppColor("orange", "#E3A663", Res.string.color_orange),
    AppColor("beige", "#E8D6AF", Res.string.color_beige),
    // Added palette (saturated, spread across the hue wheel for clear separation)
    AppColor("teal", "#009688", Res.string.color_teal),
    AppColor("cyan", "#00BCD4", Res.string.color_cyan),
    AppColor("indigo", "#3F51B5", Res.string.color_indigo),
    AppColor("violet", "#9C27B0", Res.string.color_violet),
    AppColor("lime", "#AEEA00", Res.string.color_lime),
    AppColor("amber", "#FFB300", Res.string.color_amber),
    AppColor("crimson", "#E53935", Res.string.color_crimson),
    AppColor("coral", "#FF7043", Res.string.color_coral),
    AppColor("gray", "#9E9E9E", Res.string.color_gray),
    AppColor("navy", "#1A237E", Res.string.color_navy),
)

// Colors below this saturation read as neutral; sorting them by hue scatters
// them through the spectrum, so we group them at the end instead.
private const val NEUTRAL_SATURATION = 0.18f

// Palette ordered for display: chromatic colors by hue (rainbow), then neutral
// (low-saturation) colors grouped at the end, lightest first.
val appColorsRainbow: List<AppColor> = appColors.sortedWith(
    compareBy(
        { it.hsv[1] < NEUTRAL_SATURATION },
        { if (it.hsv[1] < NEUTRAL_SATURATION) -it.hsv[2] else it.hsv[0] },
    )
)

fun colorByName(name: String?): AppColor? = appColors.find { it.label == name }

fun colorHexByName(name: String?): Color =
    colorByName(name)?.color ?: Color.LightGray

// Produces [count] shades of [base] — same hue, walking from light to dark — so a
// set of items (e.g. a category's subcategories) reads as one color family.
// Saturation nudges up slightly for darker shades so they don't go muddy.
fun shadePalette(base: Color, count: Int): List<Color> {
    val hsv = base.toHsv()
    val hue = hsv[0]
    val baseSat = hsv[1]
    return (0 until count).map { i ->
        val t = if (count <= 1) 0.4f else i / (count - 1f)
        val value = 0.92f - 0.42f * t
        val sat = (baseSat + 0.18f * t).coerceIn(0f, 1f)
        hsvToColor(floatArrayOf(hue, sat, value))
    }
}
