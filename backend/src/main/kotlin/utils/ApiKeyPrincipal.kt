package com.xavierclavel.utils

/**
 * Principal set by the "api-key" authentication provider. Routes reject it unless they
 * explicitly opt in through `getUserIdAllowingApiKey`.
 */
data class ApiKeyPrincipal(
    val userId: Long,
    val apiKeyId: Long,
)
