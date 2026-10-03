package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Request body for duplicating several expenses into another month.
 *
 * @property ids The expenses to duplicate.
 * @property year Target year of the copies.
 * @property month Target month of the copies (1-12). Each copy keeps its original day of
 * month, clamped to the last day of the target month.
 */
@Serializable
data class ExpenseDuplicateIn(
    val ids: List<Long>,
    val year: Int,
    val month: Int,
)
