package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Represents a category in the system.
 *
 * @property name Display name of the category.
 */
@Serializable
data class CategoryIn(
    val name: String,
)