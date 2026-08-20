package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.auth.SignupDto
import com.xavierclavel.exceptions.UnauthorizedCause
import com.xavierclavel.plugins.RedisService
import com.xavierclavel.plugins.SESSION_TTL_SECONDS
import com.xavierclavel.utils.AUTH_URL
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.logger
import com.xavierclavel.utils.login
import com.xavierclavel.utils.sessionToken
import com.xavierclavel.utils.signup
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import org.junit.jupiter.api.Test
import org.koin.test.inject
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class AuthControllerTest: ApplicationTest() {
    private val redisService by inject<RedisService>()

    @Test
    fun `logging in to unexisting account fails`() = runTest {
        client.login("test", "Passw0rd").apply {
            assertEquals(HttpStatusCode.Companion.Unauthorized, status)
            assertEquals(UnauthorizedCause.INVALID_CREDENTIALS.key, bodyAsText())
        }
    }

    @Test
    fun `logging in to existing account with wrong password fails`() = runTest {
        val user3 = SignupDto(
            password = "Passw0rd",
            emailAddress = "mail"
        )
        client.signup(user3)

        client.login(user3.emailAddress, "wrong_password").apply {
            assertEquals(HttpStatusCode.Companion.Unauthorized, status)
            assertEquals(UnauthorizedCause.INVALID_CREDENTIALS.key, bodyAsText())
        }
    }


    @Test
    fun `account can be logged in after signup`() = runTest {
        val user3 = SignupDto(
            password = "Passw0rd",
            emailAddress = "mail"
        )
        client.signup(user3)
        logger.info {"notice me :'("}

        client.login(user3.emailAddress, user3.password).apply {
            val a = bodyAsText()
            println(a)
            assertEquals(HttpStatusCode.Companion.OK, status)
        }
    }

    @Test
    fun `get current user`() = runTest {
        runAsUser1 {
            val result = client.getMe()
            assertEquals("user1@mail.com", result.mail)
        }
        runAsUser2 {
            val result = client.getMe()
            assertEquals("user2@mail.com", result.mail)
        }
    }

    @Test
    fun `session idle window restarts on every authenticated request`() = runTest {
        val token = client.login(user1.emailAddress, user1.password).sessionToken()
        assertFullIdleWindow(token)

        // Bring the session close to expiry, as if the user had been away for a week.
        shortenSession(token, 60)
        assertEquals(60L, redisService.getSessionTtl(token))

        client.getMe()
        assertFullIdleWindow(token)
    }

    @Test
    fun `session idle window restarts on bearer authenticated requests`() = runTest {
        // Native clients hold the session id as a bearer token. Only the authentication
        // phase is exercised here: the /me handler itself resolves the user from the
        // cookie, so a bearer-only request never reaches a 200.
        val user = userService.exportByMail(user1.emailAddress)!!
        val token = UUID.randomUUID().toString()
        redisService.createSession(token, user)
        shortenSession(token, 60)

        client.get("$AUTH_URL/me") { bearerAuth(token) }

        assertFullIdleWindow(token)
    }

    @Test
    fun `authenticated response reissues the session cookie`() = runTest {
        val token = client.login(user1.emailAddress, user1.password).sessionToken()

        client.get("$AUTH_URL/me").apply {
            assertEquals(HttpStatusCode.OK, status)
            val cookie = assertNotNull(
                headers.getAll(HttpHeaders.SetCookie)?.firstOrNull { it.startsWith("user_session=") },
                "the session cookie should be re-issued so its Max-Age tracks the server-side expiry"
            )
            assertTrue(cookie.contains(token), "cookie should still carry the same session id: $cookie")
            assertTrue(cookie.contains("Max-Age=$SESSION_TTL_SECONDS"), "unexpected cookie: $cookie")
        }
    }

    @Test
    fun `session that outlived its idle window is rejected`() = runTest {
        val token = client.login(user1.emailAddress, user1.password).sessionToken()
        redisService.deleteSession(token)

        client.get("$AUTH_URL/me").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
            assertEquals(UnauthorizedCause.SESSION_NOT_FOUND.key, bodyAsText())
        }
        // Rolling must not resurrect an expired session.
        assertNull(redisService.getSessionTtl(token))
    }

    private suspend fun assertFullIdleWindow(token: String) {
        val ttl = redisService.getSessionTtl(token)
        assertTrue(
            ttl != null && ttl > SESSION_TTL_SECONDS - 10,
            "expected a full idle window of $SESSION_TTL_SECONDS seconds, got $ttl"
        )
    }

    private suspend fun shortenSession(token: String, seconds: Long) {
        redisService.redis.expire(redisService.sessionKey(token), seconds)
    }

}