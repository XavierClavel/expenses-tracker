package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.summary.CategorySummary
import com.xavierclavel.dtos.summary.SummaryDto
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QSubcategory
import io.ebean.DB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.time.LocalDate

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
}