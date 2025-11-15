package com.xavierclavel.plugins

import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.exceptions.UnauthorizedException
import com.xavierclavel.utils.logger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.path
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.reason.key)
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.reason.key)
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, cause.reason.key)
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, cause.reason.key)
        }
        exception<Throwable> { call, cause ->
            logger.error { "Call to ${call.request.path()} failed with error ${cause.stackTraceToString()}" }
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
        }
    }
}
