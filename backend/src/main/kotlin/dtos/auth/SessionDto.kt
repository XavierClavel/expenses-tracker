package com.xavierclavel.dtos.auth

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val token: String,
)