package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountReportIn(
    val amount: String,
    val date: String,
)
