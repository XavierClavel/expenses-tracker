package com.xavierclavel.expenses_tracker.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "auth_prefs"
private const val TOKEN_KEY = "session_token"

class TokenStorage(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
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
