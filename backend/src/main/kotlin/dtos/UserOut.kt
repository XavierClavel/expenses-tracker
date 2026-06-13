package com.xavierclavel.dtos

import com.xavierclavel.enums.UserRole
import kotlinx.serialization.Serializable

/**
 * Represents a user in the system.
 *
 * @property mail e of the user.
 */
@Serializable
data class UserOut(
    val id: Long,
    val mail: String,
    val role: UserRole,
)