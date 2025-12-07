package com.xavierclavel.models

import com.xavierclavel.dtos.SubcategoryOut
import com.xavierclavel.enums.ExpenseType
import io.ebean.Model
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "subcategories")
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

    ): Model() {

    @Id
    var id: Long = 0


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
}