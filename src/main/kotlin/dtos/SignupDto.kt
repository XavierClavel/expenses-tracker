package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Used to log in into an account.
 *
 * @property emailAddress Email address of the user.
 * @property password Password of the user.
 */
@Serializable
data class SignupDto(
    val username: String,
    val emailAddress: String,
    val password: String,
)