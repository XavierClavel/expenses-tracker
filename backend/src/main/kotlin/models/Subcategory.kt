package com.xavierclavel.models

import com.xavierclavel.dtos.SubcategoryOut
import com.xavierclavel.dtos.sync.SyncSubcategory
import com.xavierclavel.enums.ExpenseType
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
@Table(name = "subcategories")
@Index(columnNames = ["user_id", "revision"])
class Subcategory(

    @ManyToOne
    var user: User,

    var name: String,

    var icon: String,

    val isDefault: Boolean = false,

    @Enumerated(EnumType.STRING)
    val type: ExpenseType,

    @ManyToOne
    var parentCategory: Category,

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


    fun toOutput() = SubcategoryOut(
        id = this.id,
        name = this.name,
        color = this.parentCategory.color,
        icon = if (this.isDefault) {
            this.parentCategory.icon
        } else {
            this.icon
        },
        type = this.type,
        isDefault = this.isDefault,
    )

    fun toSync() = SyncSubcategory(
        id = this.uuid,
        categoryId = this.parentCategory.uuid,
        name = this.name,
        icon = this.icon,
        type = this.type,
        isDefault = this.isDefault,
    )
}
