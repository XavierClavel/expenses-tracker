package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.SubcategoryIn
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun apiCreateSubcategory(subcategory: SubcategoryIn) {
    httpClient.post("$BASE_URL/subcategories") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(subcategory)
    }
}

suspend fun apiUpdateSubcategory(id: Int, subcategory: SubcategoryIn) {
    httpClient.put("$BASE_URL/subcategories/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(subcategory)
    }
}

suspend fun apiDeleteSubcategory(id: Int) {
    httpClient.delete("$BASE_URL/subcategories/$id") { authHeader() }
}
