package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.ExpenseIn
import com.xavierclavel.bankable.model.ExpenseOut
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
private data class OldestExpenseResponse(val date: String? = null)

suspend fun apiListExpenses(
    page: Int,
    size: Int,
    categoryId: Int? = null,
    subcategoryId: Int? = null,
    type: String? = null,
    from: String? = null,
    to: String? = null,
    query: String? = null,
    minAmount: String? = null,
    maxAmount: String? = null,
): List<ExpenseOut> {
    return httpClient.get("$BASE_URL/expenses") {
        authHeader()
        parameter("page", page)
        parameter("size", size)
        categoryId?.let { parameter("categoryId", it) }
        subcategoryId?.let { parameter("subcategoryId", it) }
        type?.let { parameter("type", it) }
        from?.let { parameter("from", it) }
        to?.let { parameter("to", it) }
        query?.takeIf { it.isNotBlank() }?.let { parameter("query", it) }
        minAmount?.let { parameter("minAmount", it) }
        maxAmount?.let { parameter("maxAmount", it) }
    }.body()
}

suspend fun apiCreateExpense(expense: ExpenseIn) {
    httpClient.post("$BASE_URL/expenses") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(expense)
    }
}

suspend fun apiUpdateExpense(id: Int, expense: ExpenseIn) {
    httpClient.put("$BASE_URL/expenses/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(expense)
    }
}

suspend fun apiDeleteExpense(id: Int) {
    httpClient.delete("$BASE_URL/expenses/$id") { authHeader() }
}

suspend fun apiGetOldestExpenseDate(): String? {
    return httpClient.get("$BASE_URL/expenses/oldest") { authHeader() }
        .body<OldestExpenseResponse>().date
}
