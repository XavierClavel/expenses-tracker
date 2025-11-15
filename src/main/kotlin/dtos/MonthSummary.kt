package com.xavierclavel.dtos

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Represents a category in the system.
 *
 * @property name Display name of the category.
 */
@Serializable
data class MonthSummary(
    val year: Int,
    val month: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val totalExpenses: BigDecimal,
    val byCategory: List<CategorySummary>,
)


@Serializable
data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal,
)