package com.xavierclavel.bankable.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The locale the UI is currently rendering in, as a BCP-47 tag. Provided by the
 * platform entry point (it tracks the Activity configuration on Android and
 * NSLocale on iOS) and read by every screen that formats a date or an amount.
 */
val LocalAppLocale = staticCompositionLocalOf { "en" }

/** The locale to render in, recomposing when the platform's locale changes. */
@Composable
expect fun rememberAppLocaleTag(): String

/**
 * Material You colors, when the platform offers them (Android 12+). Null means
 * "use the app's own palette".
 */
@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** True when the app can switch language in place; false when a restart is needed. */
expect val languageChangeAppliesImmediately: Boolean

/**
 * Persists [tag] as the app's language and applies it. On Android that recreates
 * the Activity so resources reload; on iOS the change takes effect on next launch.
 * An empty tag means "follow the device language".
 */
expect fun applyAppLanguage(tag: String)
