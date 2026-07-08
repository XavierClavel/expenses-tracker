package com.xavierclavel.dtos.investment

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AccountTrendDto(
    val year: Int,
    val month: Int? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val change: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val proportionalChange: BigDecimal?,
    // Cumulative net transfers (deposits − withdrawals) up to and including this
    // period. `balance − contributions` yields the accrued interest.
    @Serializable(with = BigDecimalSerializer::class)
    val contributions: BigDecimal? = null,

    // Modified-Dietz return for this period, as a fraction (0.068 = +6.8%): interest
    // earned ÷ time-weighted average capital (so mid-period transfers are weighted by
    // how long they were invested). Filled in by the service, not the SQL.
    @Serializable(with = BigDecimalSerializer::class)
    val returnRate: BigDecimal? = null,
) {
    constructor(year: Int, balance: BigDecimal, change: BigDecimal?, proportionalChange: BigDecimal?, contributions: BigDecimal?, returnRate: BigDecimal?) : this(
        year = year,
        month = null,
        balance = balance,
        change = change,
        proportionalChange = proportionalChange,
        contributions = contributions,
        returnRate = returnRate,
    )
}
