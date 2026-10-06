package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class RecurringExpenseOut(
    val id: Int,
    val title: String,
    val amount: String,
    val currency: String,
    val categoryId: Int?,
    val type: String,
    val tagIds: List<Int> = emptyList(),
    val dayOfMonth: Int,
    // "yyyy-MM-dd" of the next expense the backend will create.
    val nextDate: String,
    // "yyyy-MM-dd" of the last expense created, or of the expense it was created from, or null if
    // there is none yet.
    val lastGeneratedDate: String? = null,
)
