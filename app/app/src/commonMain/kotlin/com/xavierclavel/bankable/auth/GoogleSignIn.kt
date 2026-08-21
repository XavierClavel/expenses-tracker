package com.xavierclavel.bankable.auth

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
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.platform.isGoogleSignInSupported
import com.xavierclavel.bankable.platform.requestGoogleIdToken
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_continue_google
import com.xavierclavel.bankable.resources.ic_google_logo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * "Continue with Google" button that drives the on-device sign-in flow. Renders
 * nothing on platforms where Google sign-in isn't wired up (see
 * [isGoogleSignInSupported]).
 *
 * Loading is hoisted so the caller can keep it on across BOTH phases — the
 * platform token retrieval AND the backend token exchange that follows
 * [onIdToken]. The button turns it on when tapped and back off if the
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
    if (!isGoogleSignInSupported) return

    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            onLoadingChange(true)
            scope.launch {
                val token = requestGoogleIdToken(onError)
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
                painter = painterResource(Res.drawable.ic_google_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.action_continue_google))
        }
    }
}
