package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.TrendDto
import com.xavierclavel.dtos.dtos.CategoryTrendDto
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

    fun categoryTrendByYear(userId: Long, categoryId: Long): List<CategoryTrendDto> {
        return DB.findDto(
            CategoryTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('year', (SELECT MIN(date) FROM expenses)),
                    DATE_TRUNC('year', (SELECT MAX(date) FROM expenses)),
                    INTERVAL '1 year'
                ) AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('year', e.date) AS month,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.parent_category_id = :categoryId
                GROUP BY DATE_TRUNC('year', date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                COALESCE(mt.total, 0) AS total
            FROM months m
            LEFT JOIN monthly_totals mt USING (month)
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .setParameter("categoryId", categoryId)
            .findList()
    }

    fun categoryTrendByMonth(userId: Long, categoryId: Long): List<CategoryTrendDto> {
        return DB.findDto(
            CategoryTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', (SELECT MIN(date) FROM expenses)),
                    DATE_TRUNC('month', (SELECT MAX(date) FROM expenses)),
                    INTERVAL '1 month'
                ) AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', e.date) AS month,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.parent_category_id = :categoryId
                GROUP BY DATE_TRUNC('month', date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                EXTRACT(MONTH FROM m.month) AS month,
                COALESCE(mt.total, 0)       AS total
            FROM months m
            LEFT JOIN monthly_totals mt USING (month)
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .setParameter("categoryId", categoryId)
            .findList()
    }

    fun subcategoryTrendByYear(userId: Long, categoryId: Long): List<CategoryTrendDto> {
        return DB.findDto(
            CategoryTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('year', (SELECT MIN(date) FROM expenses)),
                    DATE_TRUNC('year', (SELECT MAX(date) FROM expenses)),
                    INTERVAL '1 year'
                ) AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('year', e.date) AS month,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.id = :categoryId
                GROUP BY DATE_TRUNC('year', date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                COALESCE(mt.total, 0) AS total
            FROM months m
            LEFT JOIN monthly_totals mt USING (month)
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .setParameter("categoryId", categoryId)
            .findList()
    }

    fun subcategoryTrendByMonth(userId: Long, categoryId: Long): List<CategoryTrendDto> {
        return DB.findDto(
            CategoryTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', (SELECT MIN(date) FROM expenses)),
                    DATE_TRUNC('month', (SELECT MAX(date) FROM expenses)),
                    INTERVAL '1 month'
                ) AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', e.date) AS month,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.id = :categoryId
                GROUP BY DATE_TRUNC('month', date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                EXTRACT(MONTH FROM m.month) AS month,
                COALESCE(mt.total, 0)       AS total
            FROM months m
            LEFT JOIN monthly_totals mt USING (month)
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .setParameter("categoryId", categoryId)
            .findList()
    }

    fun trendByYear(userId: Long): List<TrendDto> {
        return DB.findDto(
            TrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('year', (SELECT MIN(date) FROM expenses)),
                    DATE_TRUNC('year', (SELECT MAX(date) FROM expenses)),
                    INTERVAL '1 year'
                ) AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('year', date) AS month,
                    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS totalIncome,
                    SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpenses
                FROM expenses
                GROUP BY DATE_TRUNC('year', date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                COALESCE(mt.totalIncome, 0) AS totalIncome,
                COALESCE(mt.totalExpenses, 0) AS totalExpenses
            FROM months m
            LEFT JOIN monthly_totals mt USING (month)
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .findList()
    }

    fun trendByMonth(userId: Long): List<TrendDto> {
        return DB.findDto(
            TrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', (SELECT MIN(date) FROM expenses)),
                    DATE_TRUNC('month', (SELECT MAX(date) FROM expenses)),
                    INTERVAL '1 month'
                ) AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', date) AS month,
                    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS totalIncome,
                    SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpenses
                FROM expenses
                GROUP BY DATE_TRUNC('month', date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                EXTRACT(MONTH FROM m.month) AS month,
                COALESCE(mt.totalIncome, 0) AS totalIncome,
                COALESCE(mt.totalExpenses, 0) AS totalExpenses
            FROM months m
            LEFT JOIN monthly_totals mt USING (month)
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .findList()
    }
}