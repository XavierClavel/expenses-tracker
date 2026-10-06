package com.xavierclavel.routes

import com.xavierclavel.dtos.IdListIn
import com.xavierclavel.dtos.RecurringExpenseIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.RecurringExpenseService
import com.xavierclavel.utils.RECURRING_EXPENSES_URL
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupRecurringExpenseController() = route(RECURRING_EXPENSES_URL) {
    val recurringExpenseService: RecurringExpenseService by inject()
    val redisService: RedisService by inject()

    /**
     * Retrieves every recurring expense of the logged user, ordered by day of month.
     */
    get {
        val userId = getSessionUserId(redisService)
        call.respond(recurringExpenseService.list(userId = userId))
    }

    /**
     * Retrieves a specific recurring expense from its id.
     */
    get("/{id}") {
        val userId = getSessionUserId(redisService)
        val recurringExpenseId = getPathId()
        call.respond(recurringExpenseService.export(userId = userId, recurringExpenseId = recurringExpenseId))
    }

    /**
     * Creates a recurring expense. If its day of month is today, this month's expense is created right away.
     */
    post {
        val userId = getSessionUserId(redisService)
        val dto = call.receive<RecurringExpenseIn>()
        call.respond(recurringExpenseService.create(userId = userId, dto = dto))
    }

    /**
     * Creates a recurring expense from each of several expenses, repeating on its day of month.
     * Each expense counts as its month's occurrence, so it is never duplicated.
     */
    post("/from-expenses") {
        val userId = getSessionUserId(redisService)
        val dto = call.receive<IdListIn>()
        call.respond(recurringExpenseService.createFromExpenses(userId = userId, ids = dto.ids))
    }

    put("/{id}") {
        val userId = getSessionUserId(redisService)
        val recurringExpenseId = getPathId()
        val dto = call.receive<RecurringExpenseIn>()
        call.respond(
            recurringExpenseService.update(userId = userId, recurringExpenseId = recurringExpenseId, dto = dto)
        )
    }

    /**
     * Deletes a recurring expense. The expenses it already created are kept.
     */
    delete("/{id}") {
        val userId = getSessionUserId(redisService)
        val recurringExpenseId = getPathId()
        recurringExpenseService.delete(userId = userId, recurringExpenseId = recurringExpenseId)
        call.respond(HttpStatusCode.OK)
    }
}
