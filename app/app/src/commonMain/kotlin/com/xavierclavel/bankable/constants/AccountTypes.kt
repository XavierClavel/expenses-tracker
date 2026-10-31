package com.xavierclavel.bankable.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.account_type_checking
import com.xavierclavel.bankable.resources.account_type_investment
import com.xavierclavel.bankable.resources.account_type_other
import com.xavierclavel.bankable.resources.account_type_retirement
import com.xavierclavel.bankable.resources.account_type_savings
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.TransferBlue
import com.xavierclavel.bankable.util.hsvToColor
import org.jetbrains.compose.resources.StringResource

// The fixed set of account types. The [key] matches the backend AccountType enum
// name (sent/received as a String). Order here drives display & grouping order.
// [baseHue] is the hue (0..360) of the type's color family; -1 means neutral (grey).
// [accentColor] is the type's single representative color in the Night desk palette.
enum class AccountType(
    val key: String,
    val labelRes: StringResource,
    val icon: ImageVector,
    val baseHue: Float,
    val accentColor: Color,
) {
    CHECKING("CHECKING", Res.string.account_type_checking, Icons.Outlined.CreditCard, -1f, Color(0xFFA7AFBC)),
    SAVINGS("SAVINGS", Res.string.account_type_savings, Icons.Outlined.Inventory2, 207f, TransferBlue),
    INVESTMENT("INVESTMENT", Res.string.account_type_investment, Icons.AutoMirrored.Outlined.TrendingUp, 158f, Mint),
    RETIREMENT("RETIREMENT", Res.string.account_type_retirement, Icons.Outlined.Schedule, 255f, Color(0xFFC4B5FD)),
    OTHER("OTHER", Res.string.account_type_other, Icons.Outlined.MoreHoriz, -1f, Color(0xFF7C8594));

    // 10 shades of the type's color family, light→dark. Accounts of this type are
    // assigned a shade by their index within the type (see AccountDistributionScreen).
    val palette: List<Color> by lazy { buildShadePalette(baseHue) }

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
        hsvToColor(hsv)
    }
