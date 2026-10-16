package com.xavierclavel.utils

import com.xavierclavel.dtos.sync.SyncChanges
import com.xavierclavel.dtos.sync.SyncPullOut
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

suspend fun HttpClient.pull(since: Long = 0, limit: Int? = null): SyncPullOut {
    this.get("$SYNC_URL/pull") {
        parameter("since", since)
        limit?.let { parameter("limit", it) }
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<SyncPullOut>(bodyAsText())
    }
}

suspend fun HttpClient.push(changes: SyncChanges, expectedStatus: HttpStatusCode = HttpStatusCode.OK) {
    this.post("$SYNC_URL/push") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(changes)
    }.apply {
        assertEquals(expectedStatus, status)
    }
}
