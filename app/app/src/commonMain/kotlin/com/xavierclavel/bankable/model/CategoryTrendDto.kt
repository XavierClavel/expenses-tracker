package com.xavierclavel.bankable.model

data class CategoryTrendDto(
    val year: Int,
    val month: Int? = null,
    val total: String,
    val average: String? = null,
    val median: String? = null,
)
