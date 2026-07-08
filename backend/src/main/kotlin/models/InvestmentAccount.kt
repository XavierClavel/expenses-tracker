package com.xavierclavel.models

import com.xavierclavel.dtos.investment.InvestmentAccountOut
import com.xavierclavel.enums.AccountType
import com.xavierclavel.enums.InvestmentType
import com.xavierclavel.models.query.QAccountReport
import com.xavierclavel.models.query.QInvestment
import io.ebean.DB
import io.ebean.Model
import io.ebean.annotation.DbDefault
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "investment_accounts")
class InvestmentAccount(
    var name: String = "",

    @DbDefault("OTHER")
    @Enumerated(EnumType.STRING)
    var type: AccountType = AccountType.OTHER,

    @ManyToOne
    val owner: User,
): Model() {
    @Id
    var id: Long = 0

    fun toOutput() = InvestmentAccountOut(
        id = id,
        name = name,
        type = type,
        amount = QAccountReport()
            .account.id.eq(id)
            .orderBy().date.desc()
            .setMaxRows(1)
            .findOne()
            ?.amount
            ?: BigDecimal.ZERO,
        contributions = QInvestment()
            .account.id.eq(id)
            .findList()
            .fold(BigDecimal.ZERO) { acc, inv ->
                if (inv.type == InvestmentType.IN) acc + inv.amount else acc - inv.amount
            },
    )
}