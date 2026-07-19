package com.xavierclavel.utils

import com.xavierclavel.dtos.TagIn
import com.xavierclavel.dtos.TagOut
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

suspend fun HttpClient.createTag(tag: TagIn): TagOut {
    this.post(TAG_URL){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(tag)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<TagOut>(bodyAsText())
    }
}

suspend fun HttpClient.updateTag(id: Long, tag: TagIn): TagOut {
    this.put("$TAG_URL/$id"){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(tag)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<TagOut>(bodyAsText())
    }
}

suspend fun HttpClient.getTag(id: Long): TagOut {
    this.get("$TAG_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<TagOut>(bodyAsText())
    }
}

suspend fun HttpClient.listTagsByUser(): List<TagOut> {
    this.get(TAG_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<List<TagOut>>(bodyAsText())
    }
}

suspend fun HttpClient.deleteTag(id: Long) {
    this.delete("$TAG_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertTagExists(id: Long) {
    this.get("$TAG_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertTagDoesNotExist(id: Long) {
    this.get("$TAG_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}
