package com.xavierclavel.bankable.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.xavierclavel.bankable.R

// The fixed set of account types. The [key] matches the backend AccountType enum
// name (sent/received as a String). Order here drives display & grouping order.
// [baseHue] is the hue (0..360) of the type's color family; -1 means neutral (grey).
enum class AccountType(
    val key: String,
    val labelRes: Int,
    val icon: ImageVector,
    val baseHue: Float,
) {
    CHECKING("CHECKING", R.string.account_type_checking, Icons.Default.AccountBalanceWallet, 210f),
    SAVINGS("SAVINGS", R.string.account_type_savings, Icons.Default.Savings, 135f),
    INVESTMENT("INVESTMENT", R.string.account_type_investment, Icons.AutoMirrored.Filled.TrendingUp, 280f),
    RETIREMENT("RETIREMENT", R.string.account_type_retirement, Icons.Default.Elderly, 35f),
    OTHER("OTHER", R.string.account_type_other, Icons.Default.MoreHoriz, -1f);

    // 10 shades of the type's color family, light→dark. Accounts of this type are
    // assigned a shade by their index within the type (see AccountDistributionScreen).
    val palette: List<Color> by lazy { buildShadePalette(baseHue) }

    // A single light, vivid shade representing the family — used for headers/labels.
    // Kept bright (high value) for strong contrast against the app background.
    val accentColor: Color get() = Color(
        android.graphics.Color.HSVToColor(
            if (baseHue < 0f) floatArrayOf(0f, 0f, 0.72f)
            else floatArrayOf(baseHue, 0.55f, 0.92f),
        ),
    )

    companion object {
        fun fromKey(key: String?): AccountType = entries.find { it.key == key } ?: OTHER
    }
}

private fun buildShadePalette(hue: Float, count: Int = 10): List<Color> =
    (0 until count).map { i ->
        val t = if (count == 1) 0f else i / (count - 1f)
        val hsv = if (hue < 0f) {
            // Neutral: greys varying only in lightness.
            floatArrayOf(0f, 0f, 0.78f - 0.46f * t)
        } else {
            // Same hue, walking from light & soft to dark & saturated.
            floatArrayOf(hue, 0.45f + 0.45f * t, 0.95f - 0.42f * t)
        }
        Color(android.graphics.Color.HSVToColor(hsv))
    }
