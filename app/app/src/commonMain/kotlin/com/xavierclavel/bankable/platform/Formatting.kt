package com.xavierclavel.bankable.platform

/**
 * Locale-aware number and date formatting.
 *
 * Kotlin has no multiplatform ICU, so each platform delegates to its own
 * formatter stack: java.text on Android, NSNumberFormatter / NSDateFormatter on
 * iOS. Locales are passed around as BCP-47 tags ("en", "fr") rather than
 * java.util.Locale, which is JVM-only.
 */

/** ISO 4217 code (e.g. "EUR") to its display symbol (e.g. "€"). */
expect fun platformCurrencySymbol(currencyCode: String): String

/**
 * Decimal with the locale's separators, e.g. 1234.5 -> "1,234.50" (en) /
 * "1 234,50" (fr). Set [useGrouping] to false for values that read better
 * ungrouped, such as percentages.
 */
expect fun platformFormatDecimal(
    value: Double,
    localeTag: String,
    minFractionDigits: Int,
    maxFractionDigits: Int,
    useGrouping: Boolean = true,
): String

/** Grouped integer, e.g. 1403 -> "1,403" (en) / "1 403" (fr). */
expect fun platformFormatInteger(value: Long, localeTag: String): String

/** Long-form date, e.g. "7 August 2026" (en) / "7 août 2026" (fr). */
expect fun platformFormatLongDate(year: Int, month: Int, dayOfMonth: Int, localeTag: String): String

/** Abbreviated month name for the given 1-based month, e.g. "Aug" / "août". */
expect fun platformShortMonthName(year: Int, month: Int, localeTag: String): String

/** Month and year, e.g. "August 2026" / "août 2026". */
expect fun platformFormatMonthYear(year: Int, month: Int, localeTag: String): String

/** Short date without the year, e.g. "7 Aug" / "7 août". */
expect fun platformFormatShortDate(year: Int, month: Int, dayOfMonth: Int, localeTag: String): String

/** The device's current locale as a BCP-47 tag. */
expect fun platformDefaultLocaleTag(): String
