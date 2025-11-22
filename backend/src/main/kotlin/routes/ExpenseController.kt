package com.xavierclavel.routes

import com.xavierclavel.dtos.ExpenseIn
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

fun Route.setupExpenseController() = route(EXPENSES_URL) {
    val expenseService: ExpenseService by inject()
    val redisService: RedisService by inject()

        /**
         * Retrieves all users registered in the system.
         *
         * @response 200 OK - Returns a list of User objects
         */
        get("/user/{id}") {
            val id = getPathId()
            val sessionUserId = getSessionUserId(redisService)
            val paging = getPaging()
            val users = expenseService.list(userId = id, sessionUserId = sessionUserId, paging = paging)
            call.respond(users)
        }

        /**
         * Retrieves the user that matches the id.
         *
         * @response 200 OK - Returns a User object that matches the given id
         * @response 404 Not Found - If no user exists with the provided id
         */
        get("/{id}") {
            val userId = getSessionUserId(redisService)
            val expenseId = getPathId()
            val user = expenseService.export(userId = userId, expenseId = expenseId)
            call.respond(user)
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

