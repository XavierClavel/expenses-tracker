package com.xavierclavel.models

import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.dtos.sync.SyncExpense
import com.xavierclavel.enums.ExpenseType
import io.ebean.Model
import io.ebean.annotation.DbDefault
import io.ebean.annotation.Index
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
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "expenses")
@Index(columnNames = ["user_id", "revision"])
@Index(name = "idx_expense_user_date", columnNames = ["user_id", "date"])
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

    @ManyToMany
    @JoinTable(
        name = "expense_tag",
        joinColumns = [JoinColumn(name = "expense_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    var tags: MutableList<Tag> = mutableListOf(),

    /** Identifies the row to devices, which generate it for the rows they create. */
    @Column(unique = true)
    @DbDefault("\$RAW:gen_random_uuid()")
    val uuid: UUID = UUID.randomUUID(),

    ): Model() {

    @Id
    var id: Long = 0

    /** Position of the row's last change in its owner's sync history, set by the database. */
    @Column(insertable = false, updatable = false)
    @DbDefault("0")
    var revision: Long = 0


    fun toOutput() = ExpenseOut(
        id = this.id,
        title = this.title,
        amount = this.amount,
        currency = this.currency,
        date = this.date,
        categoryId = this.category?.id,
        type = this.type,
        tagIds = this.tags.map { it.id },
    )

    fun toSync() = SyncExpense(
        id = this.uuid,
        serverId = this.id,
        subcategoryId = this.category?.uuid,
        title = this.title,
        amount = this.amount,
        currency = this.currency,
        date = this.date,
        type = this.type,
        tagIds = this.tags.map { it.uuid }.sorted(),
    )
}
