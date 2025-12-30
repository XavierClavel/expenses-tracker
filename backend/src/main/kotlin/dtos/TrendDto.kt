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
    @Serializable(with = BigDecimalSerializer::class)
    val meanIncome: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val meanExpenses: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val medianIncome: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val medianExpenses: BigDecimal? = null,

    ) {
    constructor(year: Int, totalIncome: BigDecimal, totalExpenses: BigDecimal) : this(
        year = year,
        month = null,
        totalIncome = totalIncome,
        totalExpenses = totalExpenses,
    )

    constructor(
        year: Int,
        totalIncome: BigDecimal,
        totalExpenses: BigDecimal,
        meanIncome: BigDecimal,
        meanExpenses: BigDecimal,
        medianIncome: BigDecimal,
        medianExpenses: BigDecimal,
        ) : this(
        year = year,
        month = null,
        totalIncome = totalIncome,
        totalExpenses = totalExpenses,
        meanIncome = totalIncome,
        meanExpenses = totalExpenses,
        medianIncome = medianIncome,
        medianExpenses = medianExpenses,
    )
}
