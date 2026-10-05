package com.xavierclavel.dtos

import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Request body used to create or update a recurring expense.
 *
 * @property dayOfMonth Day of the month (1 to 31) on which an expense is created. In months
 * shorter than that, the expense is created on the last day of the month.
 */
@Serializable
data class RecurringExpenseIn(
    val title: String,
    val categoryId: Long?,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String,
    val type: ExpenseType,
    val tagIds: List<Long> = emptyList(),
    val dayOfMonth: Int,
)
