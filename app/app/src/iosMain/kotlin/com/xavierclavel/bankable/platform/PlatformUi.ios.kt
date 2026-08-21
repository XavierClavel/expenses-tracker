package com.xavierclavel.bankable.platform

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.xavierclavel.bankable.settings.LocaleManager
import platform.Foundation.NSUserDefaults

@Composable
actual fun rememberAppLocaleTag(): String {
    // iOS resolves the app language at launch (see [applyAppLanguage]), so the
    // tag is stable for the lifetime of the process.
    val stored = LocaleManager.storedLanguageTag()
    return remember(stored) { stored.ifEmpty { platformDefaultLocaleTag() } }
}

/** iOS has no Material You equivalent; the app's own palette is always used. */
@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

actual val languageChangeAppliesImmediately: Boolean = false

actual fun applyAppLanguage(tag: String) {
    LocaleManager.storeLanguageTag(tag)
    val defaults = NSUserDefaults.standardUserDefaults
    if (tag.isEmpty()) {
        // Fall back to the device's own language ordering.
        defaults.removeObjectForKey("AppleLanguages")
    } else {
        defaults.setObject(listOf(tag), forKey = "AppleLanguages")
    }
    // NSLocale and the bundle's resource lookup only re-read AppleLanguages at
    // launch, so the new language applies the next time the app starts.
}
