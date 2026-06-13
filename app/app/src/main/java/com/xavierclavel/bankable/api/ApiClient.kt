package com.xavierclavel.bankable.api

import android.content.Context
import com.xavierclavel.bankable.storage.PersistentCookiesStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json

const val BASE_URL = "http://178.16.131.84:30081"

// Google "Web application" OAuth client ID
const val GOOGLE_WEB_CLIENT_ID = "467368383996-pje2ab1ks5nehrom7fk59ps81s89dvn8.apps.googleusercontent.com"

var sessionToken: String? = null

val unauthorizedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

fun HttpRequestBuilder.authHeader() {
    sessionToken?.let { header("Authorization", "Bearer $it") }
}

class ApiException(val status: Int, message: String) : Exception(message)

lateinit var httpClient: HttpClient

private var cookiesStorage: PersistentCookiesStorage? = null

/** Clears persisted session cookies. Call on logout so they don't leak across accounts. */
suspend fun clearSessionCookies() {
    cookiesStorage?.clear()
}

fun initHttpClient(context: Context) {
    val storage = PersistentCookiesStorage(context)
    cookiesStorage = storage
    httpClient = HttpClient(Android) {
        install(HttpCookies) {
            this.storage = storage
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        HttpResponseValidator {
            validateResponse { response ->
                if (!response.status.isSuccess()) {
                    if (response.status.value == 401) {
                        unauthorizedFlow.tryEmit(Unit)
                    }
                    val body = response.bodyAsText()
                    throw ApiException(response.status.value, "HTTP ${response.status.value}: $body")
                }
            }
        }
    }
}
