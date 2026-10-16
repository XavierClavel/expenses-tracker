package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

// A subcategory running well above its all-time pace over a year. [mean] is what it
// would have totalled over the same months at its all-time monthly mean, so it reads
// directly against [total]; [deviation] is how far above it sits (0.5 = +50%).
@Serializable
data class SubcategoryOutlierDto(
    val subcategoryId: Int,
    val subcategoryName: String,
    val type: String,
    val total: String,
    val mean: String,
    val deviation: Double,
)
