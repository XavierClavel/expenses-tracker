package com.xavierclavel.bankable.model

/**
 * @property id The expense's uuid, shared with the backend.
 * @property serverId The id the REST API knows the expense by: null until it has been synced.
 * @property categoryId The uuid of the expense's subcategory, or null when uncategorized.
 */
data class ExpenseOut(
    val id: String,
    val serverId: Int?,
    val title: String,
    val amount: String,
    val currency: String,
    val date: String,
    val categoryId: String?,
    val type: String,
    val tagIds: List<String> = emptyList(),
)
