package com.xavierclavel.bankable.api

import com.xavierclavel.bankable.model.IdListIn
import com.xavierclavel.bankable.model.InvestmentIn
import com.xavierclavel.bankable.model.InvestmentOut
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun apiListInvestments(accountId: Int): List<InvestmentOut> =
    httpClient.get("$BASE_URL/investment/account/$accountId") { authHeader() }.body()

suspend fun apiCreateInvestment(accountId: Int, investment: InvestmentIn) {
    httpClient.post("$BASE_URL/investment/account/$accountId") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(investment)
    }
}

suspend fun apiUpdateInvestment(id: Long, investment: InvestmentIn) {
    httpClient.put("$BASE_URL/investment/$id") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(investment)
    }
}

suspend fun apiDeleteInvestment(id: Long) {
    httpClient.delete("$BASE_URL/investment/$id") { authHeader() }
}

/** Deletes every investment (transfer) in [ids] in a single request. */
suspend fun apiBatchDeleteInvestments(ids: List<Long>) {
    httpClient.post("$BASE_URL/investment/batch-delete") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(IdListIn(ids))
    }
}
