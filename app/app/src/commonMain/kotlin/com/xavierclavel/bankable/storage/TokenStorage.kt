package com.xavierclavel.bankable.storage

import com.xavierclavel.bankable.platform.createSecureStore

private const val STORE_NAME = "auth_prefs"
private const val TOKEN_KEY = "session_token"

/**
 * The session token, held in the platform's secure storage —
 * EncryptedSharedPreferences on Android, the Keychain on iOS.
 */
class TokenStorage {
    private val store = createSecureStore(STORE_NAME)

    fun saveToken(token: String) = store.putString(TOKEN_KEY, token)
    fun loadToken(): String? = store.getString(TOKEN_KEY)
    fun clearToken() = store.remove(TOKEN_KEY)
}
