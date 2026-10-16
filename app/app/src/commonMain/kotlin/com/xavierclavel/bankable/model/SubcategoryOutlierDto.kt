package com.xavierclavel.bankable.model

// A subcategory running well above its usual pace over a year. [mean] is what it would
// have totalled over the same months at its monthly mean over the years around it, so it
// reads directly against [total]; [deviation] is how far above it sits (0.5 = +50%).
data class SubcategoryOutlierDto(
    val subcategoryId: String,
    val subcategoryName: String,
    val type: String,
    val total: String,
    val mean: String,
    val deviation: Double,
)
