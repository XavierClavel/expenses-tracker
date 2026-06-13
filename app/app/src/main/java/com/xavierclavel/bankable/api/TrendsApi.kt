package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.CategoryTrendDto
import com.xavierclavel.bankable.model.TrendDto
import io.ktor.client.call.body
import io.ktor.client.request.get

suspend fun apiGetMonthTrends(): List<TrendDto> =
    httpClient.get("$BASE_URL/trends/month") { authHeader() }.body()

suspend fun apiGetYearTrends(): List<TrendDto> =
    httpClient.get("$BASE_URL/trends/year") { authHeader() }.body()

suspend fun apiGetMonthCategoryTrends(id: Int): List<CategoryTrendDto> =
    httpClient.get("$BASE_URL/trends/category/$id/month") { authHeader() }.body()

suspend fun apiGetYearCategoryTrends(id: Int): List<CategoryTrendDto> =
    httpClient.get("$BASE_URL/trends/category/$id/year") { authHeader() }.body()

suspend fun apiGetMonthSubcategoryTrends(id: Int): List<CategoryTrendDto> =
    httpClient.get("$BASE_URL/trends/subcategory/$id/month") { authHeader() }.body()

suspend fun apiGetYearSubcategoryTrends(id: Int): List<CategoryTrendDto> =
    httpClient.get("$BASE_URL/trends/subcategory/$id/year") { authHeader() }.body()

suspend fun apiGetYearFlowTrends(): List<CategoryTrendDto> =
    httpClient.get("$BASE_URL/trends/flow/year") { authHeader() }.body()
