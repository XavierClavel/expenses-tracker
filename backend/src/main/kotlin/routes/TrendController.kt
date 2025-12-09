package com.xavierclavel.routes

import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.ExpenseService
import com.xavierclavel.services.SummaryService
import com.xavierclavel.services.TrendService
import com.xavierclavel.utils.SUMMARY_URL
import com.xavierclavel.utils.TREND_URL
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupTrendController() = route(TREND_URL) {
    val redisService: RedisService by inject()
    val trendService: TrendService by inject()

    get("year") {
        val sessionUserId = getSessionUserId(redisService)
        val summary = trendService.trendByYear(userId = sessionUserId)
        call.respond(summary)
    }

    get("month") {
        val sessionUserId = getSessionUserId(redisService)
        val summary = trendService.trendByMonth(userId = sessionUserId)
        call.respond(summary)
    }

}

