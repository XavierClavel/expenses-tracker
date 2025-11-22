package com.xavierclavel.utils

import com.xavierclavel.dtos.UserOut
import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.UnauthorizedCause
import com.xavierclavel.exceptions.UnauthorizedException
import com.xavierclavel.plugins.RedisService
import io.ebean.Paging
import io.ktor.server.routing.RoutingContext
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import java.util.UUID

fun RoutingContext.getPaging(): Paging =
    Paging.of(
        call.request.queryParameters["page"]?.toIntOrNull() ?: 0,
        call.request.queryParameters["size"]?.toIntOrNull() ?: 20
    )

suspend fun RoutingContext.createSession(user: UserOut, redisService: RedisService) {
    val sessionId = UUID.randomUUID().toString()
    redisService.createSession(sessionId, user)
    call.sessions.set(UserSession(sessionId))
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun RoutingContext.getOptionalSessionId(redisService: RedisService): Long? {
    val session = call.sessions.get<UserSession>() ?: return null
    val userId = redisService.getSessionUserId(session.sessionId)
    if (userId == null) {
        call.sessions.clear<UserSession>()
    }
    return userId
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun RoutingContext.getSessionUserId(redisService: RedisService): Long {
    val session =
        call.sessions.get<UserSession>() ?: throw UnauthorizedException(UnauthorizedCause.SESSION_NOT_FOUND)
    val userId = redisService.getSessionUserId(session.sessionId)
    if (userId == null) {
        call.sessions.clear<UserSession>()
        throw UnauthorizedException(UnauthorizedCause.SESSION_NOT_FOUND)
    }
    return userId
}

fun RoutingContext.getPathId(): Long = call.parameters["id"]?.toLongOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST)
