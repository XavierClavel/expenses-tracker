package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.SummaryDto
import io.ktor.client.call.body
import io.ktor.client.request.get

suspend fun apiGetMonthSummary(year: Int, month: Int): SummaryDto =
    httpClient.get("$BASE_URL/summary/year/$year/month/$month") { authHeader() }.body()

suspend fun apiGetYearSummary(year: Int): SummaryDto =
    httpClient.get("$BASE_URL/summary/year/$year") { authHeader() }.body()
