package com.xavierclavel.enums

enum class InvestmentType {
    // Contributions: external money you add / remove.
    IN,
    OUT,
    // Earnings: money the account gained/lost on its own that you know exactly
    // (e.g. Livret A interest credited, or an account fee).
    INTEREST,
    FEE,
}