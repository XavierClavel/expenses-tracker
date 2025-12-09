package com.xavierclavel.utils

import com.xavierclavel.dtos.summary.SummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals


suspend fun HttpClient.getSummary(year: Int, month: Int): SummaryDto {
    this.get("$SUMMARY_URL/year/$year/month/$month").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<SummaryDto>(bodyAsText())
        return expense
    }
}