package com.xavierclavel.routes

import com.xavierclavel.dtos.ApiKeyIn
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.ApiKeyService
import com.xavierclavel.utils.API_KEY_URL
import com.xavierclavel.utils.getPathId
import com.xavierclavel.utils.getSessionUserId
import io.ktor.http.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Management of the personal API keys external apps use to create expenses. Session only:
 * an API key can never list, create or revoke keys.
 */
fun Route.setupApiKeyController() = route(API_KEY_URL) {
    val apiKeyService: ApiKeyService by inject()
    val redisService: RedisService by inject()

    /**
     * Retrieves every API key of the logged user, without the keys themselves.
     */
    get {
        val userId = getSessionUserId(redisService)
        call.respond(apiKeyService.list(userId = userId))
    }

    /**
     * Creates an API key. The response is the only time the full key is returned.
     */
    post {
        val userId = getSessionUserId(redisService)
        val apiKeyDto = call.receive<ApiKeyIn>()
        call.respond(apiKeyService.create(userId = userId, apiKeyDto = apiKeyDto))
    }

    /**
     * Revokes an API key immediately.
     */
    delete("/{id}") {
        val userId = getSessionUserId(redisService)
        val apiKeyId = getPathId()
        apiKeyService.delete(userId = userId, apiKeyId = apiKeyId)
        call.respond(HttpStatusCode.OK)
    }
}
