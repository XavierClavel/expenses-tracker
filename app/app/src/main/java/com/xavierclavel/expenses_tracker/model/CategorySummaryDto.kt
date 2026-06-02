package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class CategorySummaryDto(
    val categoryId: Int,
    val categoryName: String,
    val total: String,
)
