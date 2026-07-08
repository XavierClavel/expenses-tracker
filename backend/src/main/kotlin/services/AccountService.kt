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
import com.xavierclavel.enums.InvestmentType
import com.xavierclavel.models.InvestmentAccount
import com.xavierclavel.models.query.QInvestment
import com.xavierclavel.models.query.QInvestmentAccount
import com.xavierclavel.models.query.QUser
import io.ebean.DB
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
            .withAnnualReturn(userId, accountId)

    fun list(userId: Long): List<InvestmentAccountOut> {
        return QInvestmentAccount()
            .owner.id.eq(userId)
            .findList()
            .map { it.toOutput().withAnnualReturn(userId, it.id) }
    }

    /**
     * The most recent full-year return: interest earned during the latest year that
     * has a prior year to measure against, divided by that prior year's ending
     * balance. Returns (year, fraction) or null when it can't be computed.
     */
    fun latestAnnualReturn(userId: Long, accountId: Long): Pair<Int, BigDecimal>? {
        val last = trendByAccountByYear(userId, accountId).lastOrNull { it.returnRate != null } ?: return null
        return last.year to last.returnRate!!
    }

    private fun InvestmentAccountOut.withAnnualReturn(userId: Long, accountId: Long): InvestmentAccountOut {
        val (year, ret) = latestAnnualReturn(userId, accountId) ?: return this
        return copy(latestAnnualReturn = ret, latestAnnualReturnYear = year)
    }


    fun create(accountDto: InvestmentAccountIn, userId: Long): InvestmentAccountOut {
        val user = QUser().id.eq(userId).findOne() ?: throw NotFoundException(NotFoundCause.USER_NOT_FOUND)
        val account = InvestmentAccount(
            name = accountDto.name,
            type = accountDto.type,
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
                type = accountDto.type
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
            ),
            balances AS (
                SELECT
                    EXTRACT(YEAR FROM month)  AS year,
                    EXTRACT(MONTH FROM month) AS month,
                    MAX(amount) OVER (PARTITION BY grp) AS balance
                        FROM (
                            SELECT
                                month,
                                amount,
                                COUNT(amount) OVER (ORDER BY month) AS grp
                            FROM month_values
                        ) t
            )
            SELECT
                year AS year,
                month AS month,
                balance AS balance,
                balance - LAG(balance) OVER (ORDER BY year, month) AS change,
                (balance - LAG(balance) OVER (ORDER BY year, month))
                    / NULLIF(LAG(balance) OVER (ORDER BY year, month), 0) AS proportionalChange,
                COALESCE((
                    SELECT SUM(CASE WHEN i.type = 'IN' THEN i.amount ELSE -i.amount END)
                    FROM investments i
                    JOIN investment_accounts ia ON i.account_id = ia.id
                    WHERE ia.owner_id = :userId
                      AND ia.id = :accountId
                      AND DATE_TRUNC('month', i.date)::date <= make_date(balances.year::int, balances.month::int, 1)
                ), 0) AS contributions,
                NULL::numeric AS returnRate
            FROM balances
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .findList()
            .let { applyReturns(it, accountFlows(accountId), monthly = true) }
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
            ),
            balances AS (
                SELECT
                    EXTRACT(YEAR FROM year)  AS year,
                    MAX(amount) OVER (PARTITION BY grp) AS balance
                        FROM (
                            SELECT
                                year,
                                amount,
                                COUNT(amount) OVER (ORDER BY year) AS grp
                            FROM year_values
                        ) t
            )
            SELECT
                year AS year,
                balance AS balance,
                balance - LAG(balance) OVER (ORDER BY year) AS change,
                (balance - LAG(balance) OVER (ORDER BY year))
                    / NULLIF(LAG(balance) OVER (ORDER BY year), 0) AS proportionalChange,
                COALESCE((
                    SELECT SUM(CASE WHEN i.type = 'IN' THEN i.amount ELSE -i.amount END)
                    FROM investments i
                    JOIN investment_accounts ia ON i.account_id = ia.id
                    WHERE ia.owner_id = :userId
                      AND ia.id = :accountId
                      AND DATE_TRUNC('year', i.date)::date <= make_date(balances.year::int, 1, 1)
                ), 0) AS contributions,
                NULL::numeric AS returnRate
            FROM balances
            ORDER BY year;
            """
        )
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .findList()
            .let { applyReturns(it, accountFlows(accountId), monthly = false) }
    }

    fun trendByUserByMonth(userId: Long): List<AccountTrendDto> {
        return DB.findDto(
            AccountTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', (SELECT MIN(date)::date FROM account_reports ar JOIN investment_accounts ia ON ar.account_id = ia.id WHERE ia.owner_id = :userId)),
                    DATE_TRUNC('month', (SELECT MAX(date)::date FROM account_reports ar JOIN investment_accounts ia ON ar.account_id = ia.id WHERE ia.owner_id = :userId)),
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
            account_month_amounts AS (
                SELECT
                    am.account_id,
                    am.month,
                    ml.amount,
                    COUNT(ml.amount) OVER (
                        PARTITION BY am.account_id
                        ORDER BY am.month
                    ) AS grp
                FROM account_months am
                LEFT JOIN monthly_latest ml
                    ON ml.account_id = am.account_id
                   AND ml.month = am.month
            ),
            account_balances AS (
                SELECT
                    account_id,
                    month,
                    COALESCE(
                        MAX(amount) OVER (PARTITION BY account_id, grp),
                        0
                    ) AS balance
                FROM account_month_amounts
            ),
            
            balances AS (
                SELECT
                    EXTRACT(YEAR FROM month)  AS year,
                    EXTRACT(MONTH FROM month) AS month,
                    SUM(balance) AS balance
                FROM account_balances
                GROUP BY month
                ORDER BY year, month
            )
            SELECT
                year,
                month,
                balance,
                balance - LAG(balance) OVER (ORDER BY year, month) AS change,
                (balance - LAG(balance) OVER (ORDER BY year, month))
                    / NULLIF(LAG(balance) OVER (ORDER BY year, month), 0) AS proportionalChange,
                COALESCE((
                    SELECT SUM(CASE WHEN i.type = 'IN' THEN i.amount ELSE -i.amount END)
                    FROM investments i
                    JOIN investment_accounts ia ON i.account_id = ia.id
                    WHERE ia.owner_id = :userId
                      AND DATE_TRUNC('month', i.date)::date <= make_date(balances.year::int, balances.month::int, 1)
                ), 0) AS contributions,
                NULL::numeric AS returnRate
            FROM balances
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .findList()
            .let { applyReturns(it, userFlows(userId), monthly = true) }
    }

    fun trendByUserByYear(userId: Long): List<AccountTrendDto> {
        return DB.findDto(
            AccountTrendDto::class.java,
            """
            WITH years AS (
                SELECT generate_series(
                    DATE_TRUNC('year', (SELECT MIN(date)::date FROM account_reports AS ar JOIN investment_accounts AS ia ON ar.account_id = ia.id WHERE ia.owner_id = :userId)),
                    DATE_TRUNC('year', (SELECT MAX(date)::date FROM account_reports AS ar JOIN investment_accounts AS ia ON ar.account_id = ia.id WHERE ia.owner_id = :userId)),
                    INTERVAL '1 year'
                )::date AS year
            ),
            accounts AS (
                SELECT id
                FROM investment_accounts
                WHERE owner_id = :userId
            ),
            account_years AS (
                SELECT
                    a.id AS account_id,
                    m.year
                FROM accounts a
                CROSS JOIN years m
            ),
            reports_by_year AS (
                SELECT
                    ar.account_id,
                    DATE_TRUNC('year', ar.date)::date AS year,
                    ar.amount,
                    ROW_NUMBER() OVER (
                        PARTITION BY ar.account_id, DATE_TRUNC('year', ar.date)
                        ORDER BY ar.date DESC
                    ) AS rn
                FROM account_reports ar
            ),
            yearly_latest AS (
                SELECT
                    account_id,
                    year,
                    amount
                FROM reports_by_year
                WHERE rn = 1
            ),
            account_year_amounts AS (
                SELECT
                    ay.account_id,
                    ay.year,
                    yl.amount,
                    COUNT(yl.amount) OVER (
                        PARTITION BY ay.account_id
                        ORDER BY ay.year
                    ) AS grp
                FROM account_years ay
                LEFT JOIN yearly_latest yl
                    ON yl.account_id = ay.account_id
                   AND yl.year = ay.year
            ),
            account_balances AS (
                SELECT
                    account_id,
                    year,
                    COALESCE(
                        MAX(amount) OVER (PARTITION BY account_id, grp),
                        0
                    ) AS balance
                FROM account_year_amounts
            ),
            balances AS (
                SELECT
                    EXTRACT(YEAR FROM year)  AS year,
                    SUM(balance) AS balance
                FROM account_balances
                GROUP BY year
                ORDER BY year
            )
            SELECT
                year AS year,
                balance AS balance,
                balance - LAG(balance) OVER (ORDER BY year) AS change,
                (balance - LAG(balance) OVER (ORDER BY year))
                    / NULLIF(LAG(balance) OVER (ORDER BY year), 0) AS proportionalChange,
                COALESCE((
                    SELECT SUM(CASE WHEN i.type = 'IN' THEN i.amount ELSE -i.amount END)
                    FROM investments i
                    JOIN investment_accounts ia ON i.account_id = ia.id
                    WHERE ia.owner_id = :userId
                      AND DATE_TRUNC('year', i.date)::date <= make_date(balances.year::int, 1, 1)
                ), 0) AS contributions,
                NULL::numeric AS returnRate
            FROM balances
            ORDER BY year;
            """
        )
            .setParameter("userId", userId)
            .findList()
            .let { applyReturns(it, userFlows(userId), monthly = false) }
    }

    // ── Returns (Modified Dietz) ─────────────────────────────────────────────────

    private data class Flow(val date: LocalDate, val amount: BigDecimal)

    private fun accountFlows(accountId: Long): List<Flow> =
        QInvestment().account.id.eq(accountId).findList()
            .map { Flow(it.date, if (it.type == InvestmentType.IN) it.amount else it.amount.negate()) }

    private fun userFlows(userId: Long): List<Flow> =
        QInvestment().account.owner.id.eq(userId).findList()
            .map { Flow(it.date, if (it.type == InvestmentType.IN) it.amount else it.amount.negate()) }

    /**
     * Fills in each period's Modified-Dietz return: interest earned over the period
     * divided by the time-weighted average capital, so a transfer is credited only
     * for the fraction of the period it was actually invested. The first period has
     * no prior balance to measure against and is left null.
     */
    private fun applyReturns(trends: List<AccountTrendDto>, flows: List<Flow>, monthly: Boolean): List<AccountTrendDto> {
        return trends.mapIndexed { i, t ->
            if (i == 0) return@mapIndexed t
            val vStart = trends[i - 1].balance
            val periodStart: LocalDate
            val periodEnd: LocalDate
            if (monthly) {
                val month = t.month ?: return@mapIndexed t
                periodStart = LocalDate.of(t.year, month, 1)
                periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
            } else {
                periodStart = LocalDate.of(t.year, 1, 1)
                periodEnd = LocalDate.of(t.year, 12, 31)
            }
            val length = (ChronoUnit.DAYS.between(periodStart, periodEnd) + 1).toDouble()
            var netFlow = BigDecimal.ZERO
            var weighted = BigDecimal.ZERO
            flows.forEach { flow ->
                if (flow.date < periodStart || flow.date > periodEnd) return@forEach
                netFlow += flow.amount
                val offset = ChronoUnit.DAYS.between(periodStart, flow.date).toDouble()
                val weight = ((length - offset) / length).coerceIn(0.0, 1.0)
                weighted += flow.amount.multiply(BigDecimal.valueOf(weight))
            }
            val gain = (t.balance - vStart) - netFlow
            val denominator = vStart + weighted
            val returnRate = if (denominator.signum() != 0) gain.divide(denominator, 6, RoundingMode.HALF_UP) else null
            t.copy(returnRate = returnRate)
        }
    }

}