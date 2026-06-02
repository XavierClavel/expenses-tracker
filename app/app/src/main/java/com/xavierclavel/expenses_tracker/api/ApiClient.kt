package com.xavierclavel.expenses_tracker.api

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
import kotlinx.serialization.json.Json

const val BASE_URL = "http://178.16.131.84:30081"

var sessionToken: String? = null

fun HttpRequestBuilder.authHeader() {
    sessionToken?.let { header("Authorization", "Bearer $it") }
}

class ApiException(val status: Int, message: String) : Exception(message)

val httpClient = HttpClient(Android) {
    install(HttpCookies)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    HttpResponseValidator {
        validateResponse { response ->
            if (!response.status.isSuccess()) {
                val body = response.bodyAsText()
                throw ApiException(response.status.value, "HTTP ${response.status.value}: $body")
            }
        }
    }
}
