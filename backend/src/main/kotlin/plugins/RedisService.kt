package com.xavierclavel.plugins

import com.xavierclavel.dtos.UserOut
import com.xavierclavel.enums.UserRole
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.GetExArgs
import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent

/**
 * How long a session survives without being used. Sessions are rolling: every
 * authenticated request restarts this window, so only inactivity ends a session.
 */
const val SESSION_TTL_SECONDS: Long = 7 * 24 * 60 * 60

@Serializable
data class SessionData(
    val userId: Long,
    val role: UserRole,
) {
    companion object {
        fun from(user: UserOut) = SessionData(user.id, user.role)
    }
}

class RedisService(redisUrl: String): KoinComponent {
    private val client = RedisClient.create(redisUrl)
    private val connection = client.connect()

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    val redis: RedisCoroutinesCommands<String, String> = connection.coroutines()

    internal fun sessionKey(sessionId: String) = "session:$sessionId"

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun createSession(sessionId: String, user: UserOut) {
        val json = SessionData.from(user)
        redis.setex(sessionKey(sessionId), SESSION_TTL_SECONDS, Json.encodeToString(json))
    }

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun deleteSession(sessionId: String) {
        redis.del(sessionKey(sessionId))
    }



    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun hasSession(sessionId: String): Boolean =
        getSessionUserId(sessionId) != null

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun getSessionUserId(sessionId: String): Long? =
        getSession(sessionId)?.userId

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun getSession(sessionId: String): SessionData? {
        val json = redis.get(sessionKey(sessionId)) ?: return null
        return Json.decodeFromString<SessionData>(json)
    }

    /**
     * Reads a session and restarts its idle window in a single round trip
     * (GETEX). Returns null when the session is unknown or has expired, in
     * which case nothing is written back.
     */
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun rollSession(sessionId: String): SessionData? {
        val json = redis.getex(sessionKey(sessionId), GetExArgs.Builder.ex(SESSION_TTL_SECONDS)) ?: return null
        return Json.decodeFromString<SessionData>(json)
    }

    /** Seconds left before the session expires, or null if it is already gone. */
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    suspend fun getSessionTtl(sessionId: String): Long? =
        redis.ttl(sessionKey(sessionId))?.takeIf { it >= 0 }

    suspend fun isUserAdmin(sessionId: String): Boolean =
        getSession(sessionId)?.role == UserRole.ADMIN


}
