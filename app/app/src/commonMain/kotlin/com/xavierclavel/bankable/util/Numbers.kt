package com.xavierclavel.bankable.util

import com.xavierclavel.bankable.platform.platformFormatDecimal

/**
 * Fixed-decimal rendering, replacing the JVM-only `"%.1f".format(v)`.
 *
 * Like String.format it follows the current locale's decimal separator ("1.5"
 * in en, "1,5" in fr) but never groups thousands, so percentages and compact
 * axis labels read the same as before.
 */
fun formatFixed(value: Double, decimals: Int): String =
    platformFormatDecimal(
        value = value,
        localeTag = "",
        minFractionDigits = decimals,
        maxFractionDigits = decimals,
        useGrouping = false,
    )

/** Zero-padded decimal, replacing `"%02d".format(n)`. */
fun Int.padZeros(width: Int): String = toString().padStart(width, '0')

/**
 * Parses a decimal amount ("12", "12.5", "-3.20", "12,5") into cents, rounding half up past the
 * second decimal. Returns null when [text] isn't a plain decimal number.
 */
fun parseCents(text: String): Long? {
    val match = Regex("""^([-+])?(\d*)(?:[.,](\d*))?$""").matchEntire(text.trim()) ?: return null
    val (sign, whole, fraction) = match.destructured
    if (whole.isEmpty() && fraction.isEmpty()) return null
    val units = whole.ifEmpty { "0" }.toLongOrNull() ?: return null
    val digits = fraction.padEnd(3, '0')
    val cents = units * 100 + digits.substring(0, 2).toLong() + if (digits[2] >= '5') 1 else 0
    return if (sign == "-") -cents else cents
}

/** Renders cents as a plain decimal with two digits, as the backend writes amounts ("-3.20"). */
fun formatCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = if (cents < 0) -cents else cents
    return "$sign${abs / 100}.${(abs % 100).toInt().padZeros(2)}"
}
