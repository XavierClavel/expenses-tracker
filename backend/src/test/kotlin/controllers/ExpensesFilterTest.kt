package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.listExpenses
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

class ExpensesFilterTest: ApplicationTest() {

    @OptIn(ExperimentalTime::class)
    val expense = ExpenseIn(
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
        type = ExpenseType.EXPENSE,
    )

    val categoryInTemplate = CategoryIn(
        name = "Groceries",
        type = ExpenseType.EXPENSE,
        color = "",
        icon = "",
    )


    @Test
    fun `expenses are segregated by user`() = runTestAsUser{

        runAsUser1 {
            client.createExpense(expense)
        }
        runAsUser2 {
            client.createExpense(expense.copy(title = "MacDo"))
            client.createExpense(expense.copy(title = "Coffee"))
        }

        runAsUser1 {
            val result = client.listExpenses()
            assertEquals(1, result.size)
            assertTrue {
                result.any { it.title == "Carrefour" }
            }
        }

        runAsUser2 {
            val result = client.listExpenses()
            assertEquals(2, result.size)
            assertTrue {
                result.any { it.title == "MacDo" }
            }
            assertTrue {
                result.any { it.title == "Coffee" }
            }
        }
    }

    @Test
    fun `filter by category`() = runTestAsUser{
        val category1 = client.createCategory(categoryInTemplate)
        val category2 = client.createCategory(categoryInTemplate)

        val expense1 = client.createExpense(expense.copy(categoryId = category1.subcategories.first().id))
        val expense2 = client.createExpense(expense.copy(categoryId = category2.subcategories.first().id))
        val expense3 = client.createExpense(expense.copy(categoryId = category1.subcategories.first().id))

        val result1 = client.listExpenses(mapOf("categoryId" to category1.id.toString()))
        val result2 = client.listExpenses(mapOf("categoryId" to category2.id.toString()))

        assertEquals(setOf(expense1.id, expense3.id), result1.map { it.id }.toSet())
        assertEquals(setOf(expense2.id), result2.map { it.id }.toSet())
    }

    @Test
    fun `filter by subcategory`() = runTestAsUser{
    }

    @Test
    fun `filter by type`() = runTestAsUser{
        val category1 = client.createCategory(categoryInTemplate)
        val category2 = client.createCategory(categoryInTemplate)

        val expense1 = client.createExpense(expense.copy(categoryId = category1.subcategories.first().id))
        val expense2 = client.createExpense(expense.copy(categoryId = category2.subcategories.first().id))
        val expense3 = client.createExpense(expense.copy(categoryId = category1.subcategories.first().id))

        val result1 = client.listExpenses(mapOf("categoryId" to category1.id.toString()))
        val result2 = client.listExpenses(mapOf("categoryId" to category2.id.toString()))

        assertEquals(setOf(expense1.id, expense3.id), result1.map { it.id }.toSet())
        assertEquals(setOf(expense2.id), result2.map { it.id }.toSet())
    }
}