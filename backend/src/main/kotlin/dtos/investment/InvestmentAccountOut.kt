package com.xavierclavel.dtos.investment

import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class InvestmentAccountOut(
    val id: Long,
    val name: String,

    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
)