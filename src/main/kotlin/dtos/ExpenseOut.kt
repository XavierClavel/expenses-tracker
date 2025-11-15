package com.xavierclavel.dtos

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Represents a category in the system.
 *
 * @property name Display name of the category.
 */
@Serializable
data class ExpenseOut(
    val id: Long,
    val label: String,
    val categoryId: Long?,
    val amount: Double,
    val currency: String,
    val date: LocalDate,
)