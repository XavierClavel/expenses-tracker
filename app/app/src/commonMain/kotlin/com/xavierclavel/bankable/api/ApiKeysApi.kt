package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.ApiKeyIn
import com.xavierclavel.bankable.model.ApiKeyOut
import com.xavierclavel.bankable.model.CreatedApiKeyOut
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/** Where another app sends `POST` requests to add expenses with an API key. */
const val API_KEY_EXPENSES_URL = "$BASE_URL/expenses"

@Serializable
private data class ApiKeyResponse(
    val id: Int,
    val name: String,
    val hint: String,
    val createdAt: String,
    val lastUsedAt: String? = null,
)

@Serializable
private data class CreatedApiKeyResponse(
    val id: Int,
    val name: String,
    val hint: String,
    val createdAt: String,
    val lastUsedAt: String? = null,
    val key: String,
)

/** The user's API keys, oldest first. */
suspend fun apiListApiKeys(): List<ApiKeyOut> {
    val response = httpClient.get("$BASE_URL/api-keys") { authHeader() }
    return response.body<List<ApiKeyResponse>>().map { key ->
        ApiKeyOut(
            id = key.id,
            name = key.name,
            hint = key.hint,
            createdAt = key.createdAt,
            lastUsedAt = key.lastUsedAt,
        )
    }
}

/** Creates a key named [name]. This is the only response that ever carries the full secret. */
suspend fun apiCreateApiKey(name: String): CreatedApiKeyOut {
    val created = httpClient.post("$BASE_URL/api-keys") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(ApiKeyIn(name))
    }.body<CreatedApiKeyResponse>()
    return CreatedApiKeyOut(
        apiKey = ApiKeyOut(
            id = created.id,
            name = created.name,
            hint = created.hint,
            createdAt = created.createdAt,
            lastUsedAt = created.lastUsedAt,
        ),
        key = created.key,
    )
}

/** Revokes the key: requests made with it are refused from now on. */
suspend fun apiDeleteApiKey(id: Int) {
    httpClient.delete("$BASE_URL/api-keys/$id") { authHeader() }
}
