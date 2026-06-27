package com.xavierclavel.bankable.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import com.xavierclavel.bankable.R

// The fixed set of account types. The [key] matches the backend AccountType enum
// name (sent/received as a String). Order here drives display & grouping order.
enum class AccountType(
    val key: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    CHECKING("CHECKING", R.string.account_type_checking, Icons.Default.AccountBalanceWallet),
    SAVINGS("SAVINGS", R.string.account_type_savings, Icons.Default.Savings),
    INVESTMENT("INVESTMENT", R.string.account_type_investment, Icons.AutoMirrored.Filled.TrendingUp),
    RETIREMENT("RETIREMENT", R.string.account_type_retirement, Icons.Default.Elderly),
    OTHER("OTHER", R.string.account_type_other, Icons.Default.MoreHoriz);

    companion object {
        fun fromKey(key: String?): AccountType = entries.find { it.key == key } ?: OTHER
    }
}
