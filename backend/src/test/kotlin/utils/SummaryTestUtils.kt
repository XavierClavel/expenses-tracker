package com.xavierclavel.utils

import com.xavierclavel.dtos.summary.SubcategoryOutlier
import com.xavierclavel.dtos.summary.SummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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

suspend fun HttpClient.getYearOutliers(
    year: Int,
    threshold: Double? = null,
    minTotal: Int? = null,
    yearsAround: Int? = null,
): List<SubcategoryOutlier> {
    this.get("$SUMMARY_URL/year/$year/outliers") {
        threshold?.let { parameter("threshold", it) }
        minTotal?.let { parameter("minTotal", it) }
        yearsAround?.let { parameter("yearsAround", it) }
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<List<SubcategoryOutlier>>(bodyAsText())
    }
}
