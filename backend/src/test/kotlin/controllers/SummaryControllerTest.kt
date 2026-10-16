package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.TestBuilderWrapper
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.getSummary
import com.xavierclavel.utils.getYearOutliers
import com.xavierclavel.utils.SUMMARY_URL
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

class SummaryControllerTest: ApplicationTest() {
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
        val result = client.getSummary(year = 2020, month = 6)
        assertEquals(BigDecimal("50.00"), result.totalExpenses)

        assertEquals(2, result.expensesByCategory.size)
    }

    // History spans 2019-01 to 2020-12 (24 months), all of it inside the five-year window
    // around either year, so a full year's mean is half of the subcategory's total.
    private suspend fun TestBuilderWrapper.createOutlierHistory(): Map<String, Long> {
        val groceriesId = client.createCategory(categoryInTemplate.copy(name = "Groceries")).subcategories[0].id
        val travelId = client.createCategory(categoryInTemplate.copy(name = "Travel")).subcategories[0].id
        val giftsId = client.createCategory(categoryInTemplate.copy(name = "Gifts")).subcategories[0].id
        val bonusId = client.createCategory(categoryInTemplate.copy(name = "Bonus", type = ExpenseType.INCOME)).subcategories[0].id
        listOf(
            Triple(groceriesId, "100.00", "2019-01-10"),
            Triple(groceriesId, "100.00", "2020-12-10"),
            Triple(travelId, "100.00", "2019-06-01"),
            Triple(travelId, "700.00", "2020-06-01"),
            Triple(giftsId, "300.00", "2019-09-01"),
        ).forEach { (categoryId, amount, date) ->
            client.createExpense(expense.copy(categoryId = categoryId, amount = BigDecimal(amount), date = LocalDate.parse(date)))
        }
        client.createExpense(expense.copy(
            categoryId = bonusId,
            amount = BigDecimal("500.00"),
            date = LocalDate.parse("2020-03-01"),
            type = ExpenseType.INCOME,
        ))
        return mapOf("groceries" to groceriesId, "travel" to travelId, "gifts" to giftsId, "bonus" to bonusId)
    }

    @Test
    fun `year outliers are the subcategories well above their mean`() = runTestAsUser {
        val ids = createOutlierHistory()

        val result = client.getYearOutliers(year = 2020)

        // Groceries sits right at its mean and Gifts has nothing in 2020: neither shows.
        assertEquals(listOf(ids["bonus"], ids["travel"]), result.map { it.subcategoryId })
        val bonus = result[0]
        assertEquals(ExpenseType.INCOME, bonus.type)
        assertEquals(0, BigDecimal("500").compareTo(bonus.total))
        assertEquals(0, BigDecimal("250").compareTo(bonus.mean))
        assertEquals(1.0, bonus.deviation, 1e-9)
        val travel = result[1]
        assertEquals(ExpenseType.EXPENSE, travel.type)
        assertEquals(0, BigDecimal("700").compareTo(travel.total))
        assertEquals(0, BigDecimal("400").compareTo(travel.mean))
        assertEquals(0.75, travel.deviation, 1e-9)

        assertEquals(listOf(ids["gifts"]), client.getYearOutliers(year = 2019).map { it.subcategoryId })
    }

    @Test
    fun `year outliers honor the threshold`() = runTestAsUser {
        val ids = createOutlierHistory()

        assertEquals(listOf(ids["bonus"]), client.getYearOutliers(year = 2020, threshold = 0.8).map { it.subcategoryId })
        assertEquals(
            listOf(ids["bonus"], ids["travel"], ids["groceries"]),
            client.getYearOutliers(year = 2020, threshold = 0.0, minTotal = 0).map { it.subcategoryId },
        )
    }

    @Test
    fun `year outliers leave out totals under the minimum`() = runTestAsUser {
        val ids = createOutlierHistory()

        // Groceries (100) clears a zero threshold but not the default 250 minimum.
        assertEquals(
            listOf(ids["bonus"], ids["travel"]),
            client.getYearOutliers(year = 2020, threshold = 0.0).map { it.subcategoryId },
        )
        // Bonus totals exactly 500: a 500 minimum keeps it, a 600 one doesn't.
        assertEquals(listOf(ids["bonus"], ids["travel"]), client.getYearOutliers(year = 2020, minTotal = 500).map { it.subcategoryId })
        assertEquals(listOf(ids["travel"]), client.getYearOutliers(year = 2020, minTotal = 600).map { it.subcategoryId })
    }

    @Test
    fun `year outliers compare against the years around them`() = runTestAsUser {
        val rentId = client.createCategory(categoryInTemplate.copy(name = "Rent")).subcategories[0].id
        val carId = client.createCategory(categoryInTemplate.copy(name = "Car")).subcategories[0].id
        listOf(
            Triple(rentId, "9000.00", "2014-01-01"),
            Triple(rentId, "600.00", "2018-01-01"),
            Triple(rentId, "600.00", "2019-01-01"),
            Triple(rentId, "1800.00", "2020-12-01"),
            Triple(carId, "300.00", "2014-06-01"),
            Triple(carId, "1500.00", "2016-06-01"),
            Triple(carId, "300.00", "2018-06-01"),
            Triple(carId, "3000.00", "2020-06-01"),
        ).forEach { (categoryId, amount, date) ->
            client.createExpense(expense.copy(categoryId = categoryId, amount = BigDecimal(amount), date = LocalDate.parse(date)))
        }

        // The history ends in 2020-12, so 2020's window is cut to 2018-2020 (36 months).
        // Against the all-time mean, Rent's 2014 outlay would have hidden its 2020 rise.
        val latest = client.getYearOutliers(year = 2020)
        assertEquals(listOf(carId, rentId), latest.map { it.subcategoryId })
        assertEquals(0, BigDecimal("1000").compareTo(latest[1].mean))
        assertEquals(0.8, latest[1].deviation, 1e-9)

        // 2016's window is the whole of 2014-2018: Car's 2020 outlay falls outside it.
        val middle = client.getYearOutliers(year = 2016)
        assertEquals(listOf(carId), middle.map { it.subcategoryId })
        assertEquals(0, BigDecimal("420").compareTo(middle[0].mean))

        // One year either side, 2015-2017 only holds Car's 2016 outlay.
        val narrow = client.getYearOutliers(year = 2016, yearsAround = 1)
        assertEquals(0, BigDecimal("500").compareTo(narrow[0].mean))
        assertEquals(2.0, narrow[0].deviation, 1e-9)
    }

    @Test
    fun `year outliers are empty outside the history`() = runTestAsUser {
        createOutlierHistory()

        assertEquals(emptyList(), client.getYearOutliers(year = 2018))
    }

    @Test
    fun `year outliers reject malformed parameters`() = runTestAsUser {
        listOf("threshold=abc", "minTotal=abc", "yearsAround=abc", "yearsAround=-1").forEach { query ->
            val response = client.get("$SUMMARY_URL/year/2020/outliers?$query")
            assertEquals(HttpStatusCode.BadRequest, response.status, query)
        }
    }

}