package com.xavierclavel.dtos

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Represents a tag exposed to clients.
 *
 * @property label Display label of the tag.
 * @property total Sum of the amounts of the expenses linked to this tag.
 * @property expenseCount Number of expenses linked to this tag.
 */
@Serializable
data class TagOut(
    val id: Long,
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal,
    val expenseCount: Int,
)
