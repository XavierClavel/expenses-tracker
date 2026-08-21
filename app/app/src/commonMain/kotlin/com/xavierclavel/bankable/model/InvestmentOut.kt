package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class InvestmentOut(
    val id: Long,
    val amount: String,
    val accountId: Long,
    val type: String,
    val date: String,
)
