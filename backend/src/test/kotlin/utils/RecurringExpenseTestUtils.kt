package com.xavierclavel.utils

import com.xavierclavel.dtos.RecurringExpenseIn
import com.xavierclavel.dtos.RecurringExpenseOut
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

suspend fun HttpClient.createRecurringExpense(recurringExpense: RecurringExpenseIn): RecurringExpenseOut {
    this.post(RECURRING_EXPENSES_URL){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(recurringExpense)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<RecurringExpenseOut>(bodyAsText())
    }
}

suspend fun HttpClient.updateRecurringExpense(id: Long, recurringExpense: RecurringExpenseIn): RecurringExpenseOut {
    this.put("$RECURRING_EXPENSES_URL/$id"){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(recurringExpense)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<RecurringExpenseOut>(bodyAsText())
    }
}

suspend fun HttpClient.getRecurringExpense(id: Long): RecurringExpenseOut {
    this.get("$RECURRING_EXPENSES_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<RecurringExpenseOut>(bodyAsText())
    }
}

suspend fun HttpClient.listRecurringExpenses(): List<RecurringExpenseOut> {
    this.get(RECURRING_EXPENSES_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<List<RecurringExpenseOut>>(bodyAsText())
    }
}

suspend fun HttpClient.deleteRecurringExpense(id: Long) {
    this.delete("$RECURRING_EXPENSES_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertRecurringExpenseDoesNotExist(id: Long) {
    this.get("$RECURRING_EXPENSES_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}
