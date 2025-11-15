package com.xavierclavel.dtos

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Represents a category in the system.
 *
 * @property name Display name of the category.
 */
@Serializable
data class ExpenseIn(
    val label: String,
    val categoryId: Long?,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String,
    val date: LocalDate,
)