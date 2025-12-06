package com.xavierclavel.utils

import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.ExpenseOut
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

suspend fun HttpClient.createExpense(expense: ExpenseIn): ExpenseOut {
    this.post(EXPENSES_URL){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(expense)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<ExpenseOut>(bodyAsText())
        return expense
    }
}

suspend fun HttpClient.updateExpense(id: Long, expense: ExpenseIn): ExpenseOut {
    this.put("$EXPENSES_URL/$id"){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(expense)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<ExpenseOut>(bodyAsText())
        return expense
    }
}

suspend fun HttpClient.getExpense(id: Long): ExpenseOut {
    this.get("$EXPENSES_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<ExpenseOut>(bodyAsText())
        return expense
    }
}

suspend fun HttpClient.listExpenses(): List<ExpenseOut>  {
    this.get(EXPENSES_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        val categories = Json.decodeFromString<List<ExpenseOut>>(bodyAsText())
        return categories
    }
}

suspend fun HttpClient.deleteExpense(id: Long)  {
    this.delete("$EXPENSES_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertExpenseExists(id: Long) {
    this.get("$EXPENSES_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertExpenseDoesNotExist(id: Long) {
    this.get("$EXPENSES_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}