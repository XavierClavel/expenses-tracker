package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountOut(
    val id: Int,
    val name: String,
    val type: String = "OTHER",
    // How the balance is split: "CONTRIBUTIONS" (record deposits, infer interest) or
    // "INTEREST" (record known interest/fees, infer contributions).
    val tracking: String = "CONTRIBUTIONS",
    val amount: String,
    // Cumulative net transfers (deposits − withdrawals). The gap between `amount`
    // and `contributions` is the accrued interest. Defaults to "0" so the app
    // keeps working against a backend that predates this field.
    val contributions: String = "0",
    // Most recent full-year return as a fraction (e.g. "0.068" = +6.8%) and the year
    // it covers. Null when there isn't enough history to compute it.
    val latestAnnualReturn: String? = null,
    val latestAnnualReturnYear: Int? = null,
    // Interest earned during latestAnnualReturnYear (the € amount).
    val latestYearInterest: String? = null,
)
