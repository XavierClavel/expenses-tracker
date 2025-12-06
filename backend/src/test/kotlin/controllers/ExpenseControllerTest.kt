package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.utils.EXPENSES_URL
import com.xavierclavel.utils.assertExpenseDoesNotExist
import com.xavierclavel.utils.assertExpenseExists
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.deleteExpense
import com.xavierclavel.utils.getExpense
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.listExpenses
import com.xavierclavel.utils.updateExpense
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

class ExpenseControllerTest: ApplicationTest() {
    @OptIn(ExperimentalTime::class)
    val expense = ExpenseIn(
        label = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
    )

    @Test
    fun `get expense`() = runTestAsUser {
        val result = client.createExpense(expense)
        assertEquals(expense.label, result.label)
    }

    @Test
    fun `create expense`() = runTestAsUser {
        val result = client.listExpenses()
        assertEquals(0, result.size)

        client.createExpense(expense)
        val result2 = client.listExpenses()
        assertEquals(1, result2.size)
    }

    @Test
    fun `edit expense`() = runTestAsUser {
        val expense = client.createExpense(this@ExpenseControllerTest.expense)
        val result = client.getExpense(expense.id)
        assertEquals("Carrefour", result.label)

        client.updateExpense(result.id, this@ExpenseControllerTest.expense.copy(label = "Monoprix"))
        val result2 = client.getExpense(expense.id)
        assertEquals("Monoprix", result2.label)
    }

    @Test
    fun `list expenses`() = runTest{
        runAsUser1 {
            client.createExpense(expense)
        }
        runAsUser2 {
            client.createExpense(expense.copy(label = "MacDo"))
            client.createExpense(expense.copy(label = "Coffee"))
        }

        runAsUser1 {
            val result = client.listExpenses()
            assertEquals(1, result.size)
            assertEquals("Carrefour", result[0].label)
        }

        runAsUser2 {
            val result = client.listExpenses()
            assertEquals(2, result.size)
            assertTrue {
                result.any { it.label == "MacDo" }
            }
            assertTrue {
                result.any { it.label == "Coffee" }
            }
        }
    }

    @Test
    fun `delete expense`() = runTestAsUser {
        val expense = this@ExpenseControllerTest.expense
        val result = client.createExpense(expense)
        client.assertExpenseExists(result.id)
        client.deleteExpense(result.id)
        client.assertExpenseDoesNotExist(result.id)
    }

    @Test
    fun `user cannot edit expense they do not own`() = runTest {
        var expenseId: Long = 0
        runAsUser1 {
            expenseId = client.createExpense(expense).id
        }

        runAsUser2 {
            client.put("${EXPENSES_URL}/$expenseId"){
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(expense)
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }

    @Test
    fun `user cannot delete expense they do not own`() = runTest {
        var expenseId: Long = 0
        runAsUser1 {
            expenseId = client.createExpense(expense).id
        }

        runAsUser2 {
            client.delete("${EXPENSES_URL}/$expenseId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }


    @Test
    fun `users cannot see each others categories`() = runTest {
        var userId: Long = 0
        runAsUser1 {
            userId = client.getMe().id
        }

        runAsUser2 {
            client.get("${EXPENSES_URL}/user/$userId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }

}