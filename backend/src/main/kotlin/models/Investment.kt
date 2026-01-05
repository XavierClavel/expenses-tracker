package com.xavierclavel.models

import com.xavierclavel.dtos.investment.InvestmentOut
import com.xavierclavel.enums.InvestmentType
import io.ebean.Model
import io.ebean.annotation.DbDefault
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "investments")
class Investment(

    @Column(precision = 15, scale = 2)
    var amount: BigDecimal,

    @ManyToOne
    val account: InvestmentAccount,

    @ManyToOne
    val user: User,

    val date: LocalDate,

    @Enumerated(EnumType.STRING)
    val type: InvestmentType,

): Model() {

    @Id
    var id: Long = 0

    fun toOutput() = InvestmentOut(
        id = id,
        amount = amount,
        accountId = account.id,
        date = date,
        type = type,
    )
}