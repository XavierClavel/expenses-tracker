package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Request body used to create an API key.
 *
 * @property name Label telling the user which app holds the key.
 */
@Serializable
data class ApiKeyIn(
    val name: String,
)
