package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.SubcategoryOutlierDto
import com.xavierclavel.bankable.model.SummaryDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

suspend fun apiGetMonthSummary(year: Int, month: Int): SummaryDto =
    httpClient.get("$BASE_URL/summary/year/$year/month/$month") { authHeader() }.body()

suspend fun apiGetYearSummary(year: Int): SummaryDto =
    httpClient.get("$BASE_URL/summary/year/$year") { authHeader() }.body()

suspend fun apiGetYearOutliers(
    year: Int,
    threshold: Double,
    minTotal: Int,
    yearsAround: Int,
): List<SubcategoryOutlierDto> =
    httpClient.get("$BASE_URL/summary/year/$year/outliers") {
        authHeader()
        parameter("threshold", threshold)
        parameter("minTotal", minTotal)
        parameter("yearsAround", yearsAround)
    }.body()
