package com.xavierclavel.bankable.model

/**
 * @property categoryId The subcategory's local id, or null for uncategorized expenses.
 * @property categoryName The subcategory's name.
 */
data class CategorySummaryDto(
    val categoryId: String?,
    val categoryName: String,
    val total: String,
)
