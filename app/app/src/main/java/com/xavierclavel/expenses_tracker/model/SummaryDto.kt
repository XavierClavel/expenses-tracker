package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class SummaryDto(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val totalExpenses: String,
    val totalIncome: String,
    val expensesByCategory: List<CategorySummaryDto>,
    val incomeByCategory: List<CategorySummaryDto>,
)
