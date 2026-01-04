package com.xavierclavel.routes

import com.xavierclavel.dtos.investment.AccountReportIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.AccountReportService
import com.xavierclavel.services.AccountService
import com.xavierclavel.utils.ACCOUNT_REPORT_URL
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupAccountReportController() = route(ACCOUNT_REPORT_URL) {
    val accountReportService: AccountReportService by inject()
    val redisService: RedisService by inject()

        /**
         * Retrieves all users registered in the system.
         *
         * @response 200 OK - Returns a list of User objects
         */
        get {
            val sessionUserId = getSessionUserId(redisService)
            val reports = accountReportService.list(userId = sessionUserId)
            call.respond(reports)
        }

        /**
         * Retrieves the user that matches the id.
         *
         * @response 200 OK - Returns a User object that matches the given id
         * @response 404 Not Found - If no user exists with the provided id
         */
        get("/{id}") {
            val userId = getSessionUserId(redisService)
            val reportId = getPathId()
            val report = accountReportService.get(userId = userId, reportId = reportId)
            call.respond(report)
        }

        post("/account/{id}") {
            val accountId = getPathId()
            val userId = getSessionUserId(redisService)
            val reportDto = call.receive<AccountReportIn>()
            val report = accountReportService.create(userId = userId, reportDto = reportDto, accountId = accountId)
            call.respond(report)
        }

        put("/{id}") {
            val reportId = getPathId()
            val userId = getSessionUserId(redisService)
            val reportDto = call.receive<AccountReportIn>()
            val report = accountReportService.update(userId = userId, reportId = reportId, reportDto = reportDto)
            call.respond(report)
        }

        delete("/{id}") {
            val reportId = getPathId()
            val userId = getSessionUserId(redisService)
            accountReportService.delete(userId = userId, reportId = reportId)
            call.respond(HttpStatusCode.OK)
        }

    }

