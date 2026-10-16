package com.xavierclavel.bankable.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The reports the app computes locally. Cases and expected figures are those of the backend's
 * SummaryControllerTest and TrendsControllerTest, which the app's reports replace.
 */
class ReportsTest {

    /** An expense in subcategory [sub] (named after it) of category [cat]. */
    private data class Entry(val sub: String?, val cat: String?, val amount: Long, val date: String, val type: String = "EXPENSE")

    private val now = monthIndex(2026, 10)

    private fun groceries(amount: Long, date: String) = Entry("groceries", "food", amount * 100, date)
    private fun salary(amount: Long, date: String) = Entry("salary", "income", amount * 100, date, "INCOME")

    /** The monthly totals the database would return for [entries]. */
    private fun totals(vararg entries: Entry): List<MonthlyTotal> = entries
        .groupBy { Triple(monthIndex(it.date.take(4).toInt(), it.date.substring(5, 7).toInt()), it.sub, it.type) }
        .map { (key, rows) ->
            MonthlyTotal(
                month = key.first,
                subcategoryId = key.second,
                categoryId = rows.first().cat,
                subcategoryName = key.second,
                type = key.third,
                cents = rows.sumOf { it.amount },
            )
        }

    @Test
    fun monthSummary() {
        val totals = totals(
            groceries(25, "2020-06-06"),
            groceries(15, "2020-06-30"),
            Entry("activities", "leisure", 1000, "2020-06-01"),
            Entry("activities", "leisure", 1000, "2020-07-01"),
            Entry(null, null, 500, "2020-06-15"),
        )

        val june = monthIndex(2020, 6)
        val summary = Reports.summary(totals, june, june)

        assertEquals("55.00", summary.totalExpenses)
        assertEquals("0.00", summary.totalIncome)
        val bySubcategory = summary.expensesByCategory.associate { it.categoryId to it.total }
        assertEquals(mapOf("groceries" to "40.00", "activities" to "10.00", null to "5.00"), bySubcategory)
    }

    @Test
    fun yearSummaryCoversItsTwelveMonths() {
        val totals = totals(groceries(25, "2019-12-31"), groceries(15, "2020-01-01"), groceries(10, "2020-12-31"))

        val summary = Reports.summary(totals, monthIndex(2020, 1), monthIndex(2020, 12))

        assertEquals("25.00", summary.totalExpenses)
    }

    /** The outliers with the app's defaults: +50%, from 250 €, two years either side. */
    private fun outliers(
        totals: List<MonthlyTotal>,
        year: Int,
        threshold: Double = 0.5,
        minTotal: Long = 250,
        yearsAround: Int = 2,
    ) = Reports.outliers(totals, year, threshold, minTotal * 100, yearsAround, now)

    // History spans 2019-01 to 2020-12 (24 months), all of it inside the five-year window
    // around either year, so a full year's mean is half of the subcategory's total.
    private val outlierHistory = totals(
        Entry("groceries", "food", 10000, "2019-01-10"),
        Entry("groceries", "food", 10000, "2020-12-10"),
        Entry("travel", "travel", 10000, "2019-06-01"),
        Entry("travel", "travel", 70000, "2020-06-01"),
        Entry("gifts", "gifts", 30000, "2019-09-01"),
        Entry("bonus", "bonus", 50000, "2020-03-01", "INCOME"),
    )

    @Test
    fun yearOutliersAreTheSubcategoriesWellAboveTheirMean() {
        val result = outliers(outlierHistory, 2020)

        // Groceries sits right at its mean and Gifts has nothing in 2020: neither shows.
        assertEquals(listOf("bonus", "travel"), result.map { it.subcategoryId })
        val bonus = result[0]
        assertEquals("INCOME", bonus.type)
        assertEquals("500.00", bonus.total)
        assertEquals("250.00", bonus.mean)
        assertEquals(1.0, bonus.deviation, 1e-9)
        val travel = result[1]
        assertEquals("EXPENSE", travel.type)
        assertEquals("700.00", travel.total)
        assertEquals("400.00", travel.mean)
        assertEquals(0.75, travel.deviation, 1e-9)

        assertEquals(listOf("gifts"), outliers(outlierHistory, 2019).map { it.subcategoryId })
    }

    @Test
    fun yearOutliersHonorTheThreshold() {
        assertEquals(listOf("bonus"), outliers(outlierHistory, 2020, threshold = 0.8).map { it.subcategoryId })
        assertEquals(
            listOf("bonus", "travel", "groceries"),
            outliers(outlierHistory, 2020, threshold = 0.0, minTotal = 0).map { it.subcategoryId },
        )
    }

    @Test
    fun yearOutliersLeaveOutTotalsUnderTheMinimum() {
        // Groceries (100) clears a zero threshold but not the default 250 minimum.
        assertEquals(listOf("bonus", "travel"), outliers(outlierHistory, 2020, threshold = 0.0).map { it.subcategoryId })
        // Bonus totals exactly 500: a 500 minimum keeps it, a 600 one doesn't.
        assertEquals(listOf("bonus", "travel"), outliers(outlierHistory, 2020, minTotal = 500).map { it.subcategoryId })
        assertEquals(listOf("travel"), outliers(outlierHistory, 2020, minTotal = 600).map { it.subcategoryId })
    }

    @Test
    fun yearOutliersCompareAgainstTheYearsAroundThem() {
        val totals = totals(
            Entry("rent", "home", 900000, "2014-01-01"),
            Entry("rent", "home", 60000, "2018-01-01"),
            Entry("rent", "home", 60000, "2019-01-01"),
            Entry("rent", "home", 180000, "2020-12-01"),
            Entry("car", "car", 30000, "2014-06-01"),
            Entry("car", "car", 150000, "2016-06-01"),
            Entry("car", "car", 30000, "2018-06-01"),
            Entry("car", "car", 300000, "2020-06-01"),
        )

        // The history ends in 2020-12, so 2020's window is cut to 2018-2020 (36 months).
        // Against the all-time mean, Rent's 2014 outlay would have hidden its 2020 rise.
        val latest = outliers(totals, 2020)
        assertEquals(listOf("car", "rent"), latest.map { it.subcategoryId })
        assertEquals("1000.00", latest[1].mean)
        assertEquals(0.8, latest[1].deviation, 1e-9)

        // 2016's window is the whole of 2014-2018: Car's 2020 outlay falls outside it.
        val middle = outliers(totals, 2016)
        assertEquals(listOf("car"), middle.map { it.subcategoryId })
        assertEquals("420.00", middle[0].mean)

        // One year either side, 2015-2017 only holds Car's 2016 outlay.
        val narrow = outliers(totals, 2016, yearsAround = 1)
        assertEquals("500.00", narrow[0].mean)
        assertEquals(2.0, narrow[0].deviation, 1e-9)
    }

    @Test
    fun yearOutliersAreEmptyOutsideTheHistory() {
        assertTrue(outliers(outlierHistory, 2018).isEmpty())
    }

    @Test
    fun monthTrendsShowEmptyMonths() {
        val totals = totals(
            groceries(25, "2020-06-06"),
            groceries(15, "2020-06-30"),
            salary(10, "2020-06-01"),
            salary(11, "2020-08-01"),
        )

        val result = Reports.incomeExpenseByMonth(totals)

        assertEquals(listOf(6, 7, 8), result.map { it.month })
        assertEquals("40.00", result[0].totalExpenses)
        assertEquals("10.00", result[0].totalIncome)
        assertEquals("0.00", result[1].totalIncome)
        assertEquals("0.00", result[1].totalExpenses)
        assertEquals("11.00", result[2].totalIncome)
    }

    @Test
    fun yearTrendsShowEmptyYears() {
        val totals = totals(
            groceries(25, "2018-06-06"),
            groceries(25, "2020-06-06"),
            groceries(15, "2020-06-30"),
            salary(10, "2020-06-01"),
            salary(11, "2020-07-01"),
        )

        val result = Reports.incomeExpenseByYear(totals, now)

        assertEquals(listOf(2018, 2019, 2020), result.map { it.year })
        assertEquals("25.00", result[0].totalExpenses)
        assertEquals("0.00", result[1].totalExpenses)
        assertEquals("40.00", result[2].totalExpenses)
        assertEquals("21.00", result[2].totalIncome)
    }

    private val medianHistory = totals(
        groceries(25, "2019-06-06"),
        groceries(25, "2020-06-06"),
        groceries(15, "2020-07-30"),
        groceries(15, "2020-07-30"),
        groceries(5, "2020-08-30"),
        groceries(15, "2020-09-30"),
        groceries(15, "2020-10-30"),
        groceries(15, "2020-11-30"),
        groceries(15, "2020-12-30"),
        salary(10, "2020-06-01"),
        salary(11, "2020-07-01"),
    )

    @Test
    fun yearTrendsGiveTheMedianOfTheMonths() {
        val result = Reports.incomeExpenseByYear(medianHistory, now)

        assertEquals(2, result.size)
        assertEquals("10.00", result[1].medianExpenses)
    }

    @Test
    fun yearFlowIsIncomesMinusExpenses() {
        val result = Reports.flowByYear(medianHistory, now)

        assertEquals(2, result.size)
        assertEquals("-99.00", result[1].total)
        assertEquals("-8.25", result[1].average)
        assertEquals("-10.00", result[1].median)
    }

    @Test
    fun yearTrendsLeaveOutTheMonthInProgress() {
        val totals = totals(groceries(30, "2026-09-15"), groceries(1000, "2026-10-01"))

        val result = Reports.incomeExpenseByYear(totals, now)

        assertEquals("30.00", result.single().totalExpenses)
        assertEquals("30.00", result.single().meanExpenses)
    }

    @Test
    fun categoryTrendsByMonthShowEmptyMonths() {
        val totals = totals(
            groceries(25, "2020-06-06"),
            groceries(15, "2020-06-30"),
            groceries(10, "2020-08-01"),
            salary(11, "2020-08-01"),
        )

        val food = Reports.trendByMonth(totals) { it.categoryId == "food" }
        assertEquals(listOf("40.00", "0.00", "10.00"), food.map { it.total })
        val income = Reports.trendByMonth(totals) { it.subcategoryId == "salary" }
        assertEquals(listOf("0.00", "0.00", "11.00"), income.map { it.total })
    }

    @Test
    fun categoryTrendsByYearShowEmptyYears() {
        val totals = totals(
            groceries(25, "2018-06-06"),
            groceries(15, "2020-06-30"),
            groceries(10, "2020-07-01"),
            salary(11, "2020-07-01"),
        )

        val food = Reports.trendByYear(totals) { it.categoryId == "food" }
        assertEquals(listOf(2018, 2019, 2020), food.map { it.year })
        assertEquals(listOf("25.00", "0.00", "25.00"), food.map { it.total })
        val income = Reports.trendByYear(totals) { it.subcategoryId == "salary" }
        assertEquals("11.00", income[2].total)
    }

    @Test
    fun noExpensesNoReports() {
        assertNull(Reports.firstMonth(emptyList()))
        assertTrue(Reports.incomeExpenseByMonth(emptyList()).isEmpty())
        assertTrue(Reports.incomeExpenseByYear(emptyList(), now).isEmpty())
        assertTrue(Reports.trendByYear(emptyList()) { true }.isEmpty())
        assertEquals("0.00", Reports.summary(emptyList(), now, now).totalExpenses)
    }
}
