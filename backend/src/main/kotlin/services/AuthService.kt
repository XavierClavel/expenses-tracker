package com.xavierclavel.services

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.xavierclavel.config.Configuration
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
    val configuration by inject<Configuration>()

    val redirects = mutableMapOf<String, String>()
    val applicationHttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    // Verifies ID tokens minted for the native app. The audience must match our
    // Google "Web application" client id (the serverClientId used on Android).
    private val googleIdTokenVerifier: GoogleIdTokenVerifier by lazy {
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(listOf(configuration.oauth.client.id))
            .build()
    }

    /** Web redirect flow: reads user info from a Google OAuth access token. */
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
        return findOrCreateGoogleUser(response.sub, response.email, response.name)
    }

    /** Native flow: verifies a Google ID token (JWT) issued to the Android app. */
    fun loginOrSignupGoogleIdToken(idTokenString: String): UserOut {
        val idToken = try {
            googleIdTokenVerifier.verify(idTokenString)
        } catch (e: Exception) {
            logger.info { "Failed to verify Google ID token: ${e.message}" }
            throw UnauthorizedException(UnauthorizedCause.OAUTH_FAILED)
        } ?: throw UnauthorizedException(UnauthorizedCause.OAUTH_FAILED)
        val payload = idToken.payload
        val email = payload.email ?: throw UnauthorizedException(UnauthorizedCause.OAUTH_FAILED)
        return findOrCreateGoogleUser(payload.subject, email, payload["name"] as String?)
    }

    private fun findOrCreateGoogleUser(sub: String, email: String, name: String?): UserOut {
        userService.exportByGoogleId(sub)?.let { return it }
        if (userService.existsByEmail(email)) {
            throw UnauthorizedException(UnauthorizedCause.OAUTH_NOT_SETUP)
        }
        return createGoogleOauthUser(sub, name)
    }

    private fun createGoogleOauthUser(sub: String, name: String?): UserOut {
        val baseName = name?.trim() ?: UUID.randomUUID().toString()
        var username = baseName
        var index = 1
        while (userService.existsByUsername(username)) {
            username = "$baseName-$index"
            index++
        }
        val userCreated = User(
            username = username,
            googleId = sub,
        ).apply { save() }
        .toOutput()
        logger.info { "Account created through Google Oauth by ${userCreated.mail}" }
        return userCreated
    }
}
