package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.EXPENSES_URL
import com.xavierclavel.utils.assertExpenseDoesNotExist
import com.xavierclavel.utils.assertExpenseExists
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.deleteExpense
import com.xavierclavel.utils.getExpense
import com.xavierclavel.utils.listExpenses
import com.xavierclavel.utils.updateExpense
import io.ktor.client.request.delete
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
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
        type = ExpenseType.EXPENSE,
    )

    @Test
    fun `get expense`() = runTestAsUser {
        val result = client.createExpense(expense)
        assertEquals(expense.title, result.title)
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
        assertEquals("Carrefour", result.title)

        client.updateExpense(result.id, this@ExpenseControllerTest.expense.copy(title = "Monoprix"))
        val result2 = client.getExpense(expense.id)
        assertEquals("Monoprix", result2.title)
    }

    @Test
    fun `expenses are sorted by date`() = runTestAsUser{
        client.createExpense(expense.copy(title = "MacDo", date = LocalDate.parse("2021-06-06")))
        client.createExpense(expense.copy(title = "Coffee", date = LocalDate.parse("2019-06-06")))
        client.createExpense(expense.copy(title = "Groceries", date = LocalDate.parse("2020-06-06")))
        val result = client.listExpenses()
        assertEquals(3, result.size)
        assertTrue {
            result.any { it.title == "MacDo"}
        }
        assertTrue {
            result.any { it.title == "Groceries"}
        }
        assertTrue {
            result.any { it.title == "Coffee"}
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

}