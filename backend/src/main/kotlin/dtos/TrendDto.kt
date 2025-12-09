package com.xavierclavel.dtos

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TrendDto(
    val year: Int,
    val month: Int? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val totalIncome: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalExpenses: BigDecimal,
)
