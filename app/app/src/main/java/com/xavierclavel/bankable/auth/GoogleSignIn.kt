package com.xavierclavel.bankable.auth

import android.content.Context
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.api.GOOGLE_WEB_CLIENT_ID
import kotlinx.coroutines.launch

/**
 * Launches the Credential Manager "Sign in with Google" flow and returns the
 * Google ID token, or null if the user cancelled or no Google credential came back.
 */
suspend fun getGoogleIdToken(context: Context): String? {
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
            null
        }
    } catch (e: GetCredentialException) {
        null
    }
}

/** "Continue with Google" button that drives the on-device sign-in flow. */
@Composable
fun GoogleSignInButton(
    enabled: Boolean,
    onIdToken: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            scope.launch {
                getGoogleIdToken(context)?.let(onIdToken)
            }
        },
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.action_continue_google))
    }
}
