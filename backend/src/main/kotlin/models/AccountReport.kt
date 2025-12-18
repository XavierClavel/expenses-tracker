package com.xavierclavel.models

import com.xavierclavel.dtos.investment.AccountReportOut
import com.xavierclavel.utils.LocalDateSerializer
import io.ebean.Model
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "account_reports")
class AccountReport(
    @Column(precision = 15, scale = 2)
    var amount: BigDecimal,

    @ManyToOne
    var account: InvestmentAccount,

    val date: LocalDate,

): Model() {
    @Id
    var id: Long = 0

    fun toOutput() = AccountReportOut(
        id = id,
        accountId = account.id,
        amount = amount,
        date = date,
    )
}