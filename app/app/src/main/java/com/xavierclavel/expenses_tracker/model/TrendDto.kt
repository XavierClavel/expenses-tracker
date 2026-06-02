package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class TrendDto(
    val year: Int,
    val month: Int? = null,
    val totalIncome: String,
    val totalExpenses: String,
    val meanIncome: String? = null,
    val meanExpenses: String? = null,
    val medianIncome: String? = null,
    val medianExpenses: String? = null,
)
