package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.getMonthCategoryTrends
import com.xavierclavel.utils.getMonthSubcategoryTrends
import com.xavierclavel.utils.getMonthTrends
import com.xavierclavel.utils.getYearCategoryTrends
import com.xavierclavel.utils.getYearFlow
import com.xavierclavel.utils.getYearMedians
import com.xavierclavel.utils.getYearSubcategoryTrends
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
    fun `empty months are displayed`() = runTestAsUser {
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
            date = LocalDate.parse("2020-08-01"),
            categoryId = salaryId,
            type = ExpenseType.INCOME,
        ))
        val result = client.getMonthTrends()
        logger.info {result}
        assertEquals(3, result.size)
        assertEquals(0, BigDecimal("40").compareTo(result[0].totalExpenses))
        assertEquals(0, BigDecimal("10").compareTo(result[0].totalIncome))
        assertEquals(0, BigDecimal("0").compareTo(result[1].totalIncome))
        assertEquals(0, BigDecimal("0").compareTo(result[1].totalExpenses))
        assertEquals(0, BigDecimal("11").compareTo(result[2].totalIncome))
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
    fun `get year medians`() = runTestAsUser {
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
            date = LocalDate.parse("2020-07-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-07-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("5.00"),
            date = LocalDate.parse("2020-08-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-09-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-10-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-11-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-12-30"),
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
        val result = client.getYearMedians()
        logger.info {result}
        assertEquals(2, result.size)
        assertEquals(0, BigDecimal("10").compareTo(result[1].totalExpenses))
    }

    @Test
    fun `get year flow`() = runTestAsUser {
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
            date = LocalDate.parse("2020-07-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-07-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("5.00"),
            date = LocalDate.parse("2020-08-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-09-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-10-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-11-30"),
            categoryId = groceriesId,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-12-30"),
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
        val result = client.getYearFlow()
        logger.info {result}
        assertEquals(2, result.size)
        assertEquals(0, BigDecimal("-99").compareTo(result[1].total))
        assertEquals(0, BigDecimal("-8.25").compareTo(result[1].average))
        assertEquals(0, BigDecimal("-10").compareTo(result[1].median))
    }

    @Test
    fun `empty years are shown`() = runTestAsUser {
        val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
        val salaryId = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME)).subcategories[0].id
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2018-06-06"),
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
        assertEquals(3, result.size)
        assertEquals(2018, result[0].year)
        assertEquals(0, BigDecimal("25").compareTo(result[0].totalExpenses))
        assertEquals(0, BigDecimal("0").compareTo(result[1].totalExpenses))
        assertEquals(0, BigDecimal("0").compareTo(result[1].totalIncome))
        assertEquals(0, BigDecimal("40").compareTo(result[2].totalExpenses))
        assertEquals(0, BigDecimal("21").compareTo(result[2].totalIncome))
    }



    @Test
    fun `get category trends by month`() = runTestAsUser {
        val cat1 = client.createCategory(categoryInTemplate.copy(name = "Groceries"))
        val cat2 = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME))
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2020-06-06"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-06-30"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("10.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("11.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = cat2.subcategories[0].id,
        ))
        val result = client.getMonthCategoryTrends(cat1.id)
        println(result)
        assertEquals(2, result.size)
        assertEquals(0, BigDecimal("40").compareTo(result[0].total))
        assertEquals(0, BigDecimal("10").compareTo(result[1].total))

        val result2 = client.getMonthCategoryTrends(cat2.id)
        assertEquals(2, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[1].total))
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

        val result2 = client.getYearCategoryTrends(salaryId)
        assertEquals(2, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[1].total))
    }

    @Test
    fun `empty category trends by month are shown`() = runTestAsUser {
        val cat1 = client.createCategory(categoryInTemplate.copy(name = "Groceries"))
        val cat2 = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME))
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2020-06-06"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-06-30"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("10.00"),
            date = LocalDate.parse("2020-08-01"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("11.00"),
            date = LocalDate.parse("2020-08-01"),
            categoryId = cat2.subcategories[0].id,
        ))
        val result = client.getMonthCategoryTrends(cat1.id)
        println(result)
        assertEquals(3, result.size)
        assertEquals(0, BigDecimal("40").compareTo(result[0].total))
        assertEquals(0, BigDecimal("0").compareTo(result[1].total))
        assertEquals(0, BigDecimal("10").compareTo(result[2].total))

        val result2 = client.getMonthCategoryTrends(cat2.id)
        assertEquals(3, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[2].total))
    }


    @Test
    fun `empty category trends by year are shown`() = runTestAsUser {
        val groceries = client.createCategory(categoryInTemplate.copy(name = "Groceries"))
        val salary = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME))
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2018-06-06"),
            categoryId = groceries.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-06-30"),
            categoryId = groceries.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("10.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = groceries.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("11.00"),
            date = LocalDate.parse("2020-07-01"),
            categoryId = salary.subcategories[0].id,
        ))
        val result = client.getYearCategoryTrends(groceries.id)
        assertEquals(3, result.size)
        assertEquals(2018, result[0].year)
        assertEquals(0, BigDecimal("25").compareTo(result[0].total))
        assertEquals(0, BigDecimal("0").compareTo(result[1].total))
        assertEquals(0, BigDecimal("25").compareTo(result[2].total))

        val result2 = client.getYearCategoryTrends(salary.id)
        assertEquals(3, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[2].total))
    }

    @Test
    fun `empty subcategory trends by month are shown`() = runTestAsUser {
        val cat1 = client.createCategory(categoryInTemplate.copy(name = "Groceries"))
        val cat2 = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME))
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2020-06-06"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("15.00"),
            date = LocalDate.parse("2020-06-30"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("10.00"),
            date = LocalDate.parse("2020-08-01"),
            categoryId = cat1.subcategories[0].id,
        ))
        client.createExpense(expense.copy(
            amount = BigDecimal("11.00"),
            date = LocalDate.parse("2020-08-01"),
            categoryId = cat2.subcategories[0].id,
        ))
        val result = client.getMonthSubcategoryTrends(cat1.subcategories[0].id)
        println(result)
        assertEquals(3, result.size)
        assertEquals(0, BigDecimal("40").compareTo(result[0].total))
        assertEquals(0, BigDecimal("0").compareTo(result[1].total))
        assertEquals(0, BigDecimal("10").compareTo(result[2].total))

        val result2 = client.getMonthCategoryTrends(cat2.subcategories[0].id)
        assertEquals(3, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[2].total))
    }


    @Test
    fun `empty subcategory trends by year are shown`() = runTestAsUser {
        val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
        val salaryId = client.createCategory(categoryInTemplate.copy(name = "Salary", type = ExpenseType.INCOME)).subcategories[0].id
        client.createExpense(expense.copy(
            amount = BigDecimal("25.00"),
            date = LocalDate.parse("2018-06-06"),
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
        val result = client.getYearSubcategoryTrends(groceriesId)
        assertEquals(3, result.size)
        assertEquals(2018, result[0].year)
        assertEquals(0, BigDecimal("25").compareTo(result[0].total))
        assertEquals(0, BigDecimal("0").compareTo(result[1].total))
        assertEquals(0, BigDecimal("25").compareTo(result[2].total))

        val result2 = client.getYearSubcategoryTrends(salaryId)
        assertEquals(3, result2.size)
        assertEquals(0, BigDecimal("11").compareTo(result2[2].total))
    }

}