package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class SubcategoryIn(
    val name: String,
    val type: String,
    val icon: String,
    val parentCategory: Int,
)
