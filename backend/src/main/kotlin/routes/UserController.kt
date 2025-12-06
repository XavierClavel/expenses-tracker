package com.xavierclavel.routes

import com.xavierclavel.dtos.UserIn
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.UserService
import com.xavierclavel.utils.USERS_URL
import com.xavierclavel.utils.getPaging
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import com.xavierclavel.utils.logger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupUserController() = route("/users") {
    val userService: UserService by inject()
    val redisService: RedisService by inject()

        /**
         * Retrieves all users registered in the system.
         *
         * @response 200 OK - Returns a list of User objects
         */
        get {
            val paging = getPaging()
            val users = userService.exportAll(paging)
            call.respond(users)
        }

        /**
         * Retrieves the user that matches the id.
         *
         * @response 200 OK - Returns a User object that matches the given id
         * @response 404 Not Found - If no user exists with the provided id
         */
        get("/{id}") {
            val id = getPathId()
            val user = userService.export(id)
            call.respond(user)
        }

        authenticate("bearer-auth", "auth-session") {
            delete {
                val sessionUserId = getSessionUserId(redisService)
                userService.deleteById(sessionUserId)
                call.respond(HttpStatusCode.OK)
            }
        }

    }

