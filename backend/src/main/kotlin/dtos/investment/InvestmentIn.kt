package com.xavierclavel.dtos.investment

import com.xavierclavel.enums.InvestmentType
import com.xavierclavel.utils.BigDecimalSerializer
import com.xavierclavel.utils.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class InvestmentIn(
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,

    val accountId: Long,

    val type: InvestmentType,

    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)
