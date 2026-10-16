package com.xavierclavel.routes

import com.xavierclavel.dtos.sync.SyncChanges
import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.SyncService
import com.xavierclavel.utils.SYNC_URL
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupSyncController() = route(SYNC_URL) {
    val syncService: SyncService by inject()
    val redisService: RedisService by inject()

    /**
     * Retrieves the changes made to the logged user's categories, subcategories, tags and
     * expenses after revision `since` (0 or omitted for everything), at most `limit` of them.
     * Pass the returned revision as `since` next time, right away while `hasMore` is true.
     */
    get("/pull") {
        val userId = getSessionUserId(redisService)
        val since = call.request.queryParameters["since"]
            ?.let { it.toLongOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST) }
            ?: 0
        val limit = call.request.queryParameters["limit"]
            ?.let { it.toIntOrNull() ?: throw BadRequestException(BadRequestCause.INVALID_REQUEST) }
            ?: SyncService.MAX_PULL_SIZE
        call.respond(syncService.pull(userId = userId, since = since, limit = limit))
    }

    /**
     * Applies the changes made on a device. Pull afterwards: rows the server kept in another
     * state than the pushed one come back with their server version.
     */
    post("/push") {
        val userId = getSessionUserId(redisService)
        val changes = call.receive<SyncChanges>()
        syncService.push(userId = userId, changes = changes)
        call.respond(HttpStatusCode.OK)
    }
}
