package com.xavierclavel.utils

import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.dtos.investment.InvestmentAccountOut
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

suspend fun HttpClient.createAccount(account: InvestmentAccountIn): InvestmentAccountOut {
    this.post(ACCOUNT_URL){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(account)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val account = Json.decodeFromString<InvestmentAccountOut>(bodyAsText())
        return account
    }
}

suspend fun HttpClient.updateAccount(id: Long, account: InvestmentAccountIn): InvestmentAccountOut {
    this.put("$ACCOUNT_URL/$id"){
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(account)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        val account = Json.decodeFromString<InvestmentAccountOut>(bodyAsText())
        return account
    }
}

suspend fun HttpClient.getAccount(id: Long): InvestmentAccountOut {
    this.get("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        val account = Json.decodeFromString<InvestmentAccountOut>(bodyAsText())
        return account
    }
}

suspend fun HttpClient.listAccounts(): List<InvestmentAccountOut>  {
    this.get(ACCOUNT_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        val categories = Json.decodeFromString<List<InvestmentAccountOut>>(bodyAsText())
        return categories
    }
}

suspend fun HttpClient.deleteAccount(id: Long)  {
    this.delete("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertAccountExists(id: Long) {
    this.get("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertAccountDoesNotExist(id: Long) {
    this.get("$ACCOUNT_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}