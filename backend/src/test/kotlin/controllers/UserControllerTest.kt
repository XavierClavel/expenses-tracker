package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.TestBuilderWrapper
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.SubcategoryIn
import com.xavierclavel.dtos.auth.SignupDto
import com.xavierclavel.dtos.investment.AccountReportIn
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.models.query.QAccountReport
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QInvestment
import com.xavierclavel.models.query.QInvestmentAccount
import com.xavierclavel.models.query.QMonthCommentary
import com.xavierclavel.models.query.QSubcategory
import com.xavierclavel.models.query.QUser
import com.xavierclavel.utils.assertUserDoesNotExist
import com.xavierclavel.utils.assertUserExists
import com.xavierclavel.utils.createAccount
import com.xavierclavel.utils.createAccountReport
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.createSubcategory
import com.xavierclavel.utils.deleteUser
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.getUser
import com.xavierclavel.utils.listAccounts
import com.xavierclavel.utils.listCategoriesByUser
import com.xavierclavel.utils.listExpenses
import com.xavierclavel.utils.listUsers
import com.xavierclavel.utils.signup
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
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

    private val categoryTemplate =
        CategoryIn(name = "Groceries", type = ExpenseType.EXPENSE, color = "", icon = "")

    private val subcategoryTemplate =
        SubcategoryIn(name = "Supermarket", type = ExpenseType.EXPENSE, icon = "", parentCategory = -1)

    private val expenseTemplate = ExpenseIn(
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
        type = ExpenseType.EXPENSE,
    )

    // Creates one of every user-owned entity (category, subcategory, expense,
    // investment account, account report) so deletion is exercised end to end.
    private suspend fun TestBuilderWrapper.seedFullDataset() {
        val category = client.createCategory(categoryTemplate)
        val subcategory = client.createSubcategory(subcategoryTemplate.copy(parentCategory = category.id))
        client.createExpense(expenseTemplate.copy(categoryId = subcategory.id))
        val account = client.createAccount(InvestmentAccountIn(name = "Savings"))
        client.createAccountReport(
            account.id,
            AccountReportIn(amount = BigDecimal("1000.00"), date = LocalDate.of(2026, 1, 1)),
        )
    }

    // Deleting an account must wipe every piece of data the user owns, not just
    // the user row — otherwise orphaned rows violate foreign keys or leak data.
    @Test
    fun `delete user removes all owned data`() = runTest {
        var userId: Long = 0
        runAsUser1 {
            userId = client.getMe().id
            seedFullDataset()
            // sanity: the data really is there before we delete it
            assertTrue(QExpense().user.id.eq(userId).findCount() > 0)
            assertTrue(QCategory().user.id.eq(userId).findCount() > 0)
            assertTrue(QSubcategory().user.id.eq(userId).findCount() > 0)
            assertTrue(QInvestmentAccount().owner.id.eq(userId).findCount() > 0)
            assertTrue(QAccountReport().account.owner.id.eq(userId).findCount() > 0)
            client.deleteUser()
        }
        assertFalse(QUser().id.eq(userId).exists())
        assertEquals(0, QExpense().user.id.eq(userId).findCount())
        assertEquals(0, QCategory().user.id.eq(userId).findCount())
        assertEquals(0, QSubcategory().user.id.eq(userId).findCount())
        assertEquals(0, QInvestmentAccount().owner.id.eq(userId).findCount())
        assertEquals(0, QAccountReport().account.owner.id.eq(userId).findCount())
        assertEquals(0, QInvestment().user.id.eq(userId).findCount())
        assertEquals(0, QMonthCommentary().user.id.eq(userId).findCount())
    }

    // One user deleting their account must never touch another user's data.
    @Test
    fun `delete user leaves other users data intact`() = runTest {
        var user2Id: Long = 0
        runAsUser2 {
            user2Id = client.getMe().id
            seedFullDataset()
        }
        runAsUser1 {
            seedFullDataset()
            client.deleteUser()
        }
        runAsUser2 {
            assertTrue(client.listExpenses().isNotEmpty())
            assertTrue(client.listCategoriesByUser().isNotEmpty())
            assertTrue(client.listAccounts().isNotEmpty())
        }
        assertTrue(QExpense().user.id.eq(user2Id).findCount() > 0)
        assertTrue(QInvestmentAccount().owner.id.eq(user2Id).findCount() > 0)
        assertTrue(QAccountReport().account.owner.id.eq(user2Id).findCount() > 0)
    }

}