package com.xavierclavel.utils

import com.xavierclavel.dtos.SubcategoryIn
import com.xavierclavel.dtos.SubcategoryOut
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

suspend fun HttpClient.createSubcategory(category: SubcategoryIn): SubcategoryOut {
    this.post(SUBCATEGORY_URL){
        contentType(ContentType.Application.Json)
        setBody(category)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val category = Json.decodeFromString<SubcategoryOut>(bodyAsText())
        return category
    }
}

suspend fun HttpClient.updateSubcategory(id: Long, category: SubcategoryIn): SubcategoryOut {
    this.put("$SUBCATEGORY_URL/$id"){
        contentType(ContentType.Application.Json)
        setBody(category)
    }.apply {
        val b = bodyAsText()
        println(b)
        assertEquals(HttpStatusCode.OK, status)
        val category = Json.decodeFromString<SubcategoryOut>(bodyAsText())
        return category
    }
}

suspend fun HttpClient.getSubcategory(id: Long): SubcategoryOut  {
    this.get("$SUBCATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        val category = Json.decodeFromString<SubcategoryOut>(bodyAsText())
        return category
    }
}

suspend fun HttpClient.listSubcategoriesByUser(): List<SubcategoryOut>  {
    this.get(SUBCATEGORY_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        val categories = Json.decodeFromString<List<SubcategoryOut>>(bodyAsText())
        return categories
    }
}

suspend fun HttpClient.deleteSubcategory(id: Long)  {
    this.delete("$SUBCATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertSubcategoryExists(id: Long) {
    this.get("$SUBCATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertSubcategoryDoesNotExist(id: Long) {
    this.get("$SUBCATEGORY_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}