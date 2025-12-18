package com.xavierclavel.models

import com.xavierclavel.dtos.investment.InvestmentAccountOut
import com.xavierclavel.models.query.QAccountReport
import io.ebean.DB
import io.ebean.Model
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "investment_accounts")
class InvestmentAccount(
    var name: String = "",

    @OneToOne
    val owner: User,
): Model() {
    @Id
    var id: Long = 0

    fun toOutput() = InvestmentAccountOut(
        id = id,
        name = name,
        amount = QAccountReport()
            .account.id.eq(id)
            .orderBy().date.desc()
            .setMaxRows(1)
            .findOne()
            ?.amount
            ?: BigDecimal.ZERO,
    )
}