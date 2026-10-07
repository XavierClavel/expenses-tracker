package com.xavierclavel.dtos

import com.xavierclavel.utils.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * Represents an API key exposed to clients. Never carries the key itself.
 *
 * @property hint Masked form of the key, safe to display (e.g. "bk_…a1B2").
 * @property lastUsedAt Day the key last authenticated a request, or null if it never did.
 */
@Serializable
data class ApiKeyOut(
    val id: Long,
    val name: String,
    val hint: String,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val lastUsedAt: LocalDate?,
)

/**
 * Response to an API key creation: the only time the full [key] is ever returned.
 */
@Serializable
data class ApiKeyCreatedOut(
    val id: Long,
    val name: String,
    val hint: String,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val lastUsedAt: LocalDate?,
    val key: String,
)
