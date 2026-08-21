package com.xavierclavel.bankable.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * Non-sensitive settings live in NSUserDefaults, namespaced by [name] so the
 * separate stores (settings, cookies) never collide.
 */
private class UserDefaultsStore(name: String) : KeyValueStore {
    private val prefix = "$name."
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(prefix + key)

    override fun putString(key: String, value: String) = defaults.setObject(value, prefix + key)

    override fun remove(key: String) = defaults.removeObjectForKey(prefix + key)

    override fun clear() = keys().forEach { defaults.removeObjectForKey(it) }

    override fun all(): Map<String, String> = keys().mapNotNull { fullKey ->
        defaults.stringForKey(fullKey)?.let { fullKey.removePrefix(prefix) to it }
    }.toMap()

    private fun keys(): List<String> = defaults.dictionaryRepresentation().keys
        .filterIsInstance<String>()
        .filter { it.startsWith(prefix) }
}

/**
 * Keychain-backed store — the iOS counterpart of EncryptedSharedPreferences.
 * Entries are generic passwords keyed by (service = [service], account = key)
 * and readable once the device has been unlocked at least once, so a token saved
 * before a reboot still works for background refreshes.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class KeychainStore(private val service: String) : SecureStore {

    override fun getString(key: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service.toCF(),
            kSecAttrAccount to key.toCF(),
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
        ) { SecItemCopyMatching(it, result.ptr) }
        if (status != errSecSuccess) return null
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        data.toByteArray().decodeToString()
    }

    override fun putString(key: String, value: String) {
        // The Keychain has no upsert, so replace unconditionally.
        remove(key)
        withQuery(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service.toCF(),
            kSecAttrAccount to key.toCF(),
            kSecValueData to CFBridgingRetain(value.encodeToByteArray().toNSData()),
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
        ) { SecItemAdd(it, null) }
    }

    override fun remove(key: String) {
        withQuery(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service.toCF(),
            kSecAttrAccount to key.toCF(),
        ) { SecItemDelete(it) }
    }

    /**
     * Builds a Keychain query dictionary, runs [block] on it, then releases the
     * dictionary along with every CFTypeRef this call created for it.
     */
    private fun <R> withQuery(vararg pairs: Pair<CFTypeRef?, CFTypeRef?>, block: (CFDictionaryRef?) -> R): R {
        val dictionary = CFDictionaryCreateMutable(
            null,
            pairs.size.toLong(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        // Values we allocated ourselves (via CFBridgingRetain); the constants are
        // owned by the Security framework and must not be released.
        val owned = mutableListOf<CFTypeRef>()
        pairs.forEach { (key, value) ->
            CFDictionaryAddValue(dictionary, key, value)
            if (value != null && value != kCFBooleanTrue &&
                value != kSecClassGenericPassword && value != kSecMatchLimitOne &&
                value != kSecAttrAccessibleAfterFirstUnlock
            ) {
                owned += value
            }
        }
        try {
            return block(dictionary)
        } finally {
            owned.forEach { CFRelease(it) }
            CFRelease(dictionary)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun String.toCF(): CFTypeRef? = CFBridgingRetain(this)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return out
}

actual fun createKeyValueStore(name: String): KeyValueStore = UserDefaultsStore(name)

actual fun createSecureStore(name: String): SecureStore =
    KeychainStore("com.xavierclavel.bankable.$name")
