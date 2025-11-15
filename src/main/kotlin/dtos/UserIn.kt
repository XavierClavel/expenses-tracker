package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Represents a user in the system.
 *
 * @property username Display name of the user.
 */
@Serializable
data class UserIn(
    val username: String,
)
