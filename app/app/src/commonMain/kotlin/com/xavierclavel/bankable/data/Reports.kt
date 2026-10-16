package com.xavierclavel.bankable.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.xavierclavel.bankable.model.CategorySummaryDto
import com.xavierclavel.bankable.model.CategoryTrendDto
import com.xavierclavel.bankable.model.SubcategoryOutlierDto
import com.xavierclavel.bankable.model.SummaryDto
import com.xavierclavel.bankable.model.TrendDto
import com.xavierclavel.bankable.util.currentMonth
import com.xavierclavel.bankable.util.currentYear
import com.xavierclavel.bankable.util.formatCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.floor

/**
 * A month, counted from year 0 so that consecutive months are consecutive numbers:
 * `year * 12 + (month - 1)`.
 */
fun monthIndex(year: Int, month: Int): Int = year * 12 + (month - 1)

fun currentMonthIndex(): Int = monthIndex(currentYear(), currentMonth())

private val Int.yearOf get() = this / 12
private val Int.monthOf get() = this % 12 + 1

/** The total of a subcategory's expenses of one type ("EXPENSE" / "INCOME") in one month. */
data class MonthlyTotal(
    val month: Int,
    val subcategoryId: String?,
    val categoryId: String?,
    val subcategoryName: String?,
    val type: String,
    val cents: Long,
)

/**
 * The summary, outliers and trends, computed on the device from the local expenses, so that
 * they include the changes not synced yet and work offline. They follow what the backend's
 * SummaryService and TrendService compute.
 */
object Reports {

    /** The monthly totals every report is computed from. Emits again whenever expenses change. */
    fun observeMonthlyTotals(): Flow<List<MonthlyTotal>> =
        database.reportQueries.monthlyTotals()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map {
                    MonthlyTotal(
                        month = monthIndex(it.month.substring(0, 4).toInt(), it.month.substring(5, 7).toInt()),
                        subcategoryId = it.subcategory_id,
                        categoryId = it.category_id,
                        subcategoryName = it.subcategory_name,
                        type = it.type,
                        cents = it.total_cents ?: 0,
                    )
                }
            }

    /** The first month with an expense, or null when there are none. */
    fun firstMonth(totals: List<MonthlyTotal>): Int? = totals.minOfOrNull { it.month }

    /** Incomes and expenses from [firstMonth] to [lastMonth] included, in all and per subcategory. */
    fun summary(totals: List<MonthlyTotal>, firstMonth: Int, lastMonth: Int): SummaryDto {
        val period = totals.filter { it.month in firstMonth..lastMonth }
        fun bySubcategory(type: String) = period
            .filter { it.type == type }
            .groupBy { it.subcategoryId }
            .map { (subcategoryId, rows) ->
                CategorySummaryDto(
                    categoryId = subcategoryId,
                    categoryName = rows.first().subcategoryName.orEmpty(),
                    total = formatCents(rows.sumOf { it.cents }),
                )
            }
        return SummaryDto(
            totalExpenses = formatCents(period.filter { it.type == EXPENSE }.sumOf { it.cents }),
            totalIncome = formatCents(period.filter { it.type == INCOME }.sumOf { it.cents }),
            expensesByCategory = bySubcategory(EXPENSE),
            incomeByCategory = bySubcategory(INCOME),
        )
    }

    /**
     * The subcategories whose monthly pace over [year] is at least [threshold] above their
     * monthly mean over the [yearsAround] years either side of it, most deviating first. Those
     * whose total over [year] stays under [minTotalCents] are left out.
     *
     * Paces are compared per month rather than per year, so a year the history only partly
     * covers (the first one, or the one in progress) is judged fairly. The history, and so the
     * window of years around [year], ends with the last month on record, never past
     * [currentMonth]: future-dated entries would otherwise count months that haven't happened.
     */
    fun outliers(
        totals: List<MonthlyTotal>,
        year: Int,
        threshold: Double,
        minTotalCents: Long,
        yearsAround: Int,
        currentMonth: Int = currentMonthIndex(),
    ): List<SubcategoryOutlierDto> {
        val historyFirst = totals.minOfOrNull { it.month } ?: return emptyList()
        val historyLast = minOf(totals.maxOf { it.month }, currentMonth)
        val yearFirst = maxOf(historyFirst, monthIndex(year, 1))
        val yearLast = minOf(historyLast, monthIndex(year, 12))
        if (yearFirst > yearLast) return emptyList()
        val windowFirst = maxOf(historyFirst, monthIndex(year - yearsAround, 1))
        val windowLast = minOf(historyLast, monthIndex(year + yearsAround, 12))
        val windowMonths = windowLast - windowFirst + 1
        val yearMonths = yearLast - yearFirst + 1

        return totals
            .filter { it.subcategoryId != null && it.month in windowFirst..windowLast }
            .groupBy { it.subcategoryId to it.type }
            .mapNotNull { (key, rows) ->
                val windowTotal = rows.sumOf { it.cents }
                val yearTotal = rows.filter { it.month in yearFirst..yearLast }.sumOf { it.cents }
                if (yearTotal <= 0 || yearTotal < minTotalCents || windowTotal <= 0) return@mapNotNull null
                SubcategoryOutlierDto(
                    subcategoryId = key.first!!,
                    subcategoryName = rows.first().subcategoryName.orEmpty(),
                    type = key.second,
                    total = formatCents(yearTotal),
                    mean = formatCents(roundHalfUp(windowTotal.toDouble() * yearMonths / windowMonths)),
                    deviation = (yearTotal.toDouble() * windowMonths) / (windowTotal.toDouble() * yearMonths) - 1,
                )
            }
            .filter { it.deviation >= threshold }
            .sortedByDescending { it.deviation }
    }

    /** Incomes and expenses of every month, from the first to the last one with an entry. */
    fun incomeExpenseByMonth(totals: List<MonthlyTotal>): List<TrendDto> {
        val months = monthRange(totals) ?: return emptyList()
        val income = monthlySums(totals) { it.type == INCOME }
        val expenses = monthlySums(totals) { it.type == EXPENSE }
        return months.map {
            TrendDto(
                year = it.yearOf,
                month = it.monthOf,
                totalIncome = formatCents(income[it] ?: 0),
                totalExpenses = formatCents(expenses[it] ?: 0),
            )
        }
    }

    /**
     * Incomes and expenses of every year: total, mean and median of its months. Only finished
     * months count, so that the month in progress doesn't drag the mean down.
     */
    fun incomeExpenseByYear(totals: List<MonthlyTotal>, currentMonth: Int = currentMonthIndex()): List<TrendDto> {
        val finished = totals.filter { it.month < currentMonth }
        val months = monthRange(finished) ?: return emptyList()
        val income = monthlySums(finished) { it.type == INCOME }
        val expenses = monthlySums(finished) { it.type == EXPENSE }
        return months.groupBy { it.yearOf }.map { (year, yearMonths) ->
            val incomes = yearMonths.map { income[it] ?: 0L }
            val spent = yearMonths.map { expenses[it] ?: 0L }
            TrendDto(
                year = year,
                totalIncome = formatCents(incomes.sum()),
                totalExpenses = formatCents(spent.sum()),
                meanIncome = formatCents(roundHalfUp(incomes.average())),
                meanExpenses = formatCents(roundHalfUp(spent.average())),
                medianIncome = formatCents(roundHalfUp(median(incomes))),
                medianExpenses = formatCents(roundHalfUp(median(spent))),
            )
        }
    }

    /** What was saved each year (incomes minus expenses): total, mean and median of its finished months. */
    fun flowByYear(totals: List<MonthlyTotal>, currentMonth: Int = currentMonthIndex()): List<CategoryTrendDto> {
        val finished = totals.filter { it.month < currentMonth }
        val months = monthRange(finished) ?: return emptyList()
        val flow = finished.groupBy { it.month }.mapValues { (_, rows) ->
            rows.sumOf { if (it.type == INCOME) it.cents else -it.cents }
        }
        return byYear(months, flow)
    }

    /** The monthly totals of the rows [matching] (e.g. a category's), over the whole history. */
    fun trendByMonth(totals: List<MonthlyTotal>, matching: (MonthlyTotal) -> Boolean): List<CategoryTrendDto> {
        val months = monthRange(totals) ?: return emptyList()
        val sums = monthlySums(totals, matching)
        return months.map {
            CategoryTrendDto(year = it.yearOf, month = it.monthOf, total = formatCents(sums[it] ?: 0))
        }
    }

    /** The yearly total, mean and median of the rows [matching], over the whole history. */
    fun trendByYear(totals: List<MonthlyTotal>, matching: (MonthlyTotal) -> Boolean): List<CategoryTrendDto> {
        val months = monthRange(totals) ?: return emptyList()
        return byYear(months, monthlySums(totals, matching))
    }

    private fun byYear(months: IntRange, sums: Map<Int, Long>): List<CategoryTrendDto> =
        months.groupBy { it.yearOf }.map { (year, yearMonths) ->
            val values = yearMonths.map { sums[it] ?: 0L }
            CategoryTrendDto(
                year = year,
                total = formatCents(values.sum()),
                average = formatCents(roundHalfUp(values.average())),
                median = formatCents(roundHalfUp(median(values))),
            )
        }

    private fun monthRange(totals: List<MonthlyTotal>): IntRange? {
        if (totals.isEmpty()) return null
        return totals.minOf { it.month }..totals.maxOf { it.month }
    }

    private fun monthlySums(totals: List<MonthlyTotal>, matching: (MonthlyTotal) -> Boolean): Map<Int, Long> =
        totals.filter(matching).groupBy { it.month }.mapValues { (_, rows) -> rows.sumOf { it.cents } }

    /** The middle value, or the mean of the two middle ones (SQL's percentile_cont(0.5)). */
    private fun median(values: List<Long>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle].toDouble() else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    private fun roundHalfUp(cents: Double): Long = floor(cents + 0.5).toLong()

    private const val EXPENSE = "EXPENSE"
    private const val INCOME = "INCOME"
}
