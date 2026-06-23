package com.xavierclavel.bankable.storage

import android.content.Context

private const val PREFS_NAME = "settings_prefs"
private const val LANGUAGE_TAG_KEY = "language_tag"

// Plain (unencrypted) preferences for non-sensitive app settings. Kept separate
// from the encrypted auth_prefs so it can be read synchronously in
// Activity.attachBaseContext, before the rest of the app is initialised.
class LocalePreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // A BCP-47 language tag ("en", "fr"), or null/empty to follow the device locale.
    var languageTag: String?
        get() = prefs.getString(LANGUAGE_TAG_KEY, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrEmpty()) remove(LANGUAGE_TAG_KEY) else putString(LANGUAGE_TAG_KEY, value)
            }.apply()
        }
}
