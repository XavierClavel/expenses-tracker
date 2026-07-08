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
) {
    constructor(year: Int, balance: BigDecimal, change: BigDecimal?, proportionalChange: BigDecimal?, contributions: BigDecimal?) : this(
        year = year,
        month = null,
        balance = balance,
        change = change,
        proportionalChange = proportionalChange,
        contributions = contributions,
    )
}
