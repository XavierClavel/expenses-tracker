package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.TrendDto
import com.xavierclavel.dtos.investment.AccountTrendDto
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.dtos.investment.InvestmentAccountOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.models.InvestmentAccount
import com.xavierclavel.models.query.QInvestmentAccount
import com.xavierclavel.models.query.QUser
import io.ebean.DB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AccountService: KoinComponent {
    val configuration: Configuration by inject()
    val userService: UserService by inject()

    private fun getById(id: Long): InvestmentAccount =
        QInvestmentAccount().id.eq(id).findOne() ?: throw NotFoundException(NotFoundCause.ACCOUNT_NOT_FOUND)

    /**
     * Throw exception if a user tries to modify an account he does not own
     */
    private fun InvestmentAccount.checkRights(userId: Long): InvestmentAccount {
        if (this.owner.id != userId) {
            throw ForbiddenException(ForbiddenCause.MUST_BE_OWNER)
        }
        return this
    }

    fun get(userId: Long, accountId: Long): InvestmentAccountOut =
        getById(accountId)
            .checkRights(userId)
            .toOutput()

    fun list(userId: Long): List<InvestmentAccountOut> {
        return QInvestmentAccount()
            .owner.id.eq(userId)
            .findList()
            .map { it.toOutput() }
    }


    fun create(accountDto: InvestmentAccountIn, userId: Long): InvestmentAccountOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val account = InvestmentAccount(
            name = accountDto.name,
            owner = user,
        )
        account.insert()

        return account.toOutput()
    }

    fun update(userId: Long, accountId: Long, accountDto: InvestmentAccountIn): InvestmentAccountOut =
        getById(accountId)
            .checkRights(userId)
            .apply {
                name = accountDto.name
            }
            .apply { this.update() }
            .toOutput()

    fun delete(userId: Long, accountId: Long) {
        val result = getById(accountId)
            .checkRights(userId)
            .delete()
        if (!result) {
            throw Exception("Failed to delete account $accountId")
        }
    }

    fun trendByAccountByMonth(userId: Long, accountId: Long): List<AccountTrendDto> {
        return DB.findDto(
            AccountTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', (SELECT MIN(date)::date FROM account_reports WHERE account_id = :accountId)),
                    DATE_TRUNC('month', (SELECT MAX(date)::date FROM account_reports WHERE account_id = :accountId)),
                    INTERVAL '1 month'
                )::date AS month
            ),
            monthly_latest AS (
                SELECT
                    DATE_TRUNC('month', ar.date)::date AS month,
                    ar.amount,
                    ROW_NUMBER() OVER (
                        PARTITION BY DATE_TRUNC('month', ar.date)
                        ORDER BY ar.date DESC
                    ) AS rn
                FROM account_reports ar
                JOIN investment_accounts ia ON ar.account_id = ia.id
                WHERE ia.owner_id = :userId
                  AND ia.id = :accountId
            ),
            month_values AS (
                SELECT
                    m.month,
                    ml.amount
                FROM months m
                LEFT JOIN monthly_latest ml
                    ON ml.month = m.month
                   AND ml.rn = 1
            )
            SELECT
                EXTRACT(YEAR FROM month)  AS year,
                EXTRACT(MONTH FROM month) AS month,
                MAX(amount) OVER (
                    ORDER BY month
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                ) AS balance
            FROM month_values
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .findList()
    }

    fun trendByAccountByYear(userId: Long, accountId: Long): List<AccountTrendDto> {
        return DB.findDto(
            AccountTrendDto::class.java,
            """
            WITH years AS (
                SELECT generate_series(
                    DATE_TRUNC('year', (SELECT MIN(date)::date FROM account_reports WHERE account_id = :accountId)),
                    DATE_TRUNC('year', (SELECT MAX(date)::date FROM account_reports WHERE account_id = :accountId)),
                    INTERVAL '1 year'
                )::date AS year
            ),
            yearly_latest AS (
                SELECT
                    DATE_TRUNC('year', ar.date)::date AS year,
                    ar.amount,
                    ROW_NUMBER() OVER (
                        PARTITION BY DATE_TRUNC('year', ar.date)
                        ORDER BY ar.date DESC
                    ) AS rn
                FROM account_reports ar
                JOIN investment_accounts ia ON ar.account_id = ia.id
                WHERE ia.owner_id = :userId
                  AND ia.id = :accountId
            ),
            year_values AS (
                SELECT
                    m.year,
                    ml.amount
                FROM years m
                LEFT JOIN yearly_latest ml
                    ON ml.year = m.year
                   AND ml.rn = 1
            )
            SELECT
                EXTRACT(YEAR FROM year)  AS year,
                MAX(amount) OVER (
                    ORDER BY year
                    ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                ) AS balance
            FROM year_values
            ORDER BY year;
            """
        )
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .findList()
    }

    fun trendByUserByMonth(userId: Long): List<AccountTrendDto> {
        return DB.findDto(
            AccountTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', (SELECT MIN(date)::date FROM account_reports)),
                    DATE_TRUNC('month', (SELECT MAX(date)::date FROM account_reports)),
                    INTERVAL '1 month'
                )::date AS month
            ),
            monthly_account_totals AS (
                SELECT
                    ia.id AS id,
                    DATE_TRUNC('month', date::date)::date AS month,
                    MAX(amount) AS account_balance
                FROM account_reports AS ar
                JOIN investment_accounts AS ia
                ON ar.account_id = ia.id
                WHERE ia.owner_id = :userId
                GROUP BY DATE_TRUNC('month', date::date), ia.id
            ),
            monthly_totals AS (
                SELECT
                    month AS MONTH,
                    SUM(account_balance) AS balance
                FROM monthly_account_totals
                GROUP BY month
            )
            SELECT
                EXTRACT(YEAR FROM m.month)  AS year,
                EXTRACT(MONTH FROM m.month) AS month,
                COALESCE(mt.balance, 0) AS balance
            FROM months m
            LEFT JOIN monthly_totals mt USING (month)
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .findList()
    }

}