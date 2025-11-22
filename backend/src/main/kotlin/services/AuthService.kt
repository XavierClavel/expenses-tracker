package com.xavierclavel.services

import com.xavierclavel.dtos.UserOut
import com.xavierclavel.exceptions.UnauthorizedCause
import com.xavierclavel.exceptions.UnauthorizedException
import com.xavierclavel.models.User
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.utils.logger
import dtos.GoogleOauthDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

class AuthService: KoinComponent {
    val redisService by inject<RedisService>()
    val userService by inject<UserService>()

    val redirects = mutableMapOf<String, String>()
    val applicationHttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun loginOrSignupOAuth(oauthToken: String): UserOut {
        val data = applicationHttpClient.get("https://openidconnect.googleapis.com/v1/userinfo") {
            bearerAuth(oauthToken)
        }.bodyAsText()
        val response = try {
            Json.decodeFromString<GoogleOauthDto>(data)
        } catch (e: SerializationException) {
            logger.info { "Failed to parse the following data: $data" }
            throw UnauthorizedException(UnauthorizedCause.OAUTH_FAILED)
        }
        val user = userService.exportByGoogleId(response.sub)
        if (user != null) {
            return user
        }
        if (userService.existsByEmail(response.email)) {
            throw UnauthorizedException(UnauthorizedCause.OAUTH_NOT_SETUP)
        }
        return createGoogleOauthUser(response)
    }


    private fun createGoogleOauthUser(oauthDto: GoogleOauthDto): UserOut {
        val baseName = oauthDto.name?.trim() ?: UUID.randomUUID().toString()
        var name = baseName
        var index = 1
        while (userService.existsByUsername(name)) {
            name = "$baseName-$index"
            index++
        }
        val userCreated = User(
            username = name,
            googleId =oauthDto.sub,
        ).apply { save() }
        .toOutput()
        logger.info { "Account created through Google Oauth by ${userCreated.username}" }
        return userCreated
    }


}