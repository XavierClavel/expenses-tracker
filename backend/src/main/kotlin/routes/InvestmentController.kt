package com.xavierclavel.routes

import com.xavierclavel.dtos.IdListIn
import com.xavierclavel.dtos.investment.InvestmentIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.InvestmentService
import com.xavierclavel.utils.INVESTMENT_URL
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupInvestmentController() = route(INVESTMENT_URL) {
    val investmentService: InvestmentService by inject()
    val redisService: RedisService by inject()

        get {
            val sessionUserId = getSessionUserId(redisService)
            val investments = investmentService.list(userId = sessionUserId)
            call.respond(investments)
        }

        get("/{id}") {
            val userId = getSessionUserId(redisService)
            val investmentId = getPathId()
            val investment = investmentService.get(userId = userId, investmentId = investmentId)
            call.respond(investment)
        }

        get("/account/{id}") {
            val userId = getSessionUserId(redisService)
            val accountId = getPathId()
            val investments = investmentService.list(userId = userId, accountId = accountId)
            call.respond(investments)
        }

        post("/account/{id}") {
            val accountId = getPathId()
            val userId = getSessionUserId(redisService)
            val investmentDto = call.receive<InvestmentIn>()
            val investment = investmentService.create(userId = userId, investmentDto = investmentDto, accountId = accountId)
            call.respond(investment)
        }

        put("/{id}") {
            val investmentId = getPathId()
            val userId = getSessionUserId(redisService)
            val investmentDto = call.receive<InvestmentIn>()
            val investment = investmentService.update(userId = userId, investmentId = investmentId, investmentDto = investmentDto)
            call.respond(investment)
        }

        delete("/{id}") {
            val investmentId = getPathId()
            val userId = getSessionUserId(redisService)
            investmentService.delete(userId = userId, investmentId = investmentId)
            call.respond(HttpStatusCode.OK)
        }

        post("/batch-delete") {
            val userId = getSessionUserId(redisService)
            val dto = call.receive<IdListIn>()
            investmentService.batchDelete(userId = userId, ids = dto.ids)
            call.respond(HttpStatusCode.OK)
        }

    }
