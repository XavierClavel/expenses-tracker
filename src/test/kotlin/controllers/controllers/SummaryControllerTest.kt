package com.xavierclavel.controllers.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.utils.EXPENSES_URL
import com.xavierclavel.utils.assertExpenseDoesNotExist
import com.xavierclavel.utils.assertExpenseExists
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.deleteExpense
import com.xavierclavel.utils.getExpense
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.getSummary
import com.xavierclavel.utils.listExpensesByUser
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SummaryControllerTest: ApplicationTest() {
    @OptIn(ExperimentalTime::class)
    val expense = ExpenseIn(
        label = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
    )

    @Test
    fun `get month summary`() = runTestAsUser {
            val groceriesId = client.createCategory(CategoryIn(name = "Groceries")).id
            val activitiesId = client.createCategory(CategoryIn(name = "Activities")).id
            client.createExpense(expense.copy(
                amount = BigDecimal("25.00"),
                date = LocalDate.parse("2020-06-06"),
                categoryId = groceriesId,
            ))
            client.createExpense(expense.copy(
                amount = BigDecimal("15.00"),
                date = LocalDate.parse("2020-06-30"),
                categoryId = groceriesId,
            ))
            client.createExpense(expense.copy(
                amount = BigDecimal("10.00"),
                date = LocalDate.parse("2020-06-01"),
                categoryId = activitiesId,
            ))
            client.createExpense(expense.copy(
                amount = BigDecimal("10.00"),
                date = LocalDate.parse("2020-07-01"),
                categoryId = activitiesId,
            ))
        val userId = client.getMe().id
        val result = client.getSummary(id = userId, year = 2020, month = 6)
        assertEquals(BigDecimal("50.00"), result.totalExpenses)

        assertEquals(2, result.byCategory.size)
    }

}