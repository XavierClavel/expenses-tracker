package com.xavierclavel.models

import com.xavierclavel.dtos.CategoryOut
import com.xavierclavel.dtos.UserOut
import io.ebean.Model
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class Category(

    @ManyToOne
    var user: User,

    var name: String,

    ): Model() {

    @Id
    var id: Long = 0


    fun toOutput() = CategoryOut(
        id = this.id,
        name = this.name,
    )
}