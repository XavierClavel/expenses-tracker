package com.xavierclavel.dtos.investment

import com.xavierclavel.utils.BigDecimalSerializer
import com.xavierclavel.utils.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Serializable
data class AccountReportIn(
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,

    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)
