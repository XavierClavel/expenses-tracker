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
