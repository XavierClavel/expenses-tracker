package com.xavierclavel.bankable.auth

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.api.GOOGLE_WEB_CLIENT_ID
import kotlinx.coroutines.launch

/**
 * Launches the Credential Manager "Sign in with Google" flow and returns the
 * Google ID token, or null if the user dismissed the chooser or sign-in failed.
 * On failure (other than a deliberate user dismissal) [onError] is invoked with a
 * user-facing message so the screen can surface it.
 */
suspend fun getGoogleIdToken(context: Context, onError: (String) -> Unit): String? {
    val option = GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    return try {
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            onError(context.getString(R.string.error_google_signin))
            null
        }
    } catch (e: GetCredentialCancellationException) {
        // Play Services reports "[16] Account reauth failed" as a cancellation even though the
        // user didn't dismiss the chooser — surface that, but stay silent on a real user dismissal.
        android.util.Log.e("GoogleSignIn", "Credential request cancelled", e)
        if (e.message?.contains("reauth", ignoreCase = true) == true) {
            onError(context.getString(R.string.error_google_reauth))
        }
        null
    } catch (e: GetCredentialException) {
        android.util.Log.e("GoogleSignIn", "Credential request failed", e)
        onError(context.getString(R.string.error_google_signin))
        null
    }
}

/**
 * "Continue with Google" button that drives the on-device sign-in flow.
 *
 * Loading is hoisted so the caller can keep it on across BOTH phases — the
 * Credential Manager token retrieval AND the backend token exchange that
 * follows [onIdToken]. The button turns it on when tapped and back off if the
 * chooser is dismissed or token retrieval fails; the caller turns it off when
 * the backend exchange reports an error (success unmounts the screen).
 */
@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    loading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            onLoadingChange(true)
            scope.launch {
                val token = getGoogleIdToken(context, onError)
                if (token != null) onIdToken(token) else onLoadingChange(false)
            }
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_continue_google))
        }
    }
}
