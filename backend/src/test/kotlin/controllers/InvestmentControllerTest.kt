package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.investment.InvestmentIn
import com.xavierclavel.dtos.investment.InvestmentAccountIn
import com.xavierclavel.enums.InvestmentType
import com.xavierclavel.utils.INVESTMENT_URL
import com.xavierclavel.utils.createAccount
import com.xavierclavel.utils.createInvestment
import com.xavierclavel.utils.deleteInvestment
import com.xavierclavel.utils.getInvestment
import com.xavierclavel.utils.listInvestments
import com.xavierclavel.utils.listInvestmentsByAccount
import com.xavierclavel.utils.updateInvestment
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class InvestmentControllerTest: ApplicationTest() {

    private val accountDto = InvestmentAccountIn(name = "PEA")

    private fun investmentIn(
        accountId: Long,
        amount: String = "100",
        type: InvestmentType = InvestmentType.IN,
        date: String = "2021-01-01",
    ) = InvestmentIn(
        amount = BigDecimal(amount),
        accountId = accountId,
        type = type,
        date = LocalDate.parse(date),
    )

    @Test
    fun `create transfer`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val created = client.createInvestment(account.id, investmentIn(account.id, "100", InvestmentType.IN))
        val result = client.getInvestment(created.id)
        assertEquals(0, result.amount.compareTo(BigDecimal("100")))
        assertEquals(InvestmentType.IN, result.type)
        assertEquals(account.id, result.accountId)
        assertEquals(LocalDate.parse("2021-01-01"), result.date)
    }

    @Test
    fun `list transfers by account`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val account2 = client.createAccount(accountDto.copy(name = "PEG"))
        client.createInvestment(account.id, investmentIn(account.id, "100"))
        client.createInvestment(account.id, investmentIn(account.id, "50", InvestmentType.OUT, "2021-02-01"))
        client.createInvestment(account2.id, investmentIn(account2.id, "30"))

        assertEquals(2, client.listInvestmentsByAccount(account.id).size)
        assertEquals(1, client.listInvestmentsByAccount(account2.id).size)
        assertEquals(3, client.listInvestments().size)
    }

    @Test
    fun `update transfer`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val created = client.createInvestment(account.id, investmentIn(account.id, "100", InvestmentType.IN))
        val updated = client.updateInvestment(
            created.id,
            investmentIn(account.id, "250", InvestmentType.OUT, "2022-05-01"),
        )
        assertEquals(0, updated.amount.compareTo(BigDecimal("250")))
        assertEquals(InvestmentType.OUT, updated.type)
        assertEquals(LocalDate.parse("2022-05-01"), updated.date)
    }

    @Test
    fun `delete transfer`() = runTestAsUser {
        val account = client.createAccount(accountDto)
        val created = client.createInvestment(account.id, investmentIn(account.id))
        client.deleteInvestment(created.id)
        client.get("$INVESTMENT_URL/${created.id}").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun `a user cannot read another user's transfer`() = runTest {
        var otherTransferId = 0L
        runAsUser1 {
            val account = client.createAccount(accountDto)
            otherTransferId = client.createInvestment(account.id, investmentIn(account.id)).id
        }
        runAsUser2 {
            client.get("$INVESTMENT_URL/$otherTransferId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
            assertEquals(0, client.listInvestments().size)
        }
    }
}
