package com.xavierclavel.models

import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.enums.ExpenseType
import io.ebean.Model
import io.ebean.annotation.DbDefault
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "expenses")
class Expense(

    @ManyToOne
    var user: User,

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var category: Subcategory?,

    @DbDefault("")
    var title: String,

    @Column(precision = 15, scale = 2)
    var amount: BigDecimal,

    var currency: String,

    var date: LocalDate,

    var hidden: Boolean = false,

    var marked: Boolean = false,

    @DbDefault("EXPENSE")
    @Enumerated(EnumType.STRING)
    val type: ExpenseType,

    ): Model() {

    @Id
    var id: Long = 0


    fun toOutput() = ExpenseOut(
        id = this.id,
        title = this.title,
        amount = this.amount,
        currency = this.currency,
        date = this.date,
        categoryId = this.category?.id,
        type = this.type,
    )
}