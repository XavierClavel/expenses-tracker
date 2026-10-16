package com.xavierclavel.dtos.summary

import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * A subcategory whose amounts over a year run well above their usual pace.
 *
 * @property total What the subcategory amounted to over the year.
 * @property mean What it would have amounted to over the same months at its monthly mean
 *   over the years around it, so it reads directly against [total].
 * @property deviation How far [total] sits above [mean], as a fraction (0.5 = +50%).
 */
@Serializable
data class SubcategoryOutlier(
    val subcategoryId: Long,
    val subcategoryName: String,
    val type: ExpenseType,
    @Serializable(with = BigDecimalSerializer::class)
    val total: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val mean: BigDecimal,
    val deviation: Double,
)
