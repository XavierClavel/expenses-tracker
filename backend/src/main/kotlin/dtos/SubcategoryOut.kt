package com.xavierclavel.dtos

import com.xavierclavel.enums.ExpenseType
import kotlinx.serialization.Serializable

/**
 * Represents a category in the system.
 *
 * @property name Display name of the category.
 */
@Serializable
data class SubcategoryOut(
    val id: Long,
    val name: String,
    val type: ExpenseType,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
)