package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.TrendDto
import com.xavierclavel.dtos.summary.CategorySummary
import com.xavierclavel.dtos.summary.SummaryDto
import com.xavierclavel.models.query.QCategory
import com.xavierclavel.models.query.QExpense
import com.xavierclavel.models.query.QSubcategory
import io.ebean.DB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class TrendService: KoinComponent {
    val configuration: Configuration by inject()

    fun trendByYear(userId: Long): List<TrendDto> {
        return DB.findDto(
            TrendDto::class.java,
            """
            SELECT
                EXTRACT(YEAR FROM date)  AS year,
                SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS totalIncome,
                SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpenses
            FROM expenses
            WHERE user_id = :userId
            GROUP BY year
            ORDER BY year;
            """
        )
            .setParameter("userId", userId)
            .findList()
    }

    fun trendByMonth(userId: Long): List<TrendDto> {
        return DB.findDto(
            TrendDto::class.java,
            """
            SELECT
                EXTRACT(YEAR FROM date)  AS year,
                EXTRACT(MONTH FROM date) AS month,
                SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS totalIncome,
                SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpenses
            FROM expenses
            WHERE user_id = :userId
            GROUP BY year, month
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .findList()
    }
}