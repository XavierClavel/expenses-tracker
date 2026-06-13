package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class CategorySummaryDto(
    val categoryId: Int,
    val categoryName: String,
    val total: String,
)
