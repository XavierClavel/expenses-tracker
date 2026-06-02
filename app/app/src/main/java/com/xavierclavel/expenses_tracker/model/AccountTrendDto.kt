package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountTrendDto(
    val year: Int,
    val month: Int? = null,
    val balance: String,
    val change: String? = null,
    val proportionalChange: String? = null,
)
