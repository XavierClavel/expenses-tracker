package com.xavierclavel.utils

import com.xavierclavel.dtos.TrendDto
import com.xavierclavel.dtos.dtos.CategoryTrendDto
import com.xavierclavel.dtos.summary.SummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals


suspend fun HttpClient.getYearTrends(): List<TrendDto> {
    this.get("$TREND_URL/year").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<List<TrendDto>>(bodyAsText())
        return expense
    }
}

suspend fun HttpClient.getMonthTrends(): List<TrendDto> {
    this.get("$TREND_URL/month").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<List<TrendDto>>(bodyAsText())
        return expense
    }
}

suspend fun HttpClient.getYearCategoryTrends(categoryId: Long): List<CategoryTrendDto> {
    this.get("$TREND_URL/category/${categoryId}/year").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<List<CategoryTrendDto>>(bodyAsText())
        return expense
    }
}

suspend fun HttpClient.getMonthCategoryTrends(categoryId: Long): List<CategoryTrendDto> {
    this.get("$TREND_URL/category/${categoryId}/month").apply {
        assertEquals(HttpStatusCode.OK, status)
        val expense = Json.decodeFromString<List<CategoryTrendDto>>(bodyAsText())
        return expense
    }
}