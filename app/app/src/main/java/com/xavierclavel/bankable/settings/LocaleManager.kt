package com.xavierclavel.bankable.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.storage.LocalePreferences
import java.util.Locale

// Languages the app ships translations for. An empty tag means "follow the
// device locale". Add a new entry here (plus a values-<lang> folder) to offer
// another language.
enum class AppLanguage(val tag: String, @StringRes val labelRes: Int) {
    SYSTEM("", R.string.settings_language_system),
    ENGLISH("en", R.string.settings_language_english),
    FRENCH("fr", R.string.settings_language_french),
}

object LocaleManager {
    // Wraps a base context so its resources (and the JVM default Locale used by
    // NumberFormat) reflect the stored language choice. Call from
    // Activity.attachBaseContext; recreate the activity to re-apply after a change.
    fun applyStoredLocale(context: Context): Context {
        val tag = LocalePreferences(context).languageTag
        val locale = if (tag.isNullOrEmpty()) {
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

    fun currentLanguage(context: Context): AppLanguage {
        val tag = LocalePreferences(context).languageTag
        return AppLanguage.values().firstOrNull { it.tag == tag } ?: AppLanguage.SYSTEM
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        LocalePreferences(context).languageTag = language.tag
    }
}

fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
