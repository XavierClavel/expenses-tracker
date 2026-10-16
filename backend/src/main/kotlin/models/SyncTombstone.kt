package com.xavierclavel.models

import com.xavierclavel.dtos.sync.SyncDeletion
import com.xavierclavel.enums.SyncEntityType
import io.ebean.Model
import io.ebean.annotation.ConstraintMode
import io.ebean.annotation.DbDefault
import io.ebean.annotation.DbForeignKey
import io.ebean.annotation.Index
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

/**
 * Records that a synced row was deleted, so that devices still holding it drop it on their
 * next pull. The database writes one whenever a category, subcategory, tag or expense is
 * deleted. The sync service also writes one to refuse a row a device created.
 */
@Entity
@Table(name = "sync_tombstones")
@Index(columnNames = ["user_id", "revision"])
class SyncTombstone(

    @ManyToOne
    @DbForeignKey(onDelete = ConstraintMode.CASCADE)
    val user: User,

    @Enumerated(EnumType.STRING)
    val type: SyncEntityType,

    /** The deleted row's uuid. */
    @Id
    val uuid: UUID,

    ): Model() {

    /** Position of the deletion in its owner's sync history, set by the database. */
    @Column(insertable = false, updatable = false)
    @DbDefault("0")
    var revision: Long = 0

    fun toSync() = SyncDeletion(
        type = this.type,
        id = this.uuid,
    )
}
