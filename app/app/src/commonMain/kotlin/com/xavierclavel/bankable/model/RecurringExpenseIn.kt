package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class RecurringExpenseIn(
    val title: String,
    val amount: String,
    val currency: String,
    val categoryId: Int?,
    val type: String,
    val tagIds: List<Int> = emptyList(),
    // 1..31; in shorter months the expense lands on the last day.
    val dayOfMonth: Int,
)
