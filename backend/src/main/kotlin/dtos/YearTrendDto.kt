package com.xavierclavel.dtos

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal


@Serializable
data class YearTrendDto(
    val year: Int,
    val month: Int? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val median: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val average: BigDecimal,
) {
    constructor(year: Int, total: BigDecimal, median: BigDecimal, average: BigDecimal) : this(
        year = year,
        month = null,
        total = total,
        median = median,
        average = average,
    )
}
