package com.xavierclavel.bankable.api

import android.util.Base64
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class LoginResponse(val token: String)

@Serializable
private data class SignupRequest(val emailAddress: String, val password: String)

@Serializable
private data class GoogleLoginRequest(val idToken: String)

suspend fun apiLogin(email: String, password: String): String {
    val credentials = Base64.encodeToString("$email:$password".toByteArray(), Base64.NO_WRAP)
    val response = httpClient.post("$BASE_URL/auth/login") {
        header("Authorization", "Basic $credentials")
    }
    return response.body<LoginResponse>().token
}

suspend fun apiLoginGoogle(idToken: String): String {
    val response = httpClient.post("$BASE_URL/auth/login-google") {
        contentType(ContentType.Application.Json)
        setBody(GoogleLoginRequest(idToken))
    }
    return response.body<LoginResponse>().token
}

suspend fun apiSignup(email: String, password: String) {
    httpClient.post("$BASE_URL/auth/signup") {
        contentType(ContentType.Application.Json)
        setBody(SignupRequest(email, password))
    }
}

suspend fun apiFetchMe(token: String): Boolean {
    val response = httpClient.get("$BASE_URL/auth/me") {
        header("Authorization", "Bearer $token")
    }
    return response.status.value in 200..299
}

// Permanently deletes the authenticated user's account and all associated data.
suspend fun apiDeleteAccount() {
    httpClient.delete("$BASE_URL/users") { authHeader() }
}
