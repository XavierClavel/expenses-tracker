package com.xavierclavel.models

import com.xavierclavel.dtos.investment.InvestmentAccountOut
import com.xavierclavel.enums.AccountTracking
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

    @DbDefault("CONTRIBUTIONS")
    @Enumerated(EnumType.STRING)
    var tracking: AccountTracking = AccountTracking.CONTRIBUTIONS,

    @ManyToOne
    val owner: User,
): Model() {
    @Id
    var id: Long = 0

    fun toOutput(): InvestmentAccountOut {
        val balance = QAccountReport()
            .account.id.eq(id)
            .orderBy().date.desc()
            .setMaxRows(1)
            .findOne()
            ?.amount
            ?: BigDecimal.ZERO
        val investments = QInvestment().account.id.eq(id).findList()
        // Net of each pair; the mode decides which is authoritative and which is derived.
        val netContributions = investments.fold(BigDecimal.ZERO) { acc, inv ->
            when (inv.type) {
                InvestmentType.IN -> acc + inv.amount
                InvestmentType.OUT -> acc - inv.amount
                else -> acc
            }
        }
        val netInterest = investments.fold(BigDecimal.ZERO) { acc, inv ->
            when (inv.type) {
                InvestmentType.INTEREST -> acc + inv.amount
                InvestmentType.FEE -> acc - inv.amount
                else -> acc
            }
        }
        val contributions = when (tracking) {
            AccountTracking.CONTRIBUTIONS -> netContributions
            AccountTracking.INTEREST -> balance - netInterest
        }
        return InvestmentAccountOut(
            id = id,
            name = name,
            type = type,
            tracking = tracking,
            amount = balance,
            contributions = contributions,
        )
    }
}