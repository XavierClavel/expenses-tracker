package com.xavierclavel.bankable.constants

import androidx.compose.ui.graphics.Color

data class AppColor(val label: String, val hex: String) {
    val color: Color get() = Color(android.graphics.Color.parseColor(hex))
}

val appColors = listOf(
    AppColor("blue", "#009FFF"),
    AppColor("lightblue", "#93FCF8"),
    AppColor("purple", "#BDB2FA"),
    AppColor("red", "#f6698a"),
    AppColor("yellow", "#e1d481"),
    AppColor("green", "#b4f1a7"),
    AppColor("magenta", "#e193d9"),
    AppColor("lightgreen", "#efffa5"),
    AppColor("brown", "#bcb8a5"),
    AppColor("pink", "#FFA5BA"),
    AppColor("orange", "#e3a663"),
    AppColor("beige", "#e8d6af"),
)

fun colorByName(name: String?): AppColor? = appColors.find { it.label == name }

fun colorHexByName(name: String?): Color =
    colorByName(name)?.color ?: Color.LightGray
