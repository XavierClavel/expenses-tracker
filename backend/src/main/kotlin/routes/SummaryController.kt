package com.xavierclavel.routes

import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.ExpenseService
import com.xavierclavel.services.SummaryService
import com.xavierclavel.utils.SUMMARY_URL
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupSummaryController() = route(SUMMARY_URL) {
    val summaryService: SummaryService by inject()
    val redisService: RedisService by inject()

    get("/user/{user}/year/{year}") {
        val year = call.parameters["year"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val user = call.parameters["user"]?.toLongOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val sessionUserId = getSessionUserId(redisService)
        if (user != sessionUserId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_PERFORMED_ON_SELF)
        }
        val summary = summaryService.summaryOfYear(userId = sessionUserId, year = year)
        call.respond(summary)
    }

    get("/user/{user}/year/{year}/month/{month}") {
        val year = call.parameters["year"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val month = call.parameters["month"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val user = call.parameters["user"]?.toLongOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val sessionUserId = getSessionUserId(redisService)
        if (user != sessionUserId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_PERFORMED_ON_SELF)
        }
        val summary = summaryService.summaryOfMonth(userId = sessionUserId, year = year, month = month)
        call.respond(summary)
    }

    get("/user/{user}/year/{year}/month/{month}/day/{day}") {
        val year = call.parameters["year"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val month = call.parameters["month"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val day = call.parameters["day"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val user = call.parameters["user"]?.toLongOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val sessionUserId = getSessionUserId(redisService)
        if (user != sessionUserId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_PERFORMED_ON_SELF)
        }
        val summary = summaryService.summaryOfDay(userId = sessionUserId, year = year, month = month, day = day)
        call.respond(summary)
    }

}

