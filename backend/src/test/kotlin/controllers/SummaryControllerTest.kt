package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.getSummary
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

class SummaryControllerTest: ApplicationTest() {
    @OptIn(ExperimentalTime::class)
    val expense = ExpenseIn(
        label = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = 0,
        type = ExpenseType.EXPENSE
    )

    val categoryInTemplate = CategoryIn(name = "Groceries", type = ExpenseType.EXPENSE, color = "", icon = "")


    @Test
    fun `get month summary`() = runTestAsUser {
            val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
            val activitiesId = client.createCategory(categoryInTemplate.copy(name = "Activities")).subcategories[0].id
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