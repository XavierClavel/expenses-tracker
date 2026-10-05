package com.xavierclavel.dtos

import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.BigDecimalSerializer
import com.xavierclavel.utils.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Represents a recurring expense exposed to clients.
 *
 * @property dayOfMonth Day of the month (1 to 31) on which an expense is created.
 * @property nextDate Date of the next expense that will be created.
 * @property lastGeneratedDate Date of the last expense created, or null if none was created yet.
 */
@Serializable
data class RecurringExpenseOut(
    val id: Long,
    val title: String,
    val categoryId: Long?,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String,
    val type: ExpenseType,
    val tagIds: List<Long> = emptyList(),
    val dayOfMonth: Int,
    @Serializable(with = LocalDateSerializer::class)
    val nextDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val lastGeneratedDate: LocalDate? = null,
)
