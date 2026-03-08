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
) {
    constructor(year: Int, balance: BigDecimal, change: BigDecimal?, proportionalChange: BigDecimal?) : this(
        year = year,
        month = null,
        balance = balance,
        change = change,
        proportionalChange = proportionalChange,
    )
}
