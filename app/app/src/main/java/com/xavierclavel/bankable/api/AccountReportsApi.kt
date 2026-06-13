package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.AccountReportIn
import com.xavierclavel.bankable.model.AccountReportOut
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun apiListAccountReports(accountId: Int, page: Int, size: Int): List<AccountReportOut> =
    httpClient.get("$BASE_URL/account-report/account/$accountId") {
        authHeader()
        parameter("page", page)
        parameter("size", size)
    }.body()

suspend fun apiCreateAccountReport(accountId: Int, report: AccountReportIn) {
    httpClient.post("$BASE_URL/account-report/account/$accountId") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(report)
    }
}

suspend fun apiUpdateAccountReport(id: Int, report: AccountReportIn) {
    httpClient.put("$BASE_URL/account-report/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(report)
    }
}

suspend fun apiDeleteAccountReport(id: Int) {
    httpClient.delete("$BASE_URL/account-report/$id") { authHeader() }
}
