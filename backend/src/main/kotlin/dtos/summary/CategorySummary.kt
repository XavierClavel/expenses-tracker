package com.xavierclavel.dtos.summary

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal


@Serializable
data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal,
)