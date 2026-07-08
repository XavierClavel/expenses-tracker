package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.investment.AccountReportIn
import com.xavierclavel.dtos.investment.AccountTrendDto
import com.xavierclavel.dtos.investment.InvestmentIn
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.enums.InvestmentType
import com.xavierclavel.utils.createAccount
import com.xavierclavel.utils.createAccountReport
import com.xavierclavel.utils.createInvestment
import com.xavierclavel.utils.getAccount
import com.xavierclavel.utils.getAccountMonthTrendsReport
import com.xavierclavel.utils.getAccountYearTrendsReport
import com.xavierclavel.utils.getUserAccountsMonthTrendsReport
import com.xavierclavel.utils.getUserAccountsYearTrendsReport
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies the transfer/interest calculations: `contributions` is the cumulative
 * net of deposits (IN) minus withdrawals (OUT), and the accrued interest the app
 * displays is always `balance − contributions`.
 */
class AccountInterestTest: ApplicationTest() {

    private val accountDto = InvestmentAccountIn(name = "PEA")

    private fun deposit(accountId: Long, amount: String, date: String) =
        InvestmentIn(BigDecimal(amount), accountId, InvestmentType.IN, LocalDate.parse(date))

    private fun withdrawal(accountId: Long, amount: String, date: String) =
        InvestmentIn(BigDecimal(amount), accountId, InvestmentType.OUT, LocalDate.parse(date))

    private fun report(amount: String, date: String) =
        AccountReportIn(BigDecimal(amount), LocalDate.parse(date))

    private fun AccountTrendDto.assertBalance(expected: String) =
        assertEquals(0, balance.compareTo(BigDecimal(expected)), "balance mismatch: $this")

    private fun AccountTrendDto.assertContributions(expected: String) =
        assertEquals(0, contributions!!.compareTo(BigDecimal(expected)), "contributions mismatch: $this")

    // ── Account-level aggregate (used by the distribution & account header) ──────

    @Test
    fun `account contributions is net of deposits and withdrawals`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createInvestment(account.id, deposit(account.id, "100", "2021-01-01"))
        client.createInvestment(account.id, deposit(account.id, "50", "2021-02-01"))
        client.createInvestment(account.id, withdrawal(account.id, "30", "2021-03-01"))

        val result = client.getAccount(account.id)
        assertEquals(0, result.contributions.compareTo(BigDecimal("120")))
    }

    @Test
    fun `account with no transfers has zero contributions`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createAccountReport(account.id, report("200", "2021-01-01"))

        val result = client.getAccount(account.id)
        assertEquals(0, result.amount.compareTo(BigDecimal("200")))
        assertEquals(0, result.contributions.compareTo(BigDecimal("0")))
    }

    @Test
    fun `interest is balance minus contributions`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createInvestment(account.id, deposit(account.id, "100", "2021-01-01"))
        client.createInvestment(account.id, deposit(account.id, "50", "2021-02-01"))
        client.createAccountReport(account.id, report("200", "2021-03-01"))

        val result = client.getAccount(account.id)
        // balance 200, put in 150 -> 50 of interest
        assertEquals(0, result.amount.compareTo(BigDecimal("200")))
        assertEquals(0, result.contributions.compareTo(BigDecimal("150")))
        assertEquals(0, result.amount.minus(result.contributions).compareTo(BigDecimal("50")))
    }

    // ── Per-account trend contributions ──────────────────────────────────────────

    @Test
    fun `account month trends carry cumulative contributions`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createAccountReport(account.id, report("100", "2021-01-01"))
        client.createAccountReport(account.id, report("250", "2021-03-01"))
        client.createInvestment(account.id, deposit(account.id, "100", "2021-01-15"))
        client.createInvestment(account.id, deposit(account.id, "100", "2021-03-10"))

        val result = client.getAccountMonthTrendsReport(account.id)
        assertEquals(3, result.size)
        result.find { it.year == 2021 && it.month == 1 }!!.apply { assertBalance("100"); assertContributions("100") }
        result.find { it.year == 2021 && it.month == 2 }!!.apply { assertBalance("100"); assertContributions("100") }
        result.find { it.year == 2021 && it.month == 3 }!!.apply { assertBalance("250"); assertContributions("200") }
    }

    @Test
    fun `account year trends carry cumulative contributions`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createAccountReport(account.id, report("100", "2021-01-01"))
        client.createAccountReport(account.id, report("300", "2022-06-01"))
        client.createInvestment(account.id, deposit(account.id, "100", "2021-05-01"))
        client.createInvestment(account.id, deposit(account.id, "150", "2022-02-01"))

        val result = client.getAccountYearTrendsReport(account.id)
        assertEquals(2, result.size)
        result.find { it.year == 2021 }!!.apply { assertBalance("100"); assertContributions("100") }
        result.find { it.year == 2022 }!!.apply { assertBalance("300"); assertContributions("250") }
    }

    @Test
    fun `withdrawals reduce contributions in trends`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createInvestment(account.id, deposit(account.id, "200", "2021-01-01"))
        client.createInvestment(account.id, withdrawal(account.id, "50", "2021-06-01"))
        client.createAccountReport(account.id, report("250", "2021-12-01"))

        val result = client.getAccountYearTrendsReport(account.id)
        assertEquals(1, result.size)
        // balance 250, net contribution 150 -> 100 of interest
        result.find { it.year == 2021 }!!.apply { assertBalance("250"); assertContributions("150") }
    }

    // ── User-level (all accounts) trend contributions ────────────────────────────

    @Test
    fun `user month trends sum contributions across accounts`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val account2 = client.createAccount(accountDto.copy(name = "PEG"))
        client.createAccountReport(account.id, report("100", "2021-01-01"))
        client.createInvestment(account.id, deposit(account.id, "100", "2021-01-01"))
        client.createAccountReport(account2.id, report("50", "2021-02-01"))
        client.createInvestment(account2.id, deposit(account2.id, "40", "2021-02-01"))

        val result = client.getUserAccountsMonthTrendsReport()
        assertEquals(2, result.size)
        result.find { it.year == 2021 && it.month == 1 }!!.apply { assertBalance("100"); assertContributions("100") }
        result.find { it.year == 2021 && it.month == 2 }!!.apply { assertBalance("150"); assertContributions("140") }
    }

    @Test
    fun `user year trends sum contributions across accounts`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val account2 = client.createAccount(accountDto.copy(name = "PEG"))
        client.createAccountReport(account.id, report("100", "2021-01-01"))
        client.createInvestment(account.id, deposit(account.id, "100", "2021-01-01"))
        client.createAccountReport(account2.id, report("200", "2022-03-01"))
        client.createInvestment(account2.id, deposit(account2.id, "150", "2022-03-01"))

        val result = client.getUserAccountsYearTrendsReport()
        assertEquals(2, result.size)
        result.find { it.year == 2021 }!!.apply { assertBalance("100"); assertContributions("100") }
        // acct1 balance carried to 2022 (100) + acct2 (200) = 300; contributions 100 + 150 = 250
        result.find { it.year == 2022 }!!.apply { assertBalance("300"); assertContributions("250") }
    }

    @Test
    fun `contributions ignore other users transfers`() = runTest {
        runAsUser2 {
            val other = client.createAccount(accountDto.copy(name = "OTHER"))
            client.createInvestment(other.id, deposit(other.id, "999", "2021-01-01"))
        }
        runAsUser1 {
            val account = client.createAccount(accountDto)
            client.createInvestment(account.id, deposit(account.id, "100", "2021-01-01"))
            client.createAccountReport(account.id, report("120", "2021-06-01"))

            val result = client.getAccountYearTrendsReport(account.id)
            assertEquals(1, result.size)
            result.find { it.year == 2021 }!!.apply { assertBalance("120"); assertContributions("100") }
            assertEquals(0, client.getAccount(account.id).contributions.compareTo(BigDecimal("100")))
        }
    }
}
