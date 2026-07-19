package com.xavierclavel.routes

import com.xavierclavel.dtos.TagIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.TagService
import com.xavierclavel.utils.TAG_URL
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.setupTagController() = route(TAG_URL) {
    val tagService: TagService by inject()
    val redisService: RedisService by inject()

    /**
     * Retrieves every tag of the logged user, each with its aggregated total.
     */
    get {
        val userId = getSessionUserId(redisService)
        call.respond(tagService.list(userId = userId))
    }

    /**
     * Retrieves a specific tag from its id.
     */
    get("/{id}") {
        val userId = getSessionUserId(redisService)
        val tagId = getPathId()
        call.respond(tagService.export(userId = userId, tagId = tagId))
    }

    post {
        val userId = getSessionUserId(redisService)
        val tagDto = call.receive<TagIn>()
        call.respond(tagService.create(userId = userId, tagDto = tagDto))
    }

    put("/{id}") {
        val userId = getSessionUserId(redisService)
        val tagId = getPathId()
        val tagDto = call.receive<TagIn>()
        call.respond(tagService.update(userId = userId, tagId = tagId, tagDto = tagDto))
    }

    delete("/{id}") {
        val userId = getSessionUserId(redisService)
        val tagId = getPathId()
        tagService.delete(userId = userId, tagId = tagId)
        call.respond(HttpStatusCode.OK)
    }
}
