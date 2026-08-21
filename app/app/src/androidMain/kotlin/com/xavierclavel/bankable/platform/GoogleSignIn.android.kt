package com.xavierclavel.bankable.platform

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.xavierclavel.bankable.api.GOOGLE_WEB_CLIENT_ID
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.error_google_reauth
import com.xavierclavel.bankable.resources.error_google_signin
import org.jetbrains.compose.resources.getString

actual val isGoogleSignInSupported: Boolean = true

actual suspend fun requestGoogleIdToken(onError: (String) -> Unit): String? {
    // Credential Manager needs an Activity context to show the chooser.
    val activity = AppContext.currentActivity ?: run {
        onError(getString(Res.string.error_google_signin))
        return null
    }
    val option = GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            onError(getString(Res.string.error_google_signin))
            null
        }
    } catch (e: GetCredentialCancellationException) {
        // Play Services reports "[16] Account reauth failed" as a cancellation even though the
        // user didn't dismiss the chooser — surface that, but stay silent on a real user dismissal.
        Log.e("GoogleSignIn", "Credential request cancelled", e)
        if (e.message?.contains("reauth", ignoreCase = true) == true) {
            onError(getString(Res.string.error_google_reauth))
        }
        null
    } catch (e: GetCredentialException) {
        Log.e("GoogleSignIn", "Credential request failed", e)
        onError(getString(Res.string.error_google_signin))
        null
    }
}
