package com.xavierclavel.bankable.model

/**
 * @property id The category's uuid, shared with the backend.
 */
data class CategoryOut(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val type: String,
    val subcategories: List<SubcategoryOut>,
)
