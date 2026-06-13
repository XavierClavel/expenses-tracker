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
        var mail: String = ""
        runAsUser1 {
            val result = client.getMe()
            userId = result.id
            mail = result.mail
        }
        runAsAdmin {
            val result = client.getUser(userId)
            assertEquals(mail, result.mail)
        }
    }

    @Test
    fun `list users`() = runTest {
        val newUser = SignupDto(
            emailAddress = "user3@mail.com",
            password = "Passw0rd",
        )
        runAsAdmin {
            val result = client.listUsers()
            assertTrue {
                result.any { it.mail == "user1@mail.com" }
            }
            assertTrue {
                result.any { it.mail == "user2@mail.com" }
            }
            assertFalse {
                result.any { it.mail == "user3@mail.com" }
            }
        }
        client.signup(newUser)
        runAsAdmin {
            val result2 = client.listUsers()
            assertTrue {
                result2.any { it.mail == "user1@mail.com" }
            }
            assertTrue {
                result2.any { it.mail == "user2@mail.com" }
            }
            assertTrue {
                result2.any { it.mail == "user3@mail.com" }
            }
        }
    }

    @Test
    fun `delete user`() = runTest {
        var userId: Long = 0
        runAsUser1 {
            val result = client.getMe()
            userId = result.id
            client.assertUserExists(userId)
            client.deleteUser()
        }
        runAsAdmin {
            client.assertUserDoesNotExist(userId)
        }
    }

}