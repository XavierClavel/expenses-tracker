package com.xavierclavel.models

import com.xavierclavel.dtos.TagOut
import io.ebean.Model
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "tags")
class Tag(

    @ManyToOne
    var user: User,

    var label: String,

    ): Model() {

    @Id
    var id: Long = 0


    fun toOutput(total: BigDecimal = BigDecimal.ZERO, expenseCount: Int = 0) = TagOut(
        id = this.id,
        label = this.label,
        total = total,
        expenseCount = expenseCount,
    )
}
