package com.xavierclavel.bankable.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

// Expenses, categories and tags are read and written locally, and synced through SyncApi.kt.

@Serializable
private data class OldestExpenseResponse(val date: String? = null)

suspend fun apiGetOldestExpenseDate(): String? {
    return httpClient.get("$BASE_URL/expenses/oldest") { authHeader() }
        .body<OldestExpenseResponse>().date
}
