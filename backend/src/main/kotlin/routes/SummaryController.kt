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
import java.math.BigDecimal
import org.koin.ktor.ext.inject

// How far above its monthly mean a subcategory must run to count as an outlier.
private const val DEFAULT_OUTLIER_THRESHOLD = 0.5
// The smallest yearly total worth flagging.
private val DEFAULT_OUTLIER_MIN_TOTAL = BigDecimal(250)
// The mean spans this many years either side of the year looked at.
private const val DEFAULT_OUTLIER_YEARS_AROUND = 2

fun Route.setupSummaryController() = route(SUMMARY_URL) {
    val summaryService: SummaryService by inject()
    val redisService: RedisService by inject()

    get("year/{year}") {
        val year = call.parameters["year"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val sessionUserId = getSessionUserId(redisService)
        val summary = summaryService.summaryOfYear(userId = sessionUserId, year = year)
        call.respond(summary)
    }

    get("year/{year}/outliers") {
        val year = call.parameters["year"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val threshold = call.request.queryParameters["threshold"]
            ?.let { it.toDoubleOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST) }
            ?: DEFAULT_OUTLIER_THRESHOLD
        val minTotal = call.request.queryParameters["minTotal"]
            ?.let { it.toBigDecimalOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST) }
            ?: DEFAULT_OUTLIER_MIN_TOTAL
        val yearsAround = call.request.queryParameters["yearsAround"]
            ?.let { it.toIntOrNull()?.takeIf { years -> years >= 0 } ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST) }
            ?: DEFAULT_OUTLIER_YEARS_AROUND
        val sessionUserId = getSessionUserId(redisService)
        val outliers = summaryService.outliersOfYear(
            userId = sessionUserId,
            year = year,
            threshold = threshold,
            minTotal = minTotal,
            yearsAround = yearsAround,
        )
        call.respond(outliers)
    }

    get("year/{year}/month/{month}") {
        val year = call.parameters["year"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val month = call.parameters["month"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val sessionUserId = getSessionUserId(redisService)
        val summary = summaryService.summaryOfMonth(userId = sessionUserId, year = year, month = month)
        call.respond(summary)
    }

    get("/year/{year}/month/{month}/day/{day}") {
        val year = call.parameters["year"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val month = call.parameters["month"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val day = call.parameters["day"]?.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val sessionUserId = getSessionUserId(redisService)
        val summary = summaryService.summaryOfDay(userId = sessionUserId, year = year, month = month, day = day)
        call.respond(summary)
    }

}

