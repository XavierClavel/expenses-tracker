package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryTrendDto(
    val year: Int,
    val month: Int? = null,
    val total: String,
    val average: String,
    val median: String,
)
