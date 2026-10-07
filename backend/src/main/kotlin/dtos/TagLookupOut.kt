package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * A tag without its aggregated totals, served to API key callers that only need to
 * resolve tag ids.
 */
@Serializable
data class TagLookupOut(
    val id: Long,
    val label: String,
)
