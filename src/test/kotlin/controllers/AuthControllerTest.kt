package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.SignupDto
import com.xavierclavel.exceptions.UnauthorizedCause
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.logger
import com.xavierclavel.utils.login
import com.xavierclavel.utils.signup
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuthControllerTest: ApplicationTest() {

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
            username = "test",
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
            username = "test",
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
            assertEquals("user1", result.username)
        }
        runAsUser2 {
            val result = client.getMe()
            assertEquals("user2", result.username)
        }
    }

}