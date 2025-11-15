package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.dtos.summary.CategorySummary
import com.xavierclavel.dtos.summary.DaySummary
import com.xavierclavel.dtos.summary.MonthSummary
import com.xavierclavel.dtos.summary.YearSummary
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.Expense
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QUser
import io.ebean.DB
import io.ebean.Paging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.time.LocalDate

class SummaryService: KoinComponent {
    val configuration: Configuration by inject()


    fun summaryOfDay(userId: Long, year: Int, month: Int, day: Int): DaySummary {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusDays(1)
        val (total, byCategory) =  summary(userId, start, end)
        return DaySummary(
            year = year,
            month = month,
            day = day,
            totalExpenses = total,
            byCategory = byCategory,
        )
    }


    fun summaryOfMonth(userId: Long, year: Int, month: Int): MonthSummary {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusMonths(1)
        val (total, byCategory) =  summary(userId, start, end)
        return MonthSummary(
            year = year,
            month = month,
            totalExpenses = total,
            byCategory = byCategory,
        )
    }

    fun summaryOfYear(userId: Long, year: Int): YearSummary {
        val start = LocalDate.of(year, 1, 1)
        val end = start.plusYears(1)
        val (total, byCategory) =  summary(userId, start, end)
        return YearSummary(
            year = year,
            totalExpenses = total,
            byCategory = byCategory,
        )
    }

    fun summary(userId: Long, start: LocalDate, end: LocalDate): Pair<BigDecimal, List<CategorySummary>> {
        val total = QExpense()
            .select("sum(amount)")
            .user.id.eq(userId)
            .date.ge(start)
            .date.lt(end)
            .findSingleAttribute() ?: BigDecimal.ZERO

        val summaryByCategory = DB.findDto(
            CategorySummary::class.java,
            """
            select 
              e.${QExpense.Alias.category}_id as categoryId,
              c.${QCategory.Alias.name} as categoryName,
              sum(e.${QExpense.Alias.amount}) as total
            from expenses e
            left join categories c on c.id = e.${QExpense.Alias.category}_id
            where e.${QExpense.Alias.user}_id = :userId 
              and e.${QExpense.Alias.date} >= :start 
              and e.${QExpense.Alias.date} < :end
            group by e.${QExpense.Alias.category}_id, c.${QCategory.Alias.name}
            """
        )
            .setParameter("userId", userId)
            .setParameter("start", start)
            .setParameter("end", end)
            .findList()

        return Pair(total, summaryByCategory)
    }
}