package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

/** Request body for batch operations that act on a list of entity ids. */
@Serializable
data class IdListIn(
    val ids: List<Long>,
)
