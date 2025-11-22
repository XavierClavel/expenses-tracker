package com.xavierclavel.plugins

import com.xavierclavel.config.Configuration
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.UnauthorizedCause
import com.xavierclavel.exceptions.UnauthorizedException
import com.xavierclavel.services.AuthService
import com.xavierclavel.services.UserService
import com.xavierclavel.utils.UserSession
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic
import io.ktor.server.auth.oauth
import io.ktor.server.auth.session
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalLettuceCoroutinesApi::class)
fun Application.configureAuthentication() {
    val userService by inject<UserService>()
    val redisService by inject<RedisService>()
    val authService by inject<AuthService>()
    val configuration by inject<Configuration>()

    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.httpOnly = true
            cookie.path = "/"
            cookie.maxAgeInSeconds = 7 * 24 * 60 * 60
        }
    }

    install(Authentication) {
        basic("auth-basic") {
            realm = "Access to the '/' path"
            validate { credentials ->
                userService.checkCredentials(credentials.name, credentials.password)
                UserIdPrincipal(credentials.name)
            }


        }

        oauth("auth-oauth-google") {
            // Configure oauth authentication
            urlProvider = { configuration.oauth.provider.url }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://oauth2.googleapis.com/token",
                    requestMethod = HttpMethod.Post,
                    clientId = configuration.oauth.client.id,
                    clientSecret = configuration.oauth.client.secret,
                    defaultScopes = listOf("openid", "profile", "email"),
                    extraAuthParameters = listOf("access_type" to "offline"),
                    onStateCreated = { call, state ->
                        //saves new state with redirect url value
                        call.request.queryParameters["redirectUrl"]?.let {
                            authService.redirects[state] = it
                        }
                    }
                )
            }
            client = authService.applicationHttpClient
        }

        session<UserSession>("auth-session") {
            validate { session ->
                if (redisService.hasSession(session.sessionId)) {
                    session
                } else {
                    null
                }
            }
            challenge {
                throw UnauthorizedException(UnauthorizedCause.SESSION_NOT_FOUND)
            }
        }

        session<UserSession>("admin-session") {
            validate { session ->
                if (redisService.isUserAdmin(session.sessionId)) {
                    session
                } else {
                    null
                }
            }
            challenge {
                throw UnauthorizedException(UnauthorizedCause.SESSION_NOT_FOUND)
            }
        }
    }

}