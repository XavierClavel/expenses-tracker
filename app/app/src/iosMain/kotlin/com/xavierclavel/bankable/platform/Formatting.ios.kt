package com.xavierclavel.bankable.platform

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone
import platform.Foundation.localeIdentifier
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.timeZoneWithAbbreviation

private fun nsLocale(tag: String): NSLocale =
    if (tag.isEmpty()) NSLocale.currentLocale
    else NSLocale.localeWithLocaleIdentifier(tag.replace('-', '_'))

actual fun platformCurrencySymbol(currencyCode: String): String {
    val formatter = NSNumberFormatter().apply {
        setNumberStyle(platform.Foundation.NSNumberFormatterCurrencyStyle)
        setCurrencyCode(currencyCode)
    }
    return formatter.currencySymbol ?: currencyCode
}

actual fun platformFormatDecimal(
    value: Double,
    localeTag: String,
    minFractionDigits: Int,
    maxFractionDigits: Int,
    useGrouping: Boolean,
): String {
    val formatter = NSNumberFormatter().apply {
        setNumberStyle(NSNumberFormatterDecimalStyle)
        setLocale(nsLocale(localeTag))
        setMinimumFractionDigits(minFractionDigits.toULong())
        setMaximumFractionDigits(maxFractionDigits.toULong())
        setUsesGroupingSeparator(useGrouping)
    }
    return formatter.stringFromNumber(platform.Foundation.NSNumber(double = value)) ?: value.toString()
}

actual fun platformFormatInteger(value: Long, localeTag: String): String {
    val formatter = NSNumberFormatter().apply {
        setNumberStyle(NSNumberFormatterDecimalStyle)
        setLocale(nsLocale(localeTag))
        setMaximumFractionDigits(0u)
    }
    return formatter.stringFromNumber(platform.Foundation.NSNumber(long = value)) ?: value.toString()
}

actual fun platformFormatLongDate(year: Int, month: Int, dayOfMonth: Int, localeTag: String): String =
    formatDate(year, month, dayOfMonth, localeTag, "d MMMM yyyy")

actual fun platformShortMonthName(year: Int, month: Int, localeTag: String): String =
    formatDate(year, month, 1, localeTag, "MMM")

actual fun platformFormatMonthYear(year: Int, month: Int, localeTag: String): String =
    formatDate(year, month, 1, localeTag, "MMMM yyyy")

actual fun platformFormatShortDate(year: Int, month: Int, dayOfMonth: Int, localeTag: String): String =
    formatDate(year, month, dayOfMonth, localeTag, "d MMM")

actual fun platformDefaultLocaleTag(): String =
    NSLocale.currentLocale.localeIdentifier.replace('_', '-')

private fun formatDate(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    localeTag: String,
    pattern: String,
): String {
    val components = NSDateComponents().apply {
        setYear(year.toLong())
        setMonth(month.toLong())
        setDay(dayOfMonth.toLong())
        setTimeZone(NSTimeZone.timeZoneWithAbbreviation("UTC"))
    }
    // Explicitly Gregorian: the incoming y/m/d is an ISO date, so it must not be
    // reinterpreted in a locale's non-Gregorian calendar.
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    val date = calendar.dateFromComponents(components) ?: return "$year-$month-$dayOfMonth"
    val formatter = NSDateFormatter().apply {
        setLocale(nsLocale(localeTag))
        setDateFormat(pattern)
        setTimeZone(NSTimeZone.timeZoneWithAbbreviation("UTC") ?: NSTimeZone.localTimeZone)
    }
    return formatter.stringFromDate(date)
}
