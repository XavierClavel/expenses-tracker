package com.xavierclavel.bankable.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"

private class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore, SecureStore {
    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun remove(key: String) = prefs.edit().remove(key).apply()
    override fun clear() = prefs.edit().clear().apply()

    override fun all(): Map<String, String> =
        prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
}

actual fun createKeyValueStore(name: String): KeyValueStore =
    SharedPreferencesStore(AppContext.appContext.getSharedPreferences(name, Context.MODE_PRIVATE))

actual fun createSecureStore(name: String): SecureStore {
    val context = AppContext.appContext
    val prefs = try {
        buildEncryptedPrefs(context, name)
    } catch (_: Exception) {
        // The encrypted keyset on disk can't be decrypted with the current
        // Keystore master key (e.g. after a reinstall, OS upgrade, or Keystore
        // reset) -> AEADBadTagException. Wipe the corrupt state and recreate.
        // The stored token is unrecoverable, so the user simply re-authenticates.
        context.deleteSharedPreferences(name)
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(MASTER_KEY_ALIAS)
        }
        buildEncryptedPrefs(context, name)
    }
    return SharedPreferencesStore(prefs)
}

private fun buildEncryptedPrefs(context: Context, name: String): SharedPreferences =
    EncryptedSharedPreferences.create(
        context,
        name,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
