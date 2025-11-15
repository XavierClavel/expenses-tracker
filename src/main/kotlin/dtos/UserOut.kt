package com.xavierclavel.dtos

import com.xavierclavel.enums.UserRole
import kotlinx.serialization.Serializable

/**
 * Represents a user in the system.
 *
 * @property username Display name of the user.
 */
@Serializable
data class UserOut(
    val id: Long,
    val username: String,
    val role: UserRole,
)