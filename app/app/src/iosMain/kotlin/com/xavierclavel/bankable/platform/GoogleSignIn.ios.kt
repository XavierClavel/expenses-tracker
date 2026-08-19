package com.xavierclavel.bankable.platform

/**
 * Google sign-in is not wired up on iOS yet: it needs the GoogleSignIn iOS SDK
 * plus its own OAuth client ID and URL scheme. Until then the button is hidden
 * and users sign in with email + password.
 */
actual val isGoogleSignInSupported: Boolean = false

actual suspend fun requestGoogleIdToken(onError: (String) -> Unit): String? = null
