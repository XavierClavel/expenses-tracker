package com.xavierclavel.utils

import com.xavierclavel.dtos.ApiKeyCreatedOut
import com.xavierclavel.dtos.ApiKeyIn
import com.xavierclavel.dtos.ApiKeyOut
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
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

suspend fun HttpClient.createApiKey(name: String): ApiKeyCreatedOut {
    this.post(API_KEY_URL){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(ApiKeyIn(name = name))
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<ApiKeyCreatedOut>(bodyAsText())
    }
}

suspend fun HttpClient.listApiKeys(): List<ApiKeyOut> {
    this.get(API_KEY_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<List<ApiKeyOut>>(bodyAsText())
    }
}

suspend fun HttpClient.deleteApiKey(id: Long) {
    this.delete("$API_KEY_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}
