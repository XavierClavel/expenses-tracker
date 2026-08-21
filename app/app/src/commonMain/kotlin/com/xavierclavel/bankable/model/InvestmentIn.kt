package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

// A declared transfer of money into ("IN") or out of ("OUT") an account.
@Serializable
data class InvestmentIn(
    val amount: String,
    val accountId: Long,
    val type: String,
    val date: String,
)
