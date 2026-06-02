package com.xavierclavel.expenses_tracker.api

import com.xavierclavel.expenses_tracker.model.SummaryDto
import io.ktor.client.call.body
import io.ktor.client.request.get

suspend fun apiGetMonthSummary(year: Int, month: Int): SummaryDto =
    httpClient.get("$BASE_URL/summary/year/$year/month/$month") { authHeader() }.body()

suspend fun apiGetYearSummary(year: Int): SummaryDto =
    httpClient.get("$BASE_URL/summary/year/$year") { authHeader() }.body()
