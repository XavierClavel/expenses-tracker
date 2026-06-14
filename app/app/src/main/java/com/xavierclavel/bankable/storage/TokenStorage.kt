package com.xavierclavel.bankable.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "auth_prefs"
private const val TOKEN_KEY = "session_token"
private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"

class TokenStorage(context: Context) {
    private val prefs = createEncryptedPrefs(context)

    private fun createEncryptedPrefs(context: Context) = try {
        buildPrefs(context)
    } catch (e: Exception) {
        // The encrypted keyset on disk can't be decrypted with the current
        // Keystore master key (e.g. after a reinstall, OS upgrade, or Keystore
        // reset) -> AEADBadTagException. Wipe the corrupt state and recreate.
        // The stored token is unrecoverable, so the user simply re-authenticates.
        context.deleteSharedPreferences(PREFS_NAME)
        runCatching {
            java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(MASTER_KEY_ALIAS)
        }
        buildPrefs(context)
    }

    private fun buildPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) = prefs.edit().putString(TOKEN_KEY, token).apply()
    fun loadToken(): String? = prefs.getString(TOKEN_KEY, null)
    fun clearToken() = prefs.edit().remove(TOKEN_KEY).apply()
}
