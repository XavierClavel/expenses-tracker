package com.xavierclavel.expenses_tracker.model

data class SubcategoryOut(
    val id: Int,
    val name: String,
    val type: String,
    val color: String,
    val icon: String,
    val isDefault: Boolean,
)
