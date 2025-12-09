package com.xavierclavel.dtos.summary

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Represents a category in the system.
 *
 * @property name Display name of the category.
 */
@Serializable
data class SummaryDto(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val totalExpenses: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalIncome: BigDecimal,
    val expensesByCategory: List<CategorySummary>,
    val incomeByCategory: List<CategorySummary>,
)