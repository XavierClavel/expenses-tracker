package com.xavierclavel.services

import com.xavierclavel.dtos.ApiKeyCreatedOut
import com.xavierclavel.dtos.ApiKeyIn
import com.xavierclavel.dtos.ApiKeyOut
import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.ApiKey
import com.xavierclavel.models.query.QApiKey
import com.xavierclavel.models.query.QUser
import com.xavierclavel.utils.ApiKeyPrincipal
import org.koin.core.component.KoinComponent
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Base64
import java.util.HexFormat

/** Every key starts with this, so it is recognizable in configs and secret scanners. */
const val API_KEY_PREFIX = "bk_"

class ApiKeyService: KoinComponent {
    private val secureRandom = SecureRandom()

    private fun getById(id: Long): ApiKey =
        QApiKey().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.API_KEY_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify an API key he does not own
     */
    private fun ApiKey.checkRights(userId: Long): ApiKey {
        if (this.user.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_OWN_API_KEY)
        }
        return this
    }

    /** 256 random bits: a plain SHA-256 is enough to store it, no slow hash needed. */
    private fun generateKey(): String {
        val bytes = ByteArray(32).also { secureRandom.nextBytes(it) }
        return API_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(key: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.toByteArray()))

    fun list(userId: Long): List<ApiKeyOut> =
        QApiKey()
            .user.id.eq(userId)
            .orderBy().id.asc()
            .findList()
            .map { it.toOutput() }

    fun create(userId: Long, apiKeyDto: ApiKeyIn): ApiKeyCreatedOut {
        val name = apiKeyDto.name.trim()
        if (name.isEmpty()) throw BadRequestException(BadRequestCause.INVALID_REQUEST)
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val key = generateKey()
        val apiKey = ApiKey(
            user = user,
            name = name,
            hashedKey = hash(key),
            hint = "$API_KEY_PREFIX…${key.takeLast(4)}",
            createdAt = LocalDate.now(),
        )
        apiKey.insert()
        return ApiKeyCreatedOut(
            id = apiKey.id,
            name = apiKey.name,
            hint = apiKey.hint,
            createdAt = apiKey.createdAt,
            lastUsedAt = apiKey.lastUsedAt,
            key = key,
        )
    }

    fun delete(userId: Long, apiKeyId: Long) {
        val result = getById(apiKeyId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete API key $apiKeyId")
        }
    }

    /**
     * Resolve the key presented by a request. Returns null for an unknown or revoked key.
     * Records the day of use, writing at most once per key per day.
     */
    fun authenticate(key: String): ApiKeyPrincipal? {
        if (!key.startsWith(API_KEY_PREFIX)) return null
        val apiKey = QApiKey().hashedKey.eq(hash(key)).findOne() ?: return null
        val today = LocalDate.now()
        if (apiKey.lastUsedAt != today) {
            apiKey.lastUsedAt = today
            apiKey.update()
        }
        return ApiKeyPrincipal(userId = apiKey.user.id, apiKeyId = apiKey.id)
    }
}
