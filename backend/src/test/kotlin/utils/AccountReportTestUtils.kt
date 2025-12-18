package com.xavierclavel.utils

import com.xavierclavel.dtos.investment.AccountReportIn
import com.xavierclavel.dtos.investment.AccountReportOut
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

suspend fun HttpClient.createAccountReport(account: AccountReportIn): AccountReportOut {
    this.post(ACCOUNT_URL){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(account)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val account = Json.decodeFromString<AccountReportOut>(bodyAsText())
        return account
    }
}

suspend fun HttpClient.updateAccountReport(id: Long, account: AccountReportIn): AccountReportOut {
    this.put("$ACCOUNT_URL/$id"){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(account)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val account = Json.decodeFromString<AccountReportOut>(bodyAsText())
        return account
    }
}

suspend fun HttpClient.getAccountReport(id: Long): AccountReportOut {
    this.get("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        val account = Json.decodeFromString<AccountReportOut>(bodyAsText())
        return account
    }
}

suspend fun HttpClient.listAccountReports(): List<AccountReportOut>  {
    this.get(ACCOUNT_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        val categories = Json.decodeFromString<List<AccountReportOut>>(bodyAsText())
        return categories
    }
}

suspend fun HttpClient.deleteAccountReport(id: Long)  {
    this.delete("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertAccountReportExists(id: Long) {
    this.get("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertAccountReportDoesNotExist(id: Long) {
    this.get("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}