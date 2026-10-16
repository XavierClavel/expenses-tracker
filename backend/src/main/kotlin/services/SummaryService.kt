package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.summary.CategorySummary
import com.xavierclavel.dtos.summary.SubcategoryOutlier
import com.xavierclavel.dtos.summary.SummaryDto
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QSubcategory
import io.ebean.DB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class SummaryService: KoinComponent {
    val configuration: Configuration by inject()


    fun summaryOfDay(userId: Long, year: Int, month: Int, day: Int): SummaryDto {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusDays(1)
        return summary(userId, start, end).copy(
            year = year,
            month = month,
            day = day,
        )
    }


    fun summaryOfMonth(userId: Long, year: Int, month: Int): SummaryDto {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusMonths(1)
        return summary(userId, start, end).copy(
            year = year,
            month = month,
        )
    }

    fun summaryOfYear(userId: Long, year: Int): SummaryDto {
        val start = LocalDate.of(year, 1, 1)
        val end = start.plusYears(1)
        return summary(userId, start, end).copy(
            year = year,
        )
    }

    fun summary(userId: Long, start: LocalDate, end: LocalDate): SummaryDto {
        val totalExpenses = QExpense()
            .select("sum(amount)")
            .user.id.eq(userId)
            .type.eq(ExpenseType.EXPENSE)
            .date.ge(start)
            .date.lt(end)
            .findSingleAttribute() ?: BigDecimal.ZERO

        val totalIncome = QExpense()
            .select("sum(amount)")
            .user.id.eq(userId)
            .type.eq(ExpenseType.INCOME)
            .date.ge(start)
            .date.lt(end)
            .findSingleAttribute() ?: BigDecimal.ZERO

        val expensesByCategory = DB.findDto(
            CategorySummary::class.java,
            """
            select 
              e.${QExpense.Alias.category}_id as categoryId,
              c.${QSubcategory.Alias.name} as categoryName,
              sum(e.${QExpense.Alias.amount}) as total
            from expenses e
            left join subcategories c on c.id = e.${QExpense.Alias.category}_id
            where e.${QExpense.Alias.user}_id = :userId 
              and e.${QExpense.Alias.date} >= :start 
              and e.${QExpense.Alias.date} < :end
              and e.type = 'EXPENSE'
            group by e.${QExpense.Alias.category}_id, c.${QCategory.Alias.name}
            """
        )
            .setParameter("userId", userId)
            .setParameter("start", start)
            .setParameter("end", end)
            .findList()

        val incomeByCategory = DB.findDto(
            CategorySummary::class.java,
            """
            select 
              e.${QExpense.Alias.category}_id as categoryId,
              c.${QSubcategory.Alias.name} as categoryName,
              sum(e.${QExpense.Alias.amount}) as total
            from expenses e
            left join subcategories c on c.id = e.${QExpense.Alias.category}_id
            where e.${QExpense.Alias.user}_id = :userId 
              and e.${QExpense.Alias.date} >= :start 
              and e.${QExpense.Alias.date} < :end
              and e.type = 'INCOME'
            group by e.${QExpense.Alias.category}_id, c.${QCategory.Alias.name}
            """
        )
            .setParameter("userId", userId)
            .setParameter("start", start)
            .setParameter("end", end)
            .findList()

        return SummaryDto(
            totalExpenses = totalExpenses,
            totalIncome = totalIncome,
            expensesByCategory = expensesByCategory,
            incomeByCategory = incomeByCategory,
        )
    }

    /**
     * The subcategories whose monthly pace over [year] is at least [threshold] above their
     * all-time monthly mean, most deviating first.
     *
     * Paces are compared per month rather than per year, so a year the history only
     * partly covers (the first one, or the one in progress) is judged fairly.
     */
    fun outliersOfYear(userId: Long, year: Int, threshold: Double): List<SubcategoryOutlier> {
        val firstDate: LocalDate = QExpense().select("min(date)").user.id.eq(userId).findSingleAttribute()
            ?: return emptyList()
        val lastDate: LocalDate = QExpense().select("max(date)").user.id.eq(userId).findSingleAttribute()
            ?: return emptyList()

        // The history ends with the user's last month on record, never past the current
        // one: future-dated entries would otherwise count months that haven't happened.
        val historyFirst = YearMonth.from(firstDate)
        val historyLast = minOf(YearMonth.from(lastDate), YearMonth.now())
        val yearFirst = maxOf(historyFirst, YearMonth.of(year, 1))
        val yearLast = minOf(historyLast, YearMonth.of(year, 12))
        if (yearFirst > yearLast) return emptyList()

        val historyMonths = ChronoUnit.MONTHS.between(historyFirst, historyLast) + 1
        val yearMonths = ChronoUnit.MONTHS.between(yearFirst, yearLast) + 1

        val rows = DB.findDto(
            SubcategoryTotals::class.java,
            """
            select
              s.id as subcategoryId,
              s.name as subcategoryName,
              e.type as type,
              sum(e.amount) filter (where e.date >= :yearStart and e.date < :yearEnd) as yearTotal,
              sum(e.amount) as total
            from expenses e
            join subcategories s on s.id = e.category_id
            where e.user_id = :userId
              and e.date >= :historyStart
              and e.date < :historyEnd
            group by s.id, s.name, e.type
            having sum(e.amount) filter (where e.date >= :yearStart and e.date < :yearEnd) > 0
            """
        )
            .setParameter("userId", userId)
            .setParameter("historyStart", historyFirst.atDay(1))
            .setParameter("historyEnd", historyLast.plusMonths(1).atDay(1))
            .setParameter("yearStart", yearFirst.atDay(1))
            .setParameter("yearEnd", yearLast.plusMonths(1).atDay(1))
            .findList()

        return rows
            .filter { it.total.signum() > 0 }
            .map { row ->
                SubcategoryOutlier(
                    subcategoryId = row.subcategoryId,
                    subcategoryName = row.subcategoryName,
                    type = ExpenseType.valueOf(row.type),
                    total = row.yearTotal,
                    mean = (row.total * BigDecimal(yearMonths)).divide(BigDecimal(historyMonths), 2, RoundingMode.HALF_UP),
                    deviation = (row.yearTotal.toDouble() * historyMonths) / (row.total.toDouble() * yearMonths) - 1,
                )
            }
            .filter { it.deviation >= threshold }
            .sortedByDescending { it.deviation }
    }
}

/** A subcategory's total over a year and over the user's whole history. */
data class SubcategoryTotals(
    val subcategoryId: Long,
    val subcategoryName: String,
    val type: String,
    val yearTotal: BigDecimal,
    val total: BigDecimal,
)