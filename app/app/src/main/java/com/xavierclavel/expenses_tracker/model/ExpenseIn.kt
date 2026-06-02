package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseIn(
    val title: String,
    val amount: String,
    val currency: String,
    val date: String,
    val categoryId: Int?,
    val type: String,
)
