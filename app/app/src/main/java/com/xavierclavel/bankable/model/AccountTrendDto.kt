package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountTrendDto(
    val year: Int,
    val month: Int? = null,
    val balance: String,
    val change: String? = null,
    val proportionalChange: String? = null,
    // Cumulative net transfers up to this period. interests = balance − contributions.
    val contributions: String? = null,
    // Modified-Dietz return for this period as a fraction (e.g. "0.068" = +6.8%).
    val returnRate: String? = null,
)
