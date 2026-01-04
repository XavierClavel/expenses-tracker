package com.xavierclavel.routes

import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.AccountService
import com.xavierclavel.utils.ACCOUNT_URL
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupAccountController() = route(ACCOUNT_URL) {
    val accountService: AccountService by inject()
    val redisService: RedisService by inject()

        /**
         * Retrieves all users registered in the system.
         *
         * @response 200 OK - Returns a list of User objects
         */
        get {
            val sessionUserId = getSessionUserId(redisService)
            val accounts = accountService.list(userId = sessionUserId)
            call.respond(accounts)
        }

        get("/trends/account/{id}/month") {
            val sessionUserId = getSessionUserId(redisService)
            val accountId = getPathId()
            val result = accountService.trendByAccountByMonth(userId = sessionUserId, accountId = accountId)
            call.respond(result)
        }

        /**
         * Retrieves the user that matches the id.
         *
         * @response 200 OK - Returns a User object that matches the given id
         * @response 404 Not Found - If no user exists with the provided id
         */
        get("/{id}") {
            val userId = getSessionUserId(redisService)
            val accountId = getPathId()
            val account = accountService.get(userId = userId, accountId = accountId)
            call.respond(account)
        }

        post {
            val userId = getSessionUserId(redisService)
            val accountDto = call.receive<InvestmentAccountIn>()
            val account = accountService.create(userId = userId, accountDto = accountDto)
            call.respond(account)
        }

        put("/{id}") {
            val accountId = getPathId()
            val userId = getSessionUserId(redisService)
            val accountDto = call.receive<InvestmentAccountIn>()
            val account = accountService.update(userId = userId, accountId = accountId, accountDto = accountDto)
            call.respond(account)
        }

        delete("/{id}") {
            val accountId = getPathId()
            val userId = getSessionUserId(redisService)
            accountService.delete(userId = userId, accountId = accountId)
            call.respond(HttpStatusCode.OK)
        }

    }

