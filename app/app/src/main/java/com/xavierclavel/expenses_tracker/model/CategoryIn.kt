package com.xavierclavel.expenses_tracker.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryIn(
    val name: String,
    val type: String,
    val color: String,
    val icon: String,
)
