package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.investment.AccountReportIn
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.dtos.investment.InvestmentIn
import com.xavierclavel.enums.AccountTracking
import com.xavierclavel.enums.InvestmentType
import com.xavierclavel.utils.createAccount
import com.xavierclavel.utils.createAccountReport
import com.xavierclavel.utils.createInvestment
import com.xavierclavel.utils.getAccount
import com.xavierclavel.utils.getUserAccountsYearTrendsReport
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Interest-tracking accounts (e.g. Livret A): the user records the known interest/fees
 * and contributions are derived as `balance − interest`, the mirror of the default
 * contributions-tracking mode.
 */
class AccountTrackingTest: ApplicationTest() {

    private val livret = InvestmentAccountIn(name = "Livret A", tracking = AccountTracking.INTEREST)

    private fun deposit(accountId: Long, amount: String, date: String) =
        InvestmentIn(BigDecimal(amount), accountId, InvestmentType.IN, LocalDate.parse(date))
    private fun interest(accountId: Long, amount: String, date: String) =
        InvestmentIn(BigDecimal(amount), accountId, InvestmentType.INTEREST, LocalDate.parse(date))
    private fun fee(accountId: Long, amount: String, date: String) =
        InvestmentIn(BigDecimal(amount), accountId, InvestmentType.FEE, LocalDate.parse(date))
    private fun report(amount: String, date: String) =
        AccountReportIn(BigDecimal(amount), LocalDate.parse(date))

    @Test
    fun `interest mode derives contributions from balance minus interest`() = runTestAsUser {
        val account = client.createAccount(livret)
        client.createAccountReport(account.id, report("1080", "2021-12-31"))
        client.createInvestment(account.id, interest(account.id, "80", "2021-12-31"))

        val result = client.getAccount(account.id)
        assertEquals(0, result.amount.compareTo(BigDecimal("1080")))
        assertEquals(0, result.contributions.compareTo(BigDecimal("1000")))
    }

    @Test
    fun `fees count as negative interest`() = runTestAsUser {
        val account = client.createAccount(livret)
        client.createAccountReport(account.id, report("965", "2021-12-31"))
        client.createInvestment(account.id, fee(account.id, "35", "2021-06-01"))

        val result = client.getAccount(account.id)
        // contributions inferred = 965 − (−35) = 1000; interest = balance − contributions = −35
        assertEquals(0, result.contributions.compareTo(BigDecimal("1000")))
        assertEquals(0, result.amount.minus(result.contributions).compareTo(BigDecimal("-35")))
    }

    @Test
    fun `interest mode annual return uses the known interest`() = runTestAsUser {
        val account = client.createAccount(livret)
        client.createAccountReport(account.id, report("1000", "2021-12-31"))
        client.createInvestment(account.id, interest(account.id, "68", "2022-12-31"))
        client.createAccountReport(account.id, report("1068", "2022-12-31"))

        val result = client.getAccount(account.id)
        assertEquals(2022, result.latestAnnualReturnYear)
        // 68 interest over a ~1000 average balance ≈ 6.8%
        val ret = result.latestAnnualReturn!!.toDouble()
        assertTrue(ret in 0.067..0.069, "expected ~6.8% but was $ret")
    }

    @Test
    fun `interest mode shows the first year's return without a prior year`() = runTestAsUser {
        val account = client.createAccount(livret)
        client.createAccountReport(account.id, report("10000", "2024-01-15"))
        client.createInvestment(account.id, interest(account.id, "300", "2024-12-31"))

        // Single year, yet the recorded interest gives a known first-year return.
        val result = client.getAccount(account.id)
        assertEquals(2024, result.latestAnnualReturnYear)
        // 300 interest over a ~9600 average balance (held from mid-January) ≈ 3.1%
        val ret = result.latestAnnualReturn!!.toDouble()
        assertTrue(ret in 0.030..0.033, "expected ~3% annualized but was $ret")
    }

    @Test
    fun `interest return annualizes over average balance, not year-end`() = runTestAsUser {
        val account = client.createAccount(livret)
        // Balance sits at 10000 almost all year; a big deposit lands only at year-end,
        // so it earned nothing. Interest of ~300 reflects 3% on the 10000 held all year.
        client.createAccountReport(account.id, report("10000", "2024-01-01"))
        client.createAccountReport(account.id, report("30000", "2024-12-31"))
        client.createInvestment(account.id, interest(account.id, "300", "2024-12-31"))

        val result = client.getAccount(account.id)
        // Dividing by year-end balance (30000) would wrongly read ~1%; dividing by the
        // average balance (~10000) gives the true ~3% rate.
        val ret = result.latestAnnualReturn!!.toDouble()
        assertTrue(ret in 0.028..0.032, "expected ~3% but was $ret")
    }

    @Test
    fun `mixed-mode accounts aggregate correctly`() = runTestAsUser {
        val pea = client.createAccount(InvestmentAccountIn(name = "PEA")) // CONTRIBUTIONS (default)
        client.createInvestment(pea.id, deposit(pea.id, "1000", "2021-01-01"))
        client.createAccountReport(pea.id, report("1000", "2021-06-01"))

        val livretA = client.createAccount(livret) // INTEREST
        client.createAccountReport(livretA.id, report("520", "2021-06-01"))
        client.createInvestment(livretA.id, interest(livretA.id, "20", "2021-06-01"))

        val result = client.getUserAccountsYearTrendsReport()
        assertEquals(1, result.size)
        val y = result.find { it.year == 2021 }!!
        // balance 1000 + 520; contributions 1000 (PEA deposits) + 500 (520 − 20 interest)
        assertEquals(0, y.balance.compareTo(BigDecimal("1520")))
        assertEquals(0, y.contributions!!.compareTo(BigDecimal("1500")))
    }
}
