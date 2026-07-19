package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.TagIn
import com.xavierclavel.bankable.model.TagOut
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun apiListTags(): List<TagOut> {
    return httpClient.get("$BASE_URL/tags") { authHeader() }.body()
}

suspend fun apiCreateTag(tag: TagIn) {
    httpClient.post("$BASE_URL/tags") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(tag)
    }
}

suspend fun apiUpdateTag(id: Int, tag: TagIn) {
    httpClient.put("$BASE_URL/tags/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(tag)
    }
}

suspend fun apiDeleteTag(id: Int) {
    httpClient.delete("$BASE_URL/tags/$id") { authHeader() }
}
