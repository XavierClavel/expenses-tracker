package com.xavierclavel.bankable.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.xavierclavel.bankable.settings.LocaleManager

@Composable
actual fun rememberAppLocaleTag(): String =
    LocalConfiguration.current.locales[0].toLanguageTag()

actual val languageChangeAppliesImmediately: Boolean = true

actual fun applyAppLanguage(tag: String) {
    LocaleManager.storeLanguageTag(tag)
    // Re-runs attachBaseContext so resources and the default Locale pick up the
    // new language.
    AppContext.currentActivity?.recreate()
}
