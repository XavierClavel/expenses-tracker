package com.xavierclavel.dtos

import com.xavierclavel.utils.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Request body for duplicating several expenses. Give either [date], or [year] and [month].
 *
 * @property ids The expenses to duplicate.
 * @property date Exact date of every copy. Takes precedence over [year] and [month].
 * @property year Target year of the copies, used with [month] when [date] is absent.
 * @property month Target month of the copies (1-12). Each copy keeps its original day of
 * month, clamped to the last day of the target month.
 */
@Serializable
data class ExpenseDuplicateIn(
    val ids: List<Long>,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate? = null,
    val year: Int? = null,
    val month: Int? = null,
)
