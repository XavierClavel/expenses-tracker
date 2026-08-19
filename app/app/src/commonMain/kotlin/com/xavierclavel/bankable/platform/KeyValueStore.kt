package com.xavierclavel.bankable.platform

/**
 * Minimal string key/value store — the multiplatform stand-in for the
 * SharedPreferences the app used to depend on directly. Everything is stored as
 * a String; callers serialize richer values themselves (see
 * [com.xavierclavel.bankable.storage.PersistentCookiesStorage]).
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
    fun clear()

    /** Every entry currently stored, used to restore caches at start-up. */
    fun all(): Map<String, String>
}

/**
 * Single-value secure storage, backed by EncryptedSharedPreferences on Android
 * and the Keychain on iOS. Deliberately narrower than [KeyValueStore]: the
 * Keychain has no cheap enumeration, and the only secret we keep is the session
 * token.
 */
interface SecureStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

/** Plain, unencrypted store for non-sensitive settings. */
expect fun createKeyValueStore(name: String): KeyValueStore

/** Store backed by the platform's secure storage. Use for the session token, nothing else. */
expect fun createSecureStore(name: String): SecureStore
