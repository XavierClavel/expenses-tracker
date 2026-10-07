package com.xavierclavel.bankable.model

/**
 * A key that was just created, with its full secret. The backend returns [key] only in the
 * creation response and can never return it again, so it should be held no longer than it
 * takes to show it to the user.
 */
data class CreatedApiKeyOut(
    val apiKey: ApiKeyOut,
    val key: String,
) {
    // Keeps the secret out of logs and crash reports.
    override fun toString(): String = "CreatedApiKeyOut(apiKey=$apiKey, key=***)"
}
