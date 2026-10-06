package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.IdListIn
import com.xavierclavel.bankable.model.RecurringExpenseIn
import com.xavierclavel.bankable.model.RecurringExpenseOut
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun apiListRecurringExpenses(): List<RecurringExpenseOut> {
    return httpClient.get("$BASE_URL/recurring-expenses") { authHeader() }.body()
}

suspend fun apiCreateRecurringExpense(recurringExpense: RecurringExpenseIn): RecurringExpenseOut {
    return httpClient.post("$BASE_URL/recurring-expenses") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(recurringExpense)
    }.body()
}

/**
 * Creates a recurring expense from each expense in [expenseIds], repeating on its day of month.
 * Each expense counts as its month's occurrence, so it is never duplicated.
 */
suspend fun apiCreateRecurringExpensesFromExpenses(expenseIds: List<Int>): List<RecurringExpenseOut> {
    return httpClient.post("$BASE_URL/recurring-expenses/from-expenses") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(IdListIn(expenseIds.map { it.toLong() }))
    }.body()
}

suspend fun apiUpdateRecurringExpense(id: Int, recurringExpense: RecurringExpenseIn): RecurringExpenseOut {
    return httpClient.put("$BASE_URL/recurring-expenses/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(recurringExpense)
    }.body()
}

suspend fun apiDeleteRecurringExpense(id: Int) {
    httpClient.delete("$BASE_URL/recurring-expenses/$id") { authHeader() }
}
