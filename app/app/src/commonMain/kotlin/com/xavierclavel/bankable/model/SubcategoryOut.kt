package com.xavierclavel.bankable.model

/**
 * @property id The subcategory's uuid, shared with the backend.
 */
data class SubcategoryOut(
    val id: String,
    val name: String,
    val type: String,
    val color: String,
    val icon: String,
    val isDefault: Boolean,
)
