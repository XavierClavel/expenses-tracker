package com.xavierclavel.expenses_tracker.model

data class CategoryOut(
    val id: Int,
    val name: String,
    val color: String,
    val icon: String,
    val type: String,
    val subcategories: List<SubcategoryOut>,
)
