package com.xavierclavel.utils

import com.xavierclavel.dtos.UserIn
import com.xavierclavel.dtos.UserOut
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

suspend fun HttpClient.getUser(id: Long): UserOut  {
    this.get("$USERS_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        val user = Json.decodeFromString<UserOut>(bodyAsText())
        return user
    }
}

suspend fun HttpClient.listUsers(): List<UserOut>  {
    this.get(USERS_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        val users = Json.decodeFromString<List<UserOut>>(bodyAsText())
        return users
    }
}

suspend fun HttpClient.deleteUser(id: Long)  {
    this.delete("$USERS_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertUserExists(id: Long) {
    this.get("$USERS_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertUserDoesNotExist(id: Long) {
    this.get("$USERS_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}