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
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupTrendController() = route(TREND_URL) {
    val redisService: RedisService by inject()
    val trendService: TrendService by inject()

    get("/category/{id}/year") {
        val categoryId = getPathId()
        val userId = getSessionUserId(redisService)
        val result = trendService.categoryTrendByYear(userId = userId, categoryId = categoryId)
        call.respond(result)
    }

    get("/category/{id}/month") {
        val categoryId = getPathId()
        val userId = getSessionUserId(redisService)
        val result = trendService.categoryTrendByMonth(userId = userId, categoryId = categoryId)
        call.respond(result)
    }

    get("/subcategory/{id}/year") {
        val categoryId = getPathId()
        val userId = getSessionUserId(redisService)
        val result = trendService.subcategoryTrendByYear(userId = userId, categoryId = categoryId)
        call.respond(result)
    }

    get("/subcategory/{id}/month") {
        val categoryId = getPathId()
        val userId = getSessionUserId(redisService)
        val result = trendService.subcategoryTrendByMonth(userId = userId, categoryId = categoryId)
        call.respond(result)
    }

    get("flow/year") {
        val userId = getSessionUserId(redisService)
        val result = trendService.flowByYear(userId = userId)
        call.respond(result)
    }

    get("year") {
        val sessionUserId = getSessionUserId(redisService)
        val summary = trendService.trendByYear(userId = sessionUserId)
        call.respond(summary)
    }


    get("year/median") {
        val sessionUserId = getSessionUserId(redisService)
        val summary = trendService.medianByYear(userId = sessionUserId)
        call.respond(summary)
    }

    get("month") {
        val sessionUserId = getSessionUserId(redisService)
        val summary = trendService.trendByMonth(userId = sessionUserId)
        call.respond(summary)
    }

}

