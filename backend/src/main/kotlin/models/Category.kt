package com.xavierclavel.models

import com.xavierclavel.dtos.CategoryOut
import com.xavierclavel.dtos.sync.SyncCategory
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.models.query.QSubcategory
import io.ebean.Model
import io.ebean.annotation.DbDefault
import io.ebean.annotation.Index
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "categories")
@Index(columnNames = ["user_id", "revision"])
class Category(

    @ManyToOne
    var user: User,

    var name: String,

    @DbDefault("#ffffff")
    var color: String,

    @DbDefault("")
    var icon: String,

    @DbDefault("EXPENSE")
    @Enumerated(EnumType.STRING)
    val type: ExpenseType,

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


    fun toOutput() = CategoryOut(
        id = this.id,
        name = this.name,
        color = this.color,
        icon = this.icon,
        subcategories = QSubcategory().parentCategory.id.eq(this.id).findList().map { it.toOutput() },
        type = this.type,
    )

    fun toSync() = SyncCategory(
        id = this.uuid,
        serverId = this.id,
        name = this.name,
        color = this.color,
        icon = this.icon,
        type = this.type,
    )
}
