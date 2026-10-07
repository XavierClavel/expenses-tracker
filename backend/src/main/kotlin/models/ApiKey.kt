package com.xavierclavel.models

import com.xavierclavel.dtos.ApiKeyOut
import io.ebean.Model
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * Personal API key letting another app act for [user] with a restricted scope (see
 * `getUserIdAllowingApiKey`). The key itself is never stored, only its SHA-256 hash.
 *
 * @property hashedKey Hex SHA-256 of the full key, used to look it up on each request.
 * @property hint Masked form of the key, safe to display (e.g. "bk_…a1B2").
 * @property lastUsedAt Day the key last authenticated a request, or null if it never did.
 */
@Entity
@Table(name = "api_keys")
class ApiKey(

    @ManyToOne
    var user: User,

    var name: String,

    @Column(unique = true)
    var hashedKey: String,

    var hint: String,

    var createdAt: LocalDate,

    var lastUsedAt: LocalDate? = null,

    ): Model() {

    @Id
    var id: Long = 0


    fun toOutput() = ApiKeyOut(
        id = this.id,
        name = this.name,
        hint = this.hint,
        createdAt = this.createdAt,
        lastUsedAt = this.lastUsedAt,
    )
}
