package com.xavierclavel.bankable.constants

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

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

// Formats a raw amount string (e.g. "1234.50") with the given locale's grouping
// separator — "1,234.50" in English, "1 234,50" in French — preserving the original
// number of decimal places. Falls back to the raw string if it isn't a number.
fun formatAmountDisplay(amount: String, locale: Locale): String {
    val value = amount.toDoubleOrNull() ?: return amount
    val decimals = amount.substringAfter('.', "").length
    return NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)
}
