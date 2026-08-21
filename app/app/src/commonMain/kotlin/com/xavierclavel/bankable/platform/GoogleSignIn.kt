package com.xavierclavel.bankable.platform

/**
 * Whether "Continue with Google" can be offered on this platform. The button is
 * hidden entirely where it isn't — currently iOS, which would need the Google
 * Sign-In SDK and its own OAuth client.
 */
expect val isGoogleSignInSupported: Boolean

/**
 * Runs the platform's Google sign-in flow and returns the Google ID token, or
 * null if the user dismissed it or sign-in failed. On failure (other than a
 * deliberate dismissal) [onError] is invoked with a user-facing message.
 */
expect suspend fun requestGoogleIdToken(onError: (String) -> Unit): String?
