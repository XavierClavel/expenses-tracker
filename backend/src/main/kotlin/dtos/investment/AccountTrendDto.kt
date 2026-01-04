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
) {
    constructor(year: Int, balance: BigDecimal) : this(
        year = year,
        month = null,
        balance = balance,
    )
}
