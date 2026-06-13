package com.xavierclavel.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleLoginDto(
    val idToken: String,
)
