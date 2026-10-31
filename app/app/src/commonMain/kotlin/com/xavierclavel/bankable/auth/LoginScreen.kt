package com.xavierclavel.bankable.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.platform.isGoogleSignInSupported
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_log_in
import com.xavierclavel.bankable.resources.action_sign_up
import com.xavierclavel.bankable.resources.app_name
import com.xavierclavel.bankable.resources.auth_email_placeholder
import com.xavierclavel.bankable.resources.auth_new_here
import com.xavierclavel.bankable.resources.auth_or
import com.xavierclavel.bankable.resources.auth_tagline
import com.xavierclavel.bankable.resources.label_email
import com.xavierclavel.bankable.resources.label_password
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightDivider
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.ui.theme.OnMint
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: (String, (String) -> Unit) -> Unit,
    onNavigateToSignup: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleError by remember { mutableStateOf<String?>(null) }
    var googleLoading by remember { mutableStateOf(false) }
    val isLoading = authState is AuthState.Loading || googleLoading

    AuthScaffold(
        footer = {
            AuthFooter(
                prompt = stringResource(Res.string.auth_new_here),
                action = stringResource(Res.string.action_sign_up),
                enabled = !isLoading,
                onClick = onNavigateToSignup,
            )
        },
    ) {
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoTile()
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(Res.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = NightTextPrimary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.auth_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = NightTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(36.dp))
        NightTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(Res.string.label_email),
            placeholder = stringResource(Res.string.auth_email_placeholder),
            stackedLabel = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(12.dp))
        NightTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(Res.string.label_password),
            stackedLabel = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        val errorMessage = (authState as? AuthState.Unauthenticated)?.error ?: googleError
        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            AuthErrorText(errorMessage)
        }
        Spacer(Modifier.height(20.dp))
        LoadingPrimaryButton(
            text = stringResource(Res.string.action_log_in),
            loading = authState is AuthState.Loading,
            enabled = !isLoading,
            onClick = { onLogin(email, password) },
        )
        GoogleSection(
            enabled = !isLoading,
            loading = googleLoading,
            onLoadingChange = { googleLoading = it },
            onIdToken = { token ->
                googleError = null
                onGoogleSignIn(token) { err -> googleError = err; googleLoading = false }
            },
            onError = { googleError = it },
        )
        Spacer(Modifier.weight(1f))
    }
}

/** Full-height, scrollable auth layout with [footer] pinned to the bottom when content fits. */
@Composable
internal fun AuthScaffold(
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(containerColor = NightBackground) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minHeight)
                        .padding(horizontal = ScreenGutter)
                        .padding(top = 16.dp, bottom = 24.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().weight(1f), content = content)
                    Spacer(Modifier.height(24.dp))
                    footer()
                }
            }
        }
    }
}

@Composable
private fun LogoTile() {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .size(80.dp)
            .shadow(28.dp, shape, ambientColor = Mint, spotColor = Mint)
            .clip(shape)
            .background(Mint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.AccountBalance,
            contentDescription = null,
            tint = OnMint,
            modifier = Modifier.size(40.dp),
        )
    }
}

/** [PrimaryButton] that swaps its label for a spinner while [loading]. */
@Composable
internal fun LoadingPrimaryButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        PrimaryButton(
            text = if (loading) "" else text,
            onClick = onClick,
            enabled = enabled,
        )
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = OnMint,
            )
        }
    }
}

/** "or" divider followed by the Google button; nothing at all where Google sign-in is unsupported. */
@Composable
internal fun GoogleSection(
    enabled: Boolean,
    loading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    if (!isGoogleSignInSupported) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(NightDivider))
        Text(
            stringResource(Res.string.auth_or),
            style = MaterialTheme.typography.bodyMedium,
            color = NightTextTertiary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(NightDivider))
    }
    GoogleSignInButton(
        enabled = enabled,
        loading = loading,
        onLoadingChange = onLoadingChange,
        onIdToken = onIdToken,
        onError = onError,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun AuthErrorText(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = Danger,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
internal fun AuthFooter(
    prompt: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(prompt, style = MaterialTheme.typography.bodyLarge, color = NightTextSecondary)
        Spacer(Modifier.width(6.dp))
        Text(
            action,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Mint,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(PaddingValues(horizontal = 4.dp, vertical = 6.dp)),
        )
    }
}
