package com.xavierclavel.dtos.dtos

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CategoryTrendDto(
    val year: Int,
    val month: Int? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal,
) {
    constructor(year: Int, total: BigDecimal) : this(
        year = year,
        month = null,
        total = total,
    )
}
