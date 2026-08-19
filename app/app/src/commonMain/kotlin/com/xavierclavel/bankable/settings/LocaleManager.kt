package com.xavierclavel.bankable.settings

import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.settings_language_english
import com.xavierclavel.bankable.resources.settings_language_french
import com.xavierclavel.bankable.resources.settings_language_system
import com.xavierclavel.bankable.storage.LocalePreferences
import org.jetbrains.compose.resources.StringResource

// Languages the app ships translations for. An empty tag means "follow the
// device locale". Add a new entry here (plus a values-<lang> folder under
// commonMain/composeResources) to offer another language.
enum class AppLanguage(val tag: String, val labelRes: StringResource) {
    SYSTEM("", Res.string.settings_language_system),
    ENGLISH("en", Res.string.settings_language_english),
    FRENCH("fr", Res.string.settings_language_french),
}

/**
 * Reads and writes the app's language choice. Applying it is platform work
 * (see [com.xavierclavel.bankable.platform.applyAppLanguage]): Android recreates
 * the Activity so resources reload, iOS picks the change up on next launch.
 */
object LocaleManager {
    fun storedLanguageTag(): String = LocalePreferences().languageTag.orEmpty()

    fun storeLanguageTag(tag: String) {
        LocalePreferences().languageTag = tag
    }

    fun currentLanguage(): AppLanguage {
        val tag = storedLanguageTag()
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.SYSTEM
    }
}
