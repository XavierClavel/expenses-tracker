package com.xavierclavel.models

import com.xavierclavel.dtos.CategoryOut
import dtos.ExpenseOut
import io.ebean.Model
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "expenses")
class Expense(

    @ManyToOne
    var user: User,

    var category: Category? = null,

    var label: String,

    var amount: Double,

    var currency: String,

    var date: LocalDateTime = LocalDateTime.now(),

    ): Model() {

    @Id
    var id: Long = 0


    fun toOutput() = ExpenseOut(
        id = this.id,
        label = this.label,
        amount = this.amount,
        currency = this.currency,
        date = this.date,
        categoryId = this.category?.id,
    )
}