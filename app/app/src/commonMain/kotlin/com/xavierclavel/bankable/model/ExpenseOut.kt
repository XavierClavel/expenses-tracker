package com.xavierclavel.bankable.model

/**
 * @property id The expense's uuid, shared with the backend.
 * @property categoryId The uuid of the expense's subcategory, or null when uncategorized.
 */
data class ExpenseOut(
    val id: String,
    val title: String,
    val amount: String,
    val currency: String,
    val date: String,
    val categoryId: String?,
    val type: String,
    val tagIds: List<String> = emptyList(),
)
