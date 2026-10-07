package com.xavierclavel.routes

import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.CategoryService
import com.xavierclavel.utils.CATEGORY_URL
import com.xavierclavel.utils.getPaging
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import com.xavierclavel.utils.getUserIdAllowingApiKey
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupCategoryController() = route(CATEGORY_URL) {
    val categoryService: CategoryService by inject()
    val redisService: RedisService by inject()

        /**
         * Retrieves every category of the logged user with its subcategories. Also
         * reachable with a personal API key, to look up subcategory ids.
         */
        get {
            val sessionUserId = getUserIdAllowingApiKey(redisService)
            val users = categoryService.list(userId = sessionUserId)
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

