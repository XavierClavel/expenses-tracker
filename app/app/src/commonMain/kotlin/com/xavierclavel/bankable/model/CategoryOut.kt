package com.xavierclavel.bankable.model

/**
 * @property id The category's uuid, shared with the backend.
 * @property serverId The id the REST API knows the category by: null until it has been synced.
 */
data class CategoryOut(
    val id: String,
    val serverId: Int?,
    val name: String,
    val color: String,
    val icon: String,
    val type: String,
    val subcategories: List<SubcategoryOut>,
)
