package com.xavierclavel.utils

import com.xavierclavel.dtos.DateDto
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.ExpenseOut
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import java.time.LocalDate
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

suspend fun HttpClient.getOldestActivity(): LocalDate? {
    this.get("$EXPENSES_URL/oldest").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<DateDto>(bodyAsText())
        return expense.date
    }
}

suspend fun HttpClient.listExpenses(params: Map<String, String> = mapOf()): Set<ExpenseOut>  {
    this.get(EXPENSES_URL, {
        params.forEach {
            this.parameter(it.key, it.value)
        }
    }).apply {
        assertEquals(HttpStatusCode.OK, status)
        val categories = Json.decodeFromString<Set<ExpenseOut>>(bodyAsText())
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