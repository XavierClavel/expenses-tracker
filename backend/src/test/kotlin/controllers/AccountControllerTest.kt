package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.investment.AccountReportIn
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.utils.createAccount
import com.xavierclavel.utils.createAccountReport
import com.xavierclavel.utils.getAccount
import com.xavierclavel.utils.getAccountTrendsReport
import com.xavierclavel.utils.getUserAccountsTrendsReport
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountControllerTest: ApplicationTest() {

    val accountDto = InvestmentAccountIn(
        name = "PEA"
    )

    @Test
    fun `create new investment account`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val result = client.getAccount(account.id)
        assertEquals(accountDto.name, result.name)
    }

    @Test
    fun `create account report`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("15"), LocalDate.parse("2021-01-01")))
        val result = client.getAccount(account.id)
        assertEquals(0, result.amount.compareTo(BigDecimal("15")))
    }

    @Test
    fun `account month trends`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val account2 = client.createAccount(accountDto.copy(name = "PEG"))
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("15"), LocalDate.parse("2021-01-01")))
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("20"), LocalDate.parse("2021-02-01")))
        client.createAccountReport(account2.id, AccountReportIn(BigDecimal("30"), LocalDate.parse("2021-02-01")))
        val result = client.getAccountTrendsReport(account.id)
        println(result)
        assertEquals(2, result.size)
        assertEquals(0, result[0].balance.compareTo(BigDecimal("15")))
        assertEquals(0, result[1].balance.compareTo(BigDecimal("20")))

        val result2 = client.getAccountTrendsReport(account2.id)
        assertEquals(1, result2.size)
        assertEquals(0, result2[0].balance.compareTo(BigDecimal("30")))
    }

    @Test
    fun `account month trends are extrapolated`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("15"), LocalDate.parse("2021-01-01")))
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("20"), LocalDate.parse("2021-03-01")))
        val result = client.getAccountTrendsReport(account.id)
        println(result)
        assertEquals(3, result.size)
        assertEquals(0, result[0].balance.compareTo(BigDecimal("15")))
        assertEquals(0, result[1].balance.compareTo(BigDecimal("15")))
        assertEquals(0, result[2].balance.compareTo(BigDecimal("20")))
    }

    @Test
    fun `account year trends`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val account2 = client.createAccount(accountDto.copy(name = "PEG"))
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("15"), LocalDate.parse("2021-01-01")))
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("20"), LocalDate.parse("2021-02-01")))
        client.createAccountReport(account2.id, AccountReportIn(BigDecimal("30"), LocalDate.parse("2021-02-01")))
        val result = client.getAccountTrendsReport(account.id)
        println(result)
        assertEquals(2, result.size)
        assertEquals(0, result[0].balance.compareTo(BigDecimal("15")))
        assertEquals(0, result[1].balance.compareTo(BigDecimal("20")))

        val result2 = client.getAccountTrendsReport(account2.id)
        assertEquals(1, result2.size)
        assertEquals(0, result2[0].balance.compareTo(BigDecimal("30")))
    }

    @Test
    fun `user month trends`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val account2 = client.createAccount(accountDto.copy(name = "PEG"))
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("15"), LocalDate.parse("2021-01-01")))
        client.createAccountReport(account.id, AccountReportIn(BigDecimal("20"), LocalDate.parse("2021-02-01")))
        client.createAccountReport(account2.id, AccountReportIn(BigDecimal("30"), LocalDate.parse("2021-02-01")))
        val result = client.getUserAccountsTrendsReport()
        println(result)
        assertEquals(2, result.size)
        assertEquals(0, result[0].balance.compareTo(BigDecimal("15")))
        assertEquals(0, result[1].balance.compareTo(BigDecimal("50")))


    }

}