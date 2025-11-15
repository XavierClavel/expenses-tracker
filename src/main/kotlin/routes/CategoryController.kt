package com.xavierclavel.routes

import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.CategoryService
import com.xavierclavel.utils.CATEGORY_URL
import com.xavierclavel.utils.getPaging
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupCategoryController() = route(CATEGORY_URL) {
    val categoryService: CategoryService by inject()
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
            val users = categoryService.list(userId = id, sessionUserId = sessionUserId, paging = paging)
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
            val categoryId = getPathId()
            val user = categoryService.export(userId = userId, categoryId = categoryId)
            call.respond(user)
        }

        post {
            val userId = getSessionUserId(redisService)
            val categoryDto = call.receive<CategoryIn>()
            val category = categoryService.create(userId = userId, categoryDto = categoryDto)
            call.respond(category)
        }

        put("/{id}") {
            val categoryId = getPathId()
            val userId = getSessionUserId(redisService)
            val categoryDto = call.receive<CategoryIn>()
            val category = categoryService.update(userId = userId, categoryId = categoryId, categoryDto = categoryDto)
            call.respond(category)
        }

        delete("/{id}") {
            val categoryId = getPathId()
            val userId = getSessionUserId(redisService)
            categoryService.delete(userId = userId, categoryId = categoryId)
            call.respond(HttpStatusCode.OK)
        }

    }

