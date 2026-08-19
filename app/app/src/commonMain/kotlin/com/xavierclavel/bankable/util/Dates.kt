package com.xavierclavel.bankable.util

import com.xavierclavel.bankable.platform.platformFormatLongDate
import com.xavierclavel.bankable.platform.platformFormatShortDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Date helpers shared by every screen with a date field.
 *
 * The backend stores plain "yyyy-MM-dd" calendar dates, so everything here works
 * in UTC: a date means the same day regardless of where the phone is. Millis are
 * only used to talk to Material's DatePicker, which takes UTC midnight.
 */

private const val MILLIS_PER_DAY = 86_400_000L

/** Today's date as "yyyy-MM-dd" (UTC). */
@OptIn(ExperimentalTime::class)
fun todayIsoDate(): String =
    Clock.System.now().toLocalDateTime(TimeZone.UTC).date.toString()

/** UTC midnight for a "yyyy-MM-dd" date, falling back to today if it can't be parsed. */
@OptIn(ExperimentalTime::class)
fun isoDateToUtcMillis(isoDate: String): Long =
    parseIsoDate(isoDate)?.let { it.toEpochDays() * MILLIS_PER_DAY }
        ?: Clock.System.now().toEpochMilliseconds()

/** The "yyyy-MM-dd" date that UTC-midnight [millis] falls on. */
fun utcMillisToIsoDate(millis: Long): String =
    LocalDate.fromEpochDays(millis.floorDiv(MILLIS_PER_DAY)).toString()

/**
 * Long-form, localized rendering of a "yyyy-MM-dd" date — "7 August 2026" in en,
 * "7 août 2026" in fr. Unparseable input is passed through unchanged.
 */
fun formatIsoDateLong(isoDate: String, localeTag: String): String {
    val date = parseIsoDate(isoDate) ?: return isoDate
    return platformFormatLongDate(date.year, date.month.ordinal + 1, date.day, localeTag)
}

/**
 * Short, localized rendering of a "yyyy-MM-dd" date without the year —
 * "7 Aug" in en, "7 août" in fr. Unparseable input is passed through unchanged.
 */
fun formatIsoDateShort(isoDate: String, localeTag: String): String {
    val date = parseIsoDate(isoDate) ?: return isoDate
    return platformFormatShortDate(date.year, date.month.ordinal + 1, date.day, localeTag)
}

/**
 * The current year on the device's own clock. Unlike the stored dates above this
 * is deliberately local, not UTC: it answers "what period is the user in right
 * now", which drives the period pickers and the "this year" label.
 */
@OptIn(ExperimentalTime::class)
fun currentYear(): Int = nowLocal().year

/** The current month (1..12) on the device's own clock. */
@OptIn(ExperimentalTime::class)
fun currentMonth(): Int = nowLocal().month.ordinal + 1

@OptIn(ExperimentalTime::class)
private fun nowLocal() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/** Number of days in the given month, leap years included. */
fun lastDayOfMonth(year: Int, month: Int): Int {
    val firstOfThisMonth = LocalDate(year, month, 1)
    val firstOfNextMonth =
        if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return (firstOfNextMonth.toEpochDays() - firstOfThisMonth.toEpochDays()).toInt()
}

private fun parseIsoDate(isoDate: String): LocalDate? =
    runCatching { LocalDate.parse(isoDate) }.getOrNull()
