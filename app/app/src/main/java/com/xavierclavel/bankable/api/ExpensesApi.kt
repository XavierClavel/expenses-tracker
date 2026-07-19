package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.ExpenseIn
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.IdListIn
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

@Serializable
private data class TagOperation(
    val tagId: Int,
    val operation: String,
    val expenseIds: List<Int>,
)

@Serializable
private data class ExpenseBatchIn(
    val tagOperations: List<TagOperation> = emptyList(),
)

/** Adds ([add] = true) or removes ([add] = false) [tagId] on every expense in [expenseIds]. */
suspend fun apiBatchTagExpenses(expenseIds: List<Int>, tagId: Int, add: Boolean) {
    httpClient.put("$BASE_URL/expenses/batch") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(
            ExpenseBatchIn(
                listOf(TagOperation(tagId, if (add) "ADD" else "REMOVE", expenseIds)),
            ),
        )
    }
}

/** Deletes every expense in [ids] in a single request. */
suspend fun apiBatchDeleteExpenses(ids: List<Int>) {
    httpClient.post("$BASE_URL/expenses/batch-delete") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(IdListIn(ids.map { it.toLong() }))
    }
}

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
    tagId: Int? = null,
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
        tagId?.let { parameter("tagId", it) }
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
