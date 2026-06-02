package com.xavierclavel.expenses_tracker.api

import com.xavierclavel.expenses_tracker.model.ExpenseIn
import com.xavierclavel.expenses_tracker.model.ExpenseOut
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
    subcategoryId: Int? = null,
    from: String? = null,
    to: String? = null,
): List<ExpenseOut> {
    return httpClient.get("$BASE_URL/expenses") {
        authHeader()
        parameter("page", page)
        parameter("size", size)
        subcategoryId?.let { parameter("subcategoryId", it) }
        from?.let { parameter("from", it) }
        to?.let { parameter("to", it) }
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
