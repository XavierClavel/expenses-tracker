package com.xavierclavel.expenses_tracker.api

import com.xavierclavel.expenses_tracker.model.AccountIn
import com.xavierclavel.expenses_tracker.model.AccountOut
import com.xavierclavel.expenses_tracker.model.AccountTrendDto
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun apiListAccounts(): List<AccountOut> =
    httpClient.get("$BASE_URL/account") { authHeader() }.body()

suspend fun apiCreateAccount(account: AccountIn) {
    httpClient.post("$BASE_URL/account") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(account)
    }
}

suspend fun apiUpdateAccount(id: Int, account: AccountIn) {
    httpClient.put("$BASE_URL/account/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(account)
    }
}

suspend fun apiDeleteAccount(id: Int) {
    httpClient.delete("$BASE_URL/account/$id") { authHeader() }
}

suspend fun apiGetAccountTrendsMonth(accountId: Int): List<AccountTrendDto> =
    httpClient.get("$BASE_URL/account/$accountId/trends/month") { authHeader() }.body()

suspend fun apiGetAccountTrendsYear(accountId: Int): List<AccountTrendDto> =
    httpClient.get("$BASE_URL/account/$accountId/trends/year") { authHeader() }.body()

suspend fun apiGetUserTrendsMonth(): List<AccountTrendDto> =
    httpClient.get("$BASE_URL/account/trends/month") { authHeader() }.body()

suspend fun apiGetUserTrendsYear(): List<AccountTrendDto> =
    httpClient.get("$BASE_URL/account/trends/year") { authHeader() }.body()
