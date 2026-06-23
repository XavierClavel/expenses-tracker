package com.xavierclavel.bankable.constants

import java.text.NumberFormat
import java.util.Currency

// Maps an ISO 4217 currency code (e.g. "EUR") to its display symbol (e.g. "€").
// Falls back to the raw code for unknown currencies.
fun currencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (_: Exception) {
        currencyCode
    }
}

// Rounds to the nearest unit and formats with the device locale's grouping
// separator — "1,403" in English, "1 403" in French. Includes a sign for
// negative values; no currency symbol is appended.
fun formatRoundedAmount(value: Double): String =
    NumberFormat.getIntegerInstance().format(Math.round(value))
