package com.xavierclavel.routes

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.auth.SignupDto
import com.xavierclavel.exceptions.BadRequestCause
import com.xavierclavel.exceptions.BadRequestException
import com.xavierclavel.exceptions.UnauthorizedCause
import com.xavierclavel.exceptions.UnauthorizedException
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.services.AuthService
import com.xavierclavel.services.UserService
import com.xavierclavel.utils.AUTH_URL
import com.xavierclavel.utils.UserSession
import com.xavierclavel.utils.createSession
import com.xavierclavel.utils.getSessionUserId
import com.xavierclavel.utils.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.koin.ktor.ext.inject

fun Route.setupAuthController() = route(AUTH_URL) {
    val userService: UserService by inject()
    val authService: AuthService by inject()
    val redisService: RedisService by inject()
    val configuration: Configuration by inject()

    authenticate("auth-basic") {

        @OptIn(ExperimentalLettuceCoroutinesApi::class)
        post("/login") {
            val mail = call.principal<UserIdPrincipal>()?.name.toString()
            val user = userService.exportByMail(mail) ?: throw UnauthorizedException(UnauthorizedCause.INVALID_CREDENTIALS)
            createSession(user, redisService)
            call.respond(HttpStatusCode.OK)
        }

    }

    post("/logout") {
        call.sessions.clear<UserSession>()
        call.respond(HttpStatusCode.OK)
    }

    authenticate("auth-oauth-google") {
        get("/login-oauth-google") {

        }

        get("/callback-oauth-google") {
            val currentPrincipal: OAuthAccessTokenResponse.OAuth2 = call.principal() ?: return@get call.respondRedirect(configuration.oauth.redirect.url)
            currentPrincipal.state ?: return@get call.respondRedirect(configuration.oauth.redirect.url)
            val user = authService.loginOrSignupOAuth(currentPrincipal.accessToken)
            createSession(user, redisService)
            val redirect = authService.redirects[currentPrincipal.state] ?: return@get call.respondRedirect(configuration.oauth.redirect.url)
            return@get call.respondRedirect(redirect)

        }
    }

    authenticate("auth-session") {
        get("/me") {
            val id = getSessionUserId(redisService)
            val userInfo = userService.export(id)
            call.respond(userInfo)
        }
    }

    post("/signup") {
        val userDTO = call.receive<SignupDto>()
        if (userService.existsByUsername(userDTO.username)) throw BadRequestException(BadRequestCause.USERNAME_ALREADY_USED)
        if (userService.existsByEmail(userDTO.emailAddress)) throw BadRequestException(BadRequestCause.MAIL_ALREADY_USED)
        val userCreated = userService.create(userDTO)
        logger.info { "Account created through basic auth by ${userCreated.username}" }
        call.respond(HttpStatusCode.Created, userCreated)
    }




}