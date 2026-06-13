package com.xavierclavel.bankable.constants

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.xavierclavel.bankable.R

// `label` is the stable identifier persisted with each category — never change
// existing labels or stored data breaks. `nameRes` is the localized display name.
data class AppColor(val label: String, val hex: String, @StringRes val nameRes: Int) {
    val color: Color get() = Color(android.graphics.Color.parseColor(hex))

    // [hue (0..360), saturation (0..1), value (0..1)]
    val hsv: FloatArray
        get() = FloatArray(3).also { android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(hex), it) }
}

val appColors = listOf(
    // Original palette (pastel)
    AppColor("blue", "#009FFF", R.string.color_blue),
    AppColor("lightblue", "#93FCF8", R.string.color_lightblue),
    AppColor("purple", "#BDB2FA", R.string.color_purple),
    AppColor("red", "#F6698A", R.string.color_red),
    AppColor("yellow", "#E1D481", R.string.color_yellow),
    AppColor("green", "#B4F1A7", R.string.color_green),
    AppColor("magenta", "#E193D9", R.string.color_magenta),
    AppColor("lightgreen", "#EFFFA5", R.string.color_lightgreen),
    AppColor("brown", "#BCB8A5", R.string.color_brown),
    AppColor("pink", "#FFA5BA", R.string.color_pink),
    AppColor("orange", "#E3A663", R.string.color_orange),
    AppColor("beige", "#E8D6AF", R.string.color_beige),
    // Added palette (saturated, spread across the hue wheel for clear separation)
    AppColor("teal", "#009688", R.string.color_teal),
    AppColor("cyan", "#00BCD4", R.string.color_cyan),
    AppColor("indigo", "#3F51B5", R.string.color_indigo),
    AppColor("violet", "#9C27B0", R.string.color_violet),
    AppColor("lime", "#AEEA00", R.string.color_lime),
    AppColor("amber", "#FFB300", R.string.color_amber),
    AppColor("crimson", "#E53935", R.string.color_crimson),
    AppColor("coral", "#FF7043", R.string.color_coral),
    AppColor("gray", "#9E9E9E", R.string.color_gray),
    AppColor("navy", "#1A237E", R.string.color_navy),
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
