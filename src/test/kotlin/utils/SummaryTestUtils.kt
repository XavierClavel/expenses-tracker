package com.xavierclavel.utils

import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.dtos.MonthSummary
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


suspend fun HttpClient.getSummary(id: Long, year: Int, month: Int): MonthSummary {
    this.get("$EXPENSES_URL/user/$id/year/$year/month/$month").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<MonthSummary>(bodyAsText())
        return expense
    }
}