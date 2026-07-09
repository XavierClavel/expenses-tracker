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
        assertEquals(0, result.latestAnnualReturn!!.compareTo(BigDecimal("0.068")))
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
