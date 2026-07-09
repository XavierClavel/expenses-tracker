package com.xavierclavel.services

import com.xavierclavel.config.Configuration
import com.xavierclavel.dtos.investment.AccountTrendDto
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.dtos.investment.InvestmentAccountOut
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.exceptions.ForbiddenException
import com.xavierclavel.exceptions.NotFoundCause
import com.xavierclavel.exceptions.NotFoundException
import com.xavierclavel.enums.AccountTracking
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
     * has a prior year to measure against, over its time-weighted capital. Returns
     * (year, fraction) or null when it can't be computed.
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
            tracking = accountDto.tracking,
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
                tracking = accountDto.tracking
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

    // ── Trends ───────────────────────────────────────────────────────────────────
    //
    // Balance per period comes from the account-report snapshots (the SQL below).
    // Everything else — the contributions/interest split and the return — is derived
    // in Kotlin so it can respect each account's tracking mode.

    fun trendByAccountByMonth(userId: Long, accountId: Long): List<AccountTrendDto> =
        accountRich(userId, accountId, getById(accountId).tracking, monthly = true).toDtos()

    fun trendByAccountByYear(userId: Long, accountId: Long): List<AccountTrendDto> =
        accountRich(userId, accountId, getById(accountId).tracking, monthly = false).toDtos()

    fun trendByUserByMonth(userId: Long): List<AccountTrendDto> = aggregateUser(userId, monthly = true)

    fun trendByUserByYear(userId: Long): List<AccountTrendDto> = aggregateUser(userId, monthly = false)

    /** Aggregate every account's series onto a shared period axis (mode-aware). */
    private fun aggregateUser(userId: Long, monthly: Boolean): List<AccountTrendDto> {
        val accounts = QInvestmentAccount().owner.id.eq(userId).findList()
        val perAccount = accounts
            .map { accountRich(userId, it.id, it.tracking, monthly) }
            .filter { it.isNotEmpty() }
        if (perAccount.isEmpty()) return emptyList()

        // Full contiguous range from the earliest to the latest period across accounts.
        val minOrd = perAccount.minOf { it.first().key.ordinal }
        val maxOrd = perAccount.maxOf { it.last().key.ordinal }
        val keys = (minOrd..maxOrd).map { keyFromOrdinal(it, monthly) }
        val byOrd = perAccount.map { series -> series.associateBy { it.key.ordinal } }

        val aggregated = keys.map { key ->
            var balance = BigDecimal.ZERO
            var contributions = BigDecimal.ZERO
            var gain = BigDecimal.ZERO
            var weightedCapital = BigDecimal.ZERO
            var measured = false
            perAccount.forEachIndexed { a, series ->
                // Balance & contributions carry forward: an account still holds its money
                // (and principal) after its last report, and is absent (0) before its first.
                val carried = series.lastOrNull { it.key.ordinal <= key.ordinal }
                if (carried != null) {
                    balance += carried.balance
                    contributions += carried.contributions
                }
                // Return only counts periods an account was actually measured in.
                val exact = byOrd[a][key.ordinal]
                if (exact?.gain != null && exact.weightedCapital != null) {
                    gain += exact.gain
                    weightedCapital += exact.weightedCapital
                    measured = true
                }
            }
            Rich(key, balance, contributions, if (measured) gain else null, if (measured) weightedCapital else null)
        }
        return aggregated.toDtos()
    }

    private fun accountRich(userId: Long, accountId: Long, tracking: AccountTracking, monthly: Boolean): List<Rich> {
        val raw = if (monthly) accountRawMonth(userId, accountId) else accountRawYear(userId, accountId)
        return richSeries(raw, accountEvents(accountId), tracking, monthly)
    }

    // ── Per-account computation ───────────────────────────────────────────────────

    private data class PeriodKey(val year: Int, val month: Int?) {
        // A single monotonic index for a period, so periods can be compared/enumerated.
        val ordinal: Int get() = if (month != null) year * 12 + (month - 1) else year
    }

    private data class Event(val date: LocalDate, val amount: BigDecimal, val type: InvestmentType)

    private data class Rich(
        val key: PeriodKey,
        val balance: BigDecimal,
        val contributions: BigDecimal,
        val gain: BigDecimal?,            // interest earned this period (Modified/Simple Dietz numerator)
        val weightedCapital: BigDecimal?, // time-weighted average capital (denominator)
    )

    private fun Rich.toDto(prev: Rich?): AccountTrendDto {
        val change = if (prev != null) balance - prev.balance else null
        val proportional = if (prev != null && prev.balance.signum() != 0)
            change!!.divide(prev.balance, 6, RoundingMode.HALF_UP) else null
        val returnRate = if (gain != null && weightedCapital != null && weightedCapital.signum() != 0)
            gain.divide(weightedCapital, 6, RoundingMode.HALF_UP) else null
        return AccountTrendDto(
            year = key.year,
            month = key.month,
            balance = balance,
            change = change,
            proportionalChange = proportional,
            contributions = contributions,
            returnRate = returnRate,
        )
    }

    private fun List<Rich>.toDtos(): List<AccountTrendDto> = mapIndexed { i, r -> r.toDto(getOrNull(i - 1)) }

    private fun accountEvents(accountId: Long): List<Event> =
        QInvestment().account.id.eq(accountId).findList().map { Event(it.date, it.amount, it.type) }

    private val HALF = BigDecimal.valueOf(0.5)

    /**
     * Build the per-period series for one account. Contributions are authoritative or
     * derived depending on the tracking mode; the return is Modified Dietz (transfers
     * date-weighted) in CONTRIBUTIONS mode, and Simple Dietz (mid-period) in INTEREST
     * mode, where deposits aren't itemised.
     */
    private fun richSeries(
        raw: List<AccountTrendDto>,
        events: List<Event>,
        tracking: AccountTracking,
        monthly: Boolean,
    ): List<Rich> {
        val keys = raw.map { PeriodKey(it.year, it.month) }
        val bounds = keys.map { periodBounds(it, monthly) }
        val contributions = raw.indices.map { i ->
            when (tracking) {
                AccountTracking.CONTRIBUTIONS -> netContributionsUpTo(events, bounds[i].second)
                AccountTracking.INTEREST -> raw[i].balance - netInterestUpTo(events, bounds[i].second)
            }
        }
        return raw.indices.map { i ->
            val balance = raw[i].balance
            if (i == 0) {
                Rich(keys[i], balance, contributions[i], gain = null, weightedCapital = null)
            } else {
                val prevBalance = raw[i - 1].balance
                val deltaContrib = contributions[i] - contributions[i - 1]
                val gain = (balance - prevBalance) - deltaContrib
                val weightedCapital = when (tracking) {
                    AccountTracking.CONTRIBUTIONS ->
                        prevBalance + weightedContributionFlows(events, bounds[i].first, bounds[i].second)
                    AccountTracking.INTEREST ->
                        prevBalance + deltaContrib.multiply(HALF)
                }
                Rich(keys[i], balance, contributions[i], gain, weightedCapital)
            }
        }
    }

    private fun periodBounds(key: PeriodKey, monthly: Boolean): Pair<LocalDate, LocalDate> =
        if (monthly) {
            val start = LocalDate.of(key.year, key.month!!, 1)
            start to start.withDayOfMonth(start.lengthOfMonth())
        } else {
            LocalDate.of(key.year, 1, 1) to LocalDate.of(key.year, 12, 31)
        }

    private fun keyFromOrdinal(ordinal: Int, monthly: Boolean): PeriodKey =
        if (monthly) PeriodKey(ordinal / 12, ordinal % 12 + 1) else PeriodKey(ordinal, null)

    private fun netContributionsUpTo(events: List<Event>, end: LocalDate): BigDecimal =
        events.filter { it.date <= end }.fold(BigDecimal.ZERO) { acc, e ->
            when (e.type) {
                InvestmentType.IN -> acc + e.amount
                InvestmentType.OUT -> acc - e.amount
                else -> acc
            }
        }

    private fun netInterestUpTo(events: List<Event>, end: LocalDate): BigDecimal =
        events.filter { it.date <= end }.fold(BigDecimal.ZERO) { acc, e ->
            when (e.type) {
                InvestmentType.INTEREST -> acc + e.amount
                InvestmentType.FEE -> acc - e.amount
                else -> acc
            }
        }

    /** Σ of contribution flows in [start, end], each weighted by the fraction of the period it was invested. */
    private fun weightedContributionFlows(events: List<Event>, start: LocalDate, end: LocalDate): BigDecimal {
        val length = (ChronoUnit.DAYS.between(start, end) + 1).toDouble()
        return events
            .filter { (it.type == InvestmentType.IN || it.type == InvestmentType.OUT) && it.date >= start && it.date <= end }
            .fold(BigDecimal.ZERO) { acc, e ->
                val signed = if (e.type == InvestmentType.IN) e.amount else e.amount.negate()
                val offset = ChronoUnit.DAYS.between(start, e.date).toDouble()
                val weight = ((length - offset) / length).coerceIn(0.0, 1.0)
                acc + signed.multiply(BigDecimal.valueOf(weight))
            }
    }

    // ── Balance snapshots (SQL) ────────────────────────────────────────────────────
    // Contributions/returnRate are computed in Kotlin; the SQL only supplies the
    // carried-forward balance per period (columns nulled so findDto still maps them).

    private fun accountRawMonth(userId: Long, accountId: Long): List<AccountTrendDto> {
        return DB.findDto(
            AccountTrendDto::class.java,
            """
            WITH months AS (
                SELECT generate_series(
                    DATE_TRUNC('month', LEAST(
                        (SELECT MIN(date)::date FROM account_reports WHERE account_id = :accountId),
                        (SELECT MIN(date)::date FROM investments WHERE account_id = :accountId)
                    )),
                    DATE_TRUNC('month', GREATEST(
                        (SELECT MAX(date)::date FROM account_reports WHERE account_id = :accountId),
                        (SELECT MAX(date)::date FROM investments WHERE account_id = :accountId)
                    )),
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
                    COALESCE(MAX(amount) OVER (PARTITION BY grp), 0) AS balance
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
                NULL::numeric AS contributions,
                NULL::numeric AS returnRate
            FROM balances
            ORDER BY year, month;
            """
        )
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .findList()
    }

    private fun accountRawYear(userId: Long, accountId: Long): List<AccountTrendDto> {
        return DB.findDto(
            AccountTrendDto::class.java,
            """
            WITH years AS (
                SELECT generate_series(
                    DATE_TRUNC('year', LEAST(
                        (SELECT MIN(date)::date FROM account_reports WHERE account_id = :accountId),
                        (SELECT MIN(date)::date FROM investments WHERE account_id = :accountId)
                    )),
                    DATE_TRUNC('year', GREATEST(
                        (SELECT MAX(date)::date FROM account_reports WHERE account_id = :accountId),
                        (SELECT MAX(date)::date FROM investments WHERE account_id = :accountId)
                    )),
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
                    COALESCE(MAX(amount) OVER (PARTITION BY grp), 0) AS balance
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
                NULL::numeric AS contributions,
                NULL::numeric AS returnRate
            FROM balances
            ORDER BY year;
            """
        )
            .setParameter("userId", userId)
            .setParameter("accountId", accountId)
            .findList()
    }

}
