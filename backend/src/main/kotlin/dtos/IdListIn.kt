package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Request body carrying a list of entity ids, used by batch operations.
 */
@Serializable
data class IdListIn(
    val ids: List<Long>,
)
