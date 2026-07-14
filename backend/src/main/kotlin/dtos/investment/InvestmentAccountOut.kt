package com.xavierclavel.dtos.investment

import com.xavierclavel.enums.AccountTracking
import com.xavierclavel.enums.AccountType
import com.xavierclavel.utils.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class InvestmentAccountOut(
    val id: Long,
    val name: String,
    val type: AccountType,
    val tracking: AccountTracking = AccountTracking.CONTRIBUTIONS,

    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,

    // Cumulative net transfers into the account (deposits − withdrawals). The
    // difference between `amount` and `contributions` is the accrued interest.
    @Serializable(with = BigDecimalSerializer::class)
    val contributions: BigDecimal,

    // Most recent full-year return rate as a fraction (0.068 = +6.8%): the interest
    // earned during `latestAnnualReturnYear` divided by that year's starting balance.
    // Null when there isn't a prior year with a positive balance to measure against.
    @Serializable(with = BigDecimalSerializer::class)
    val latestAnnualReturn: BigDecimal? = null,
    val latestAnnualReturnYear: Int? = null,

    // Interest earned during `latestAnnualReturnYear` (the € amount, not the rate).
    @Serializable(with = BigDecimalSerializer::class)
    val latestYearInterest: BigDecimal? = null,
)