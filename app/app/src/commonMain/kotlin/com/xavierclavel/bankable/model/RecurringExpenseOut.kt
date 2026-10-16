package com.xavierclavel.bankable.model

/**
 * @property id The recurring expense's uuid, shared with the backend.
 * @property categoryId The uuid of its subcategory, or null when uncategorized.
 */
data class RecurringExpenseOut(
    val id: String,
    val title: String,
    val amount: String,
    val currency: String,
    val categoryId: String?,
    val type: String,
    val tagIds: List<String> = emptyList(),
    val dayOfMonth: Int,
    // "yyyy-MM-dd" of the next expense the backend will create.
    val nextDate: String,
    // "yyyy-MM-dd" of the last expense created, or of the expense it was created from, or null if
    // there is none yet.
    val lastGeneratedDate: String? = null,
)
