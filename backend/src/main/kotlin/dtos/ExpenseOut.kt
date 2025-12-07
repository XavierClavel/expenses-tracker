package com.xavierclavel.dtos

import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.BigDecimalSerializer
import com.xavierclavel.utils.LocalDateSerializer

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

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
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val type: ExpenseType,
)