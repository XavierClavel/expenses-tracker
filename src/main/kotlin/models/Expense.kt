package com.xavierclavel.models

import com.xavierclavel.dtos.CategoryOut
import com.xavierclavel.dtos.ExpenseOut
import io.ebean.Model
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import kotlinx.datetime.toKotlinLocalDate
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "expenses")
class Expense(

    @ManyToOne
    var user: User,

    @ManyToOne
    var category: Category? = null,

    var label: String,

    @Column(precision = 15, scale = 2)
    var amount: BigDecimal,

    var currency: String,

    var date: LocalDate,

    ): Model() {

    @Id
    var id: Long = 0


    fun toOutput() = ExpenseOut(
        id = this.id,
        label = this.label,
        amount = this.amount,
        currency = this.currency,
        date = this.date.toKotlinLocalDate(),
        categoryId = this.category?.id,
    )
}