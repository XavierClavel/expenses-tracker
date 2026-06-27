package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountOut(
    val id: Int,
    val name: String,
    val type: String = "OTHER",
    val amount: String,
)
