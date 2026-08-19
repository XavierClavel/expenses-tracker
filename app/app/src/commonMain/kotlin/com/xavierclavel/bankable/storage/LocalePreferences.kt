package com.xavierclavel.bankable.storage

import com.xavierclavel.bankable.platform.createKeyValueStore

private const val STORE_NAME = "settings_prefs"
private const val LANGUAGE_TAG_KEY = "language_tag"

// Plain (unencrypted) preferences for non-sensitive app settings. Kept separate
// from the encrypted auth store so it can be read synchronously very early in
// start-up — on Android that's Activity.attachBaseContext, before the rest of
// the app is initialised.
class LocalePreferences {
    private val store = createKeyValueStore(STORE_NAME)

    // A BCP-47 language tag ("en", "fr"), or null/empty to follow the device locale.
    var languageTag: String?
        get() = store.getString(LANGUAGE_TAG_KEY)
        set(value) {
            if (value.isNullOrEmpty()) store.remove(LANGUAGE_TAG_KEY)
            else store.putString(LANGUAGE_TAG_KEY, value)
        }
}
