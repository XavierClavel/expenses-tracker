package com.xavierclavel.bankable.model

/**
 * @property id The tag's uuid, shared with the backend.
 * @property serverId The id the REST API knows the tag by: null until it has been synced.
 */
data class TagOut(
    val id: String,
    val serverId: Int?,
    val label: String,
    // Net balance of the expenses linked to this tag (incomes minus expenses), as a plain decimal string.
    val total: String,
    val expenseCount: Int,
)
