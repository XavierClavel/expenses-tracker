package com.xavierclavel.bankable.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * Applies the stored language on Android by wrapping the Activity's base
 * context, which is the only way to make resources — and the JVM default Locale
 * that NumberFormat reads — follow an in-app language choice.
 */
object AndroidLocaleManager {
    // Call from Activity.attachBaseContext; recreate the Activity to re-apply
    // after a change.
    fun applyStoredLocale(context: Context): Context {
        val tag = LocaleManager.storedLanguageTag()
        val locale = if (tag.isEmpty()) {
            // The true device locale, unaffected by any previous override.
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale.forLanguageTag(tag)
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
