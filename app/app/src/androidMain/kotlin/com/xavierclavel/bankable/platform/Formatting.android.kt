package com.xavierclavel.bankable.platform

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale

private fun locale(tag: String): Locale =
    if (tag.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(tag)

actual fun platformCurrencySymbol(currencyCode: String): String = try {
    Currency.getInstance(currencyCode).symbol
} catch (_: Exception) {
    currencyCode
}

actual fun platformFormatDecimal(
    value: Double,
    localeTag: String,
    minFractionDigits: Int,
    maxFractionDigits: Int,
    useGrouping: Boolean,
): String = NumberFormat.getNumberInstance(locale(localeTag)).apply {
    minimumFractionDigits = minFractionDigits
    maximumFractionDigits = maxFractionDigits
    isGroupingUsed = useGrouping
}.format(value)

actual fun platformFormatInteger(value: Long, localeTag: String): String =
    NumberFormat.getIntegerInstance(locale(localeTag)).format(value)

actual fun platformFormatLongDate(year: Int, month: Int, dayOfMonth: Int, localeTag: String): String =
    SimpleDateFormat("d MMMM yyyy", locale(localeTag)).format(calendarFor(year, month, dayOfMonth).time)

actual fun platformShortMonthName(year: Int, month: Int, localeTag: String): String =
    SimpleDateFormat("MMM", locale(localeTag)).format(calendarFor(year, month, 1).time)

actual fun platformFormatMonthYear(year: Int, month: Int, localeTag: String): String =
    SimpleDateFormat("MMMM yyyy", locale(localeTag)).format(calendarFor(year, month, 1).time)

actual fun platformFormatShortDate(year: Int, month: Int, dayOfMonth: Int, localeTag: String): String =
    SimpleDateFormat("d MMM", locale(localeTag)).format(calendarFor(year, month, dayOfMonth).time)

actual fun platformDefaultLocaleTag(): String = Locale.getDefault().toLanguageTag()

private fun calendarFor(year: Int, month: Int, dayOfMonth: Int): Calendar =
    Calendar.getInstance().apply {
        clear()
        set(year, month - 1, dayOfMonth)
    }
