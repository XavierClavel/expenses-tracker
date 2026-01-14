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
                    DATE_TRUNC('month', (SELECT MIN(date)::date FROM account_reports AS ar JOIN investment_accounts AS ia ON ar.account_id = ia.id WHERE ia.owner_id = :userId)),
                    DATE_TRUNC('month', (SELECT MAX(date)::date FROM account_reports AS ar JOIN investment_accounts AS ia ON ar.account_id = ia.id WHERE ia.owner_id = :userId)),
                    INTERVAL '1 month'
                )::date AS month
            ),
            accounts AS (
                SELECT id
                FROM investment_accounts
                WHERE owner_id = :userId
            ),
            account_months AS (
                SELECT
                    a.id AS account_id,
                    m.month
                FROM accounts a
                CROSS JOIN months m
            ),
            reports_by_month AS (
                SELECT
                    ar.account_id,
                    DATE_TRUNC('month', ar.date)::date AS month,
                    ar.amount,
                    ROW_NUMBER() OVER (
                        PARTITION BY ar.account_id, DATE_TRUNC('month', ar.date)
                        ORDER BY ar.date DESC
                    ) AS rn
                FROM account_reports ar
            ),
            monthly_latest AS (
                SELECT
                    account_id,
                    month,
                    amount
                FROM reports_by_month
                WHERE rn = 1
            ),
            account_balances AS (
                SELECT
                    am.account_id,
                    am.month,
                    COALESCE(
                        MAX(ml.amount) OVER (
                            PARTITION BY am.account_id
                            ORDER BY am.month
                            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
                        ),
                        0
                    ) AS balance
                FROM account_months am
                LEFT JOIN monthly_latest ml
                    ON ml.account_id = am.account_id
                   AND ml.month = am.month
            )
            SELECT
                EXTRACT(YEAR FROM month)  AS year,
                EXTRACT(MONTH FROM month) AS month,
                SUM(balance) AS balance
            FROM account_balances
            GROUP BY month
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .findList()
    }

}