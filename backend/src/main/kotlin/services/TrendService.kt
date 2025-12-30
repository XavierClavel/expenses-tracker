package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.TrendDto
import com.xavierclavel.dtos.YearTrendDto
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

    fun categoryTrendByYear(userId: Long, categoryId: Long): List<YearTrendDto> {
        return DB.findDto(
            YearTrendDto::class.java,
            """
            $MONTHS_SERIES
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', date::date)::date AS month_date,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.parent_category_id = :categoryId
                GROUP BY DATE_TRUNC('month', date::date)
            ),
            $OUTPUT
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
                    DATE_TRUNC('month', (SELECT MIN(date)::date FROM expenses)),
                    DATE_TRUNC('month', (SELECT MAX(date)::date FROM expenses)),
                    INTERVAL '1 month'
                )::date AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', e.date::date)::date AS month,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.parent_category_id = :categoryId
                GROUP BY DATE_TRUNC('month', date::date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                EXTRACT(MONTH FROM m.month) AS month,
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

    fun subcategoryTrendByYear(userId: Long, categoryId: Long): List<YearTrendDto> {
        return DB.findDto(
            YearTrendDto::class.java,
            """
            $MONTHS_SERIES
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', date::date)::date AS month_date,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.id = :categoryId
                GROUP BY DATE_TRUNC('month', date::date)
            ),
            $OUTPUT
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
                    DATE_TRUNC('month', (SELECT MIN(date)::date FROM expenses)),
                    DATE_TRUNC('month', (SELECT MAX(date)::date FROM expenses)),
                    INTERVAL '1 month'
                )::date AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', e.date::date)::date AS month,
                    SUM(e.amount) AS total
                FROM expenses AS e
                JOIN subcategories AS s
                ON e.category_id = s.id
                WHERE e.user_id = :userId
                AND s.id = :categoryId
                GROUP BY DATE_TRUNC('month', date::date)
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                EXTRACT(MONTH FROM m.month) AS month,
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

    fun trendByYear(userId: Long): List<TrendDto> {
        return DB.findDto(
            TrendDto::class.java,
            """
            WITH years AS (
                SELECT generate_series(
                    DATE_TRUNC('year', (SELECT MIN(date)::date FROM expenses)),
                    DATE_TRUNC('year', (SELECT MAX(date)::date FROM expenses)),
                    INTERVAL '1 year'
                )::date AS year_date
            ),
            yearly_totals AS (
                SELECT
                    DATE_TRUNC('year', date::date)::date AS year_date,
                    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS totalIncome,
                    SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpenses
                FROM expenses
                WHERE user_id = :userId
                GROUP BY DATE_TRUNC('year', date::date)
            )
            SELECT
                EXTRACT(YEAR FROM y.year_date) AS year,
                COALESCE(t.totalIncome, 0)     AS totalIncome,
                COALESCE(t.totalExpenses, 0)   AS totalExpenses
            FROM years y
            LEFT JOIN yearly_totals t USING (year_date)
            ORDER BY year;
            """
        )
            .setParameter("userId", userId)
            .findList()
    }

    fun medianByYear(userId: Long): List<TrendDto> {
        return DB.findDto(
            TrendDto::class.java,
            """
            $MONTHS_SERIES
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', date::date)::date AS month_date,
                    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS totalIncome,
                    SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpenses
                FROM expenses
                WHERE user_id = :userId
                GROUP BY DATE_TRUNC('month', date::date)
            ),
            months_with_totals AS (
                SELECT
                    m.month_date,
                    COALESCE(mt.totalIncome, 0)  AS totalIncome,
                    COALESCE(mt.totalExpenses, 0) AS totalExpenses
                FROM months m
                LEFT JOIN monthly_totals mt USING (month_date)
            )
            SELECT
                EXTRACT(YEAR FROM month_date) AS year,
            
                percentile_cont(0.5)
                    WITHIN GROUP (ORDER BY totalIncome)
                    AS medianMonthlyIncome,
            
                percentile_cont(0.5)
                    WITHIN GROUP (ORDER BY totalExpenses)
                    AS medianMonthlyExpenses
            
            FROM months_with_totals
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
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', (SELECT MIN(date)::date FROM expenses)),
                    DATE_TRUNC('month', (SELECT MAX(date)::date FROM expenses)),
                    INTERVAL '1 month'
                )::date AS month
            ),
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', date::date)::date AS month,
                    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS totalIncome,
                    SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpenses
                FROM expenses
                WHERE user_id = :userId
                GROUP BY DATE_TRUNC('month', date::date)
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

    fun flowByYear(userId: Long): List<YearTrendDto> {
        return DB.findDto(
            YearTrendDto::class.java,
            """
            $MONTHS_SERIES
            monthly_totals AS (
                SELECT
                    DATE_TRUNC('month', date::date)::date AS month_date,
                    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE -amount END) AS total
                FROM expenses
                WHERE user_id = :userId
                GROUP BY DATE_TRUNC('month', date::date)
            ),
            $OUTPUT
            """
        )
            .setParameter("userId", userId)
            .findList()
    }

    private val MONTHS_SERIES = """
        WITH months AS (
            SELECT generate_series(
                DATE_TRUNC('month', (SELECT MIN(date)::date FROM expenses WHERE user_id = :userId)),
                DATE_TRUNC('month', (SELECT MAX(date)::date FROM expenses WHERE user_id = :userId)),
                INTERVAL '1 month'
            )::date AS month_date
        ),""".trimIndent()

    private val OUTPUT = """
        months_with_totals AS (
                SELECT
                    m.month_date,
                    COALESCE(mt.total, 0)  AS total
                FROM months m
                LEFT JOIN monthly_totals mt USING (month_date)
            )
            SELECT
                EXTRACT(YEAR FROM month_date) AS year,
                    
                SUM(total) as total,
                
                percentile_cont(0.5)
                    WITHIN GROUP (ORDER BY total)
                    AS median,
                
                AVG(total) as average
            
            FROM months_with_totals
            GROUP BY year
            ORDER BY year;
    """.trimIndent()
}