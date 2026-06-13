package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountReportOut(
    val id: Int,
    val amount: String,
    val date: String,
)
