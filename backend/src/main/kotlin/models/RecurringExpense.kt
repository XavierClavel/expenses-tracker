package com.xavierclavel.models

import com.xavierclavel.dtos.RecurringExpenseOut
import com.xavierclavel.enums.ExpenseType
import io.ebean.Model
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Template for an expense that repeats every month on [dayOfMonth]. On each occurrence a
 * regular [Expense] is created from its fields; editing the template never touches the
 * expenses it already created.
 *
 * @property dayOfMonth 1 to 31. In months shorter than that, the occurrence falls on the last day.
 * @property nextDate Date of the next expense to create.
 * @property lastGeneratedDate Date of the last expense created, or null if none was created yet.
 */
@Entity
@Table(name = "recurring_expenses")
class RecurringExpense(

    @ManyToOne
    var user: User,

    @ManyToOne
    var category: Subcategory?,

    var title: String,

    @Column(precision = 15, scale = 2)
    var amount: BigDecimal,

    var currency: String,

    @Enumerated(EnumType.STRING)
    var type: ExpenseType,

    var dayOfMonth: Int,

    var nextDate: LocalDate,

    var lastGeneratedDate: LocalDate? = null,

    @ManyToMany
    @JoinTable(
        name = "recurring_expense_tag",
        joinColumns = [JoinColumn(name = "recurring_expense_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    var tags: MutableList<Tag> = mutableListOf(),

    ): Model() {

    @Id
    var id: Long = 0

    /** Build the expense this template produces on [date]. */
    fun toExpense(date: LocalDate) = Expense(
        user = this.user,
        category = this.category,
        title = this.title,
        amount = this.amount,
        currency = this.currency,
        date = date,
        type = this.type,
        tags = this.tags.toMutableList(),
    )

    fun toOutput() = RecurringExpenseOut(
        id = this.id,
        title = this.title,
        amount = this.amount,
        currency = this.currency,
        categoryId = this.category?.id,
        type = this.type,
        tagIds = this.tags.map { it.id },
        dayOfMonth = this.dayOfMonth,
        nextDate = this.nextDate,
        lastGeneratedDate = this.lastGeneratedDate,
    )
}
