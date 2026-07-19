package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class TagOut(
    val id: Int,
    val label: String,
    // Sum of the amounts of the expenses linked to this tag, as a plain decimal string.
    val total: String,
    val expenseCount: Int,
)
