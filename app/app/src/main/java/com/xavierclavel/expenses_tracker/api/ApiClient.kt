package com.xavierclavel.expenses_tracker.api

import android.content.Context
import com.xavierclavel.expenses_tracker.storage.PersistentCookiesStorage
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

var sessionToken: String? = null

val unauthorizedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

fun HttpRequestBuilder.authHeader() {
    sessionToken?.let { header("Authorization", "Bearer $it") }
}

class ApiException(val status: Int, message: String) : Exception(message)

lateinit var httpClient: HttpClient

fun initHttpClient(context: Context) {
    httpClient = HttpClient(Android) {
        install(HttpCookies) {
            storage = PersistentCookiesStorage(context)
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
