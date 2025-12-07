package com.xavierclavel.utils

import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.CategoryOut
import com.xavierclavel.dtos.UserIn
import com.xavierclavel.models.Category
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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

suspend fun HttpClient.createCategory(category: CategoryIn): CategoryOut {
    this.post(CATEGORY_URL){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(category)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val category = Json.decodeFromString<CategoryOut>(bodyAsText())
        return category
    }
}

suspend fun HttpClient.updateCategory(id: Long, category: CategoryIn): CategoryOut {
    this.put("$CATEGORY_URL/$id"){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(category)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val category = Json.decodeFromString<CategoryOut>(bodyAsText())
        return category
    }
}

suspend fun HttpClient.getCategory(id: Long): CategoryOut  {
    this.get("$CATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        val category = Json.decodeFromString<CategoryOut>(bodyAsText())
        return category
    }
}

suspend fun HttpClient.listCategoriesByUser(): List<CategoryOut>  {
    this.get(CATEGORY_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        val categories = Json.decodeFromString<List<CategoryOut>>(bodyAsText())
        return categories
    }
}

suspend fun HttpClient.deleteCategory(id: Long)  {
    this.delete("$CATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertCategoryExists(id: Long) {
    this.get("$CATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertCategoryDoesNotExist(id: Long) {
    this.get("$CATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}