package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Request body used to create or update a tag.
 *
 * @property label Display label of the tag.
 */
@Serializable
data class TagIn(
    val label: String,
)
