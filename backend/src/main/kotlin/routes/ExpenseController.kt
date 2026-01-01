package com.xavierclavel.routes

import com.xavierclavel.dtos.DateDto
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.ExpenseService
import com.xavierclavel.utils.EXPENSES_URL
import com.xavierclavel.utils.getPaging
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.time.LocalDate

fun Route.setupExpenseController() = route(EXPENSES_URL) {
    val expenseService: ExpenseService by inject()
    val redisService: RedisService by inject()

        /**
         * Retrieves a paginated list of expenses from logged user.
         */
        get {
            val sessionUserId = getSessionUserId(redisService)
            val paging = getPaging()
            val categoryId = call.parameters["categoryId"]?.toLongOrNull()
            val subcategoryId = call.parameters["subcategoryId"]?.toLongOrNull()
            val expenseType = call.parameters["type"]?.let { ExpenseType.valueOf(it.uppercase()) }
            val from = call.request.queryParameters["from"]?.let { LocalDate.parse(it) }
            val to = call.request.queryParameters["to"]?.let { LocalDate.parse(it) }
            val users = expenseService.list(
                userId = sessionUserId,
                paging = paging,
                subcategoryId = subcategoryId,
                expenseType = expenseType,
                categoryId = categoryId,
                from = from,
                to = to,
            )
            call.respond(users)
        }

        /**
         * Retrieves a specific expense from its id.
         */
        get("/{id}") {
            val userId = getSessionUserId(redisService)
            val expenseId = getPathId()
            val user = expenseService.export(userId = userId, expenseId = expenseId)
            call.respond(user)
        }

    get("/oldest") {
        val userId = getSessionUserId(redisService)
        val oldestExpenseDate = expenseService.getOldestActivity(userId = userId)
        call.respond(DateDto(oldestExpenseDate))
    }

        post {
            val userId = getSessionUserId(redisService)
            val expenseDto = call.receive<ExpenseIn>()
            val category = expenseService.create(userId = userId, expenseDto = expenseDto)
            call.respond(category)
        }

        put("/{id}") {
            val expenseId = getPathId()
            val userId = getSessionUserId(redisService)
            val expenseDto = call.receive<ExpenseIn>()
            val category = expenseService.update(userId = userId, expenseId = expenseId, expenseDto = expenseDto)
            call.respond(category)
        }

        delete("/{id}") {
            val expenseId = getPathId()
            val userId = getSessionUserId(redisService)
            expenseService.delete(userId = userId, expenseId = expenseId)
            call.respond(HttpStatusCode.OK)
        }

    }

