package com.xavierclavel.models

import com.xavierclavel.dtos.UserOut
import com.xavierclavel.enums.UserRole
import io.ebean.Model
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User(

    var username: String = "",

    @Column(unique = true)
    var emailAddress: String = "",

    var hashedPassword: String? = null,

    var googleId: String? = null,

    @Enumerated(EnumType.STRING)
    var role: UserRole = UserRole.USER,

    @OneToMany(cascade = [(CascadeType.REMOVE)])
    var categories: MutableList<Category> = mutableListOf(),

    @OneToMany(cascade = [(CascadeType.REMOVE)])
    var expenses: MutableList<Expense> = mutableListOf(),

    ): Model() {

    @Id
    var id: Long = 0


    fun toOutput() = UserOut(
        id = this.id,
        username = this.username,
        role = this.role,
    )
}