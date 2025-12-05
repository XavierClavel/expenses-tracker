package com.xavierclavel.models

import io.ebean.Model
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "month_commentaries")
class MonthCommentary(
    val month: Int,
    val year: Int,
    val comment: String,

    @ManyToOne
    val user: User,

): Model() {
    @Id
    var id: Long = 0
}