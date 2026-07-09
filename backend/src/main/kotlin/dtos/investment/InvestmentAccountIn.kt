package com.xavierclavel.dtos.investment

import com.xavierclavel.enums.AccountTracking
import com.xavierclavel.enums.AccountType
import kotlinx.serialization.Serializable

@Serializable
data class InvestmentAccountIn(
    val name: String,
    val type: AccountType = AccountType.OTHER,
    val tracking: AccountTracking = AccountTracking.CONTRIBUTIONS,
)
