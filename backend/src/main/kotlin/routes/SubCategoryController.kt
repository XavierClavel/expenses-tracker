package com.xavierclavel.routes

import com.xavierclavel.dtos.SubcategoryIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.SubcategoryService
import com.xavierclavel.utils.SUBCATEGORY_URL
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupSubcategoryController() = route(SUBCATEGORY_URL) {
    val subcategoryService: SubcategoryService by inject()
    val redisService: RedisService by inject()

        /**
         * Retrieves the user that matches the id.
         *
         * @response 200 OK - Returns a User object that matches the given id
         * @response 404 Not Found - If no user exists with the provided id
         */
        get("/{id}") {
            val userId = getSessionUserId(redisService)
            val categoryId = getPathId()
            val user = subcategoryService.export(userId = userId, categoryId = categoryId)
            call.respond(user)
        }

        post {
            val userId = getSessionUserId(redisService)
            val categoryDto = call.receive<SubcategoryIn>()
            val category = subcategoryService.create(userId = userId, subcategoryDto = categoryDto)
            call.respond(category)
        }

        put("/{id}") {
            val categoryId = getPathId()
            val userId = getSessionUserId(redisService)
            val categoryDto = call.receive<SubcategoryIn>()
            val category = subcategoryService.update(userId = userId, subcategoryId = categoryId, subcategoryDto = categoryDto)
            call.respond(category)
        }

        delete("/{id}") {
            val categoryId = getPathId()
            val userId = getSessionUserId(redisService)
            subcategoryService.delete(userId = userId, subcategoryId = categoryId)
            call.respond(HttpStatusCode.OK)
        }

    }

