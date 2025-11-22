package com.xavierclavel.utils

import com.xavierclavel.dtos.auth.SignupDto
import com.xavierclavel.dtos.UserOut
import io.ktor.client.HttpClient
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

suspend fun HttpClient.signup(dto: SignupDto): UserOut  {
    this.post("$AUTH_URL/signup"){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(dto)
    }.apply {
        assertEquals(HttpStatusCode.Created, status)
        val user = Json.decodeFromString<UserOut>(bodyAsText())
        return user
    }
}

suspend fun HttpClient.getMe(): UserOut  {
    this.get("$AUTH_URL/me").apply {
        assertEquals(HttpStatusCode.OK, status)
        val user = Json.decodeFromString<UserOut>(bodyAsText())
        return user
    }
}

suspend fun HttpClient.login(mail: String, password: String) =
    this.post("$AUTH_URL/login") {
        basicAuth(username = mail, password = password)
    }


suspend fun HttpClient.logout() =
    this.post("$AUTH_URL/logout")
