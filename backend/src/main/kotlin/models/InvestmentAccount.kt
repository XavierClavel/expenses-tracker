package com.xavierclavel.models

import com.xavierclavel.dtos.InvestmentAccountOut
import io.ebean.Model
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

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
        amount =
    )
}