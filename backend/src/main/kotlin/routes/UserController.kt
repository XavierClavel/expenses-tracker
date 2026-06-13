package com.xavierclavel.routes

import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.UserService
import com.xavierclavel.utils.getPaging
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionId
import com.xavierclavel.utils.getSessionUserId
import com.xavierclavel.utils.requireAdmin
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupUserController() = route("/users") {
    val userService: UserService by inject()
    val redisService: RedisService by inject()

        authenticate("bearer-auth", "auth-session") {
            /**
             * Retrieves all users registered in the system. Admin only.
             *
             * @response 200 OK - Returns a list of User objects
             */
            get {
                requireAdmin(redisService)
                val paging = getPaging()
                val users = userService.exportAll(paging)
                call.respond(users)
            }

            /**
             * Retrieves the user that matches the id. Only the user themselves or an admin may access it.
             *
             * @response 200 OK - Returns a User object that matches the given id
             * @response 403 Forbidden - If the caller is neither the target user nor an admin
             * @response 404 Not Found - If no user exists with the provided id
             */
            get("/{id}") {
                val id = getPathId()
                val sessionUserId = getSessionUserId(redisService)
                if (id != sessionUserId && !redisService.isUserAdmin(getSessionId())) {
                    throw ForbiddenException(ForbiddenCause.MUST_BE_PERFORMED_ON_SELF)
                }
                val user = userService.export(id)
                call.respond(user)
            }

            delete {
                val sessionUserId = getSessionUserId(redisService)
                userService.deleteById(sessionUserId)
                call.respond(HttpStatusCode.OK)
            }
        }

    }

