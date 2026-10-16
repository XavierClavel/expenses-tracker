package com.xavierclavel.models

import com.xavierclavel.dtos.TagOut
import com.xavierclavel.dtos.sync.SyncTag
import io.ebean.Model
import io.ebean.annotation.DbDefault
import io.ebean.annotation.Index
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "tags")
@Index(columnNames = ["user_id", "revision"])
class Tag(

    @ManyToOne
    var user: User,

    var label: String,

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


    fun toOutput(total: BigDecimal = BigDecimal.ZERO, expenseCount: Int = 0) = TagOut(
        id = this.id,
        label = this.label,
        total = total,
        expenseCount = expenseCount,
    )

    fun toSync() = SyncTag(
        id = this.uuid,
        label = this.label,
    )
}
