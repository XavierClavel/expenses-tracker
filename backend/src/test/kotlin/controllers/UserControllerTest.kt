package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.auth.SignupDto
import com.xavierclavel.utils.assertUserDoesNotExist
import com.xavierclavel.utils.assertUserExists
import com.xavierclavel.utils.deleteUser
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.getUser
import com.xavierclavel.utils.listUsers
import com.xavierclavel.utils.signup
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserControllerTest: ApplicationTest() {

    @Test
    fun `get user`() = runTest {
        var userId: Long = 0
        var username: String = ""
        runAsUser1 {
            val result = client.getMe()
            userId = result.id
            username = result.username
        }
        runAsUser2 {
            val result = client.getUser(userId)
            assertEquals(username, result.username)
        }
    }

    @Test
    fun `list users`() = runTest {
        val result = client.listUsers()
        val newUser = SignupDto(
            username = "user3",
            emailAddress = "user3@mail.com",
            password = "Passw0rd",
        )
        assertTrue {
            result.any { it.username == "user1" }
        }
        assertTrue {
            result.any { it.username == "user2" }
        }
        assertFalse {
            result.any { it.username == "user3" }
        }
        client.signup(newUser)
        val result2 = client.listUsers()
        assertTrue {
            result2.any { it.username == "user1" }
        }
        assertTrue {
            result2.any { it.username == "user2" }
        }
        assertTrue {
            result2.any { it.username == "user3" }
        }
    }

    @Test
    fun `delete user`() = runTest {
        var userId: Long = 0
        runAsUser1 {
            val result = client.getMe()
            userId = result.id
            client.assertUserExists(userId)
            client.deleteUser(userId)
        }
        runAsUser2 {
            client.assertUserDoesNotExist(userId)
        }
    }

}