package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.getMonthCategoryTrends
import com.xavierclavel.utils.getMonthTrends
import com.xavierclavel.utils.getYearCategoryTrends
import com.xavierclavel.utils.getYearTrends
import com.xavierclavel.utils.logger
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

class TrendsControllerTest: ApplicationTest() {
    @OptIn(ExperimentalTime::class)
    val expense = ExpenseIn(
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = 0,
        type = ExpenseType.EXPENSE
    )

    val categoryInTemplate = CategoryIn(name = "Groceries", type = ExpenseType.EXPENSE, color = "", icon = "")


    @Test
    fun `get month trends`() = runTestAsUser {
            val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
            val salaryId = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME)).subcategories[0].id
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
                categoryId = salaryId,
                type = ExpenseType.INCOME,
            ))
            client.createExpense(expense.copy(
                amount = BigDecimal("11.00"),
                date = LocalDate.parse("2020-07-01"),
                categoryId = salaryId,
                type = ExpenseType.INCOME,
            ))
        val result = client.getMonthTrends()
        logger.info {result}
        assertEquals(2, result.size)
        assertEquals(0, BigDecimal("40").compareTo(result[0].totalExpenses))
        assertEquals(0, BigDecimal("10").compareTo(result[0].totalIncome))
        assertEquals(0, BigDecimal("11").compareTo(result[1].totalIncome))
    }


    @Test
    fun `get year trends`() = runTestAsUser {
        val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
        val salaryId = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME)).subcategories[0].id
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2019-06-06"),
            categoryId = groceriesId,
        ))
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
            categoryId = salaryId,
            type = ExpenseType.INCOME,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("11.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = salaryId,
            type = ExpenseType.INCOME,
        ))
        val result = client.getYearTrends()
        logger.info {result}
        assertEquals(2, result.size)
        assertEquals(0, BigDecimal("25").compareTo(result[0].totalExpenses))
        assertEquals(0, BigDecimal("40").compareTo(result[1].totalExpenses))
        assertEquals(0, BigDecimal("21").compareTo(result[1].totalIncome))
    }



    @Test
    fun `get category trends by month`() = runTestAsUser {
        val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
        val salaryId = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME)).subcategories[0].id
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
            date = LocalDate.parse("2020-07-01"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("11.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = salaryId,
        ))
        val result = client.getMonthCategoryTrends(groceriesId)
        assertEquals(2, result.size)
        assertEquals(0, BigDecimal("40").compareTo(result[0].total))
        assertEquals(0, BigDecimal("10").compareTo(result[1].total))

        val result2 = client.getMonthCategoryTrends(salaryId)
        assertEquals(1, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[0].total))
    }


    @Test
    fun `get category trends by year`() = runTestAsUser {
        val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
        val salaryId = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME)).subcategories[0].id
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2019-06-06"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-06-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("10.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("11.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = salaryId,
        ))
        val result = client.getYearCategoryTrends(groceriesId)
        assertEquals(2, result.size)
        assertEquals(0, BigDecimal("25").compareTo(result[0].total))
        assertEquals(0, BigDecimal("25").compareTo(result[1].total))

        val result2 = client.getMonthCategoryTrends(salaryId)
        assertEquals(1, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[0].total))
    }

}