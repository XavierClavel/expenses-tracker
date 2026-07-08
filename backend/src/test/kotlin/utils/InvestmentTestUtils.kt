package com.xavierclavel.utils

import com.xavierclavel.dtos.investment.InvestmentIn
import com.xavierclavel.dtos.investment.InvestmentOut
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

suspend fun HttpClient.createInvestment(accountId: Long, investment: InvestmentIn): InvestmentOut {
    this.post("$INVESTMENT_URL/account/$accountId") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(investment)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<InvestmentOut>(bodyAsText())
    }
}

suspend fun HttpClient.updateInvestment(id: Long, investment: InvestmentIn): InvestmentOut {
    this.put("$INVESTMENT_URL/$id") {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(investment)
    }.apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<InvestmentOut>(bodyAsText())
    }
}

suspend fun HttpClient.getInvestment(id: Long): InvestmentOut {
    this.get("$INVESTMENT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<InvestmentOut>(bodyAsText())
    }
}

suspend fun HttpClient.listInvestments(): List<InvestmentOut> {
    this.get(INVESTMENT_URL).apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<List<InvestmentOut>>(bodyAsText())
    }
}

suspend fun HttpClient.listInvestmentsByAccount(accountId: Long): List<InvestmentOut> {
    this.get("$INVESTMENT_URL/account/$accountId").apply {
        assertEquals(HttpStatusCode.OK, status)
        return Json.decodeFromString<List<InvestmentOut>>(bodyAsText())
    }
}

suspend fun HttpClient.deleteInvestment(id: Long) {
    this.delete("$INVESTMENT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertInvestmentExists(id: Long) {
    this.get("$INVESTMENT_URL/$id").apply {
        assertEquals(HttpStatusCode.OK, status)
    }
}

suspend fun HttpClient.assertInvestmentDoesNotExist(id: Long) {
    this.get("$INVESTMENT_URL/$id").apply {
        assertEquals(HttpStatusCode.NotFound, status)
    }
}
