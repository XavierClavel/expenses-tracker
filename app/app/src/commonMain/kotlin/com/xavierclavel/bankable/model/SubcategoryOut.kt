package com.xavierclavel.bankable.model

/**
 * @property id The subcategory's uuid, shared with the backend.
 * @property serverId The id the REST API knows the subcategory by: null until it has been synced.
 */
data class SubcategoryOut(
    val id: String,
    val serverId: Int?,
    val name: String,
    val type: String,
    val color: String,
    val icon: String,
    val isDefault: Boolean,
)
