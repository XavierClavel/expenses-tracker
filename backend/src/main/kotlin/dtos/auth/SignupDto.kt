package com.xavierclavel.dtos.auth

import kotlinx.serialization.Serializable

/**
 * Used to log in into an account.
 *
 * @property emailAddress Email address of the user.
 * @property password Password of the user.
 */
@Serializable
data class SignupDto(
    val emailAddress: String,
    val password: String,
)