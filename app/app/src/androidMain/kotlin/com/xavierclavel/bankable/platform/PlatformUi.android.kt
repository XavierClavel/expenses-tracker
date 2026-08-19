package com.xavierclavel.bankable.platform

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.xavierclavel.bankable.settings.LocaleManager

@Composable
actual fun rememberAppLocaleTag(): String =
    LocalConfiguration.current.locales[0].toLanguageTag()

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    // Material You is available on Android 12+.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

actual val languageChangeAppliesImmediately: Boolean = true

actual fun applyAppLanguage(tag: String) {
    LocaleManager.storeLanguageTag(tag)
    // Re-runs attachBaseContext so resources and the default Locale pick up the
    // new language.
    AppContext.currentActivity?.recreate()
}
