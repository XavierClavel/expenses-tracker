package com.xavierclavel.bankable.constants

import com.xavierclavel.bankable.platform.platformCurrencySymbol
import com.xavierclavel.bankable.platform.platformFormatDecimal
import com.xavierclavel.bankable.platform.platformFormatInteger
import kotlin.math.roundToLong

// Maps an ISO 4217 currency code (e.g. "EUR") to its display symbol (e.g. "€").
// Falls back to the raw code for unknown currencies.
fun currencySymbol(currencyCode: String): String = platformCurrencySymbol(currencyCode)

// Rounds to the nearest unit and formats with the device locale's grouping
// separator — "1,403" in English, "1 403" in French. Includes a sign for
// negative values; no currency symbol is appended.
fun formatRoundedAmount(value: Double): String =
    platformFormatInteger(value.roundToLong(), localeTag = "")

// Formats a raw amount string (e.g. "1234.50") with the given locale's grouping
// separator — "1,234.50" in English, "1 234,50" in French — preserving the original
// number of decimal places. Falls back to the raw string if it isn't a number.
fun formatAmountDisplay(amount: String, localeTag: String): String {
    val value = amount.toDoubleOrNull() ?: return amount
    val decimals = amount.substringAfter('.', "").length
    return platformFormatDecimal(value, localeTag, minFractionDigits = decimals, maxFractionDigits = decimals)
}
