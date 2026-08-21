package com.xavierclavel.bankable.model

data class SubcategoryOut(
    val id: Int,
    val name: String,
    val type: String,
    val color: String,
    val icon: String,
    val isDefault: Boolean,
)
