package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.CategoryIn
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.SubcategoryOut
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class SubcategoryResponse(
    val id: Int,
    val name: String,
    val color: String? = null,
    val icon: String,
    val isDefault: Boolean,
)

@Serializable
private data class CategoryResponse(
    val id: Int,
    val name: String,
    val color: String,
    val icon: String,
    val type: String,
    val subcategories: List<SubcategoryResponse>,
)

suspend fun apiListCategories(): List<CategoryOut> {
    val response = httpClient.get("$BASE_URL/categories") { authHeader() }
    return response.body<List<CategoryResponse>>().map { cat ->
        CategoryOut(
            id = cat.id,
            name = cat.name,
            color = cat.color,
            icon = cat.icon,
            type = cat.type,
            subcategories = cat.subcategories.map { sub ->
                SubcategoryOut(
                    id = sub.id,
                    name = if (sub.isDefault) "${cat.name} - Default" else sub.name,
                    type = cat.type,
                    color = sub.color ?: cat.color,
                    icon = sub.icon,
                    isDefault = sub.isDefault,
                )
            },
        )
    }
}

suspend fun apiCreateCategory(category: CategoryIn) {
    httpClient.post("$BASE_URL/categories") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(category)
    }
}

suspend fun apiUpdateCategory(id: Int, category: CategoryIn) {
    httpClient.put("$BASE_URL/categories/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(category)
    }
}

suspend fun apiDeleteCategory(id: Int) {
    httpClient.delete("$BASE_URL/categories/$id") { authHeader() }
}
