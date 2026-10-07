package com.xavierclavel.bankable.model

/** A personal API key, as listed. The secret itself is never returned after creation. */
data class ApiKeyOut(
    val id: Int,
    val name: String,
    // Masked form of the key, ready to display, e.g. "bk_…a1B2".
    val hint: String,
    // "yyyy-MM-dd".
    val createdAt: String,
    // "yyyy-MM-dd" of the last request made with the key, or null if it was never used.
    val lastUsedAt: String?,
)
