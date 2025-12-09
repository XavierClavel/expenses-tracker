package com.xavierclavel.utils

import com.xavierclavel.dtos.summary.MonthSummary
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals


suspend fun HttpClient.getSummary(year: Int, month: Int): MonthSummary {
    this.get("$SUMMARY_URL/year/$year/month/$month").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<MonthSummary>(bodyAsText())
        return expense
    }
}