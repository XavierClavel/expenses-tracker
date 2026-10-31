package com.xavierclavel.bankable.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_log_in
import com.xavierclavel.bankable.resources.action_sign_up
import com.xavierclavel.bankable.resources.auth_email_placeholder
import com.xavierclavel.bankable.resources.auth_have_account
import com.xavierclavel.bankable.resources.auth_signup_subtitle
import com.xavierclavel.bankable.resources.auth_signup_title
import com.xavierclavel.bankable.resources.error_password_mismatch
import com.xavierclavel.bankable.resources.label_confirm_password
import com.xavierclavel.bankable.resources.label_email
import com.xavierclavel.bankable.resources.label_password
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignupScreen(
    onSignup: (String, String, () -> Unit, (String) -> Unit) -> Unit,
    onGoogleSignIn: (String, (String) -> Unit) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var googleLoading by remember { mutableStateOf(false) }
    val passwordMismatch = stringResource(Res.string.error_password_mismatch)
    val isMismatching = confirmPassword.isNotEmpty() && confirmPassword != password

    AuthScaffold(
        footer = {
            AuthFooter(
                prompt = stringResource(Res.string.auth_have_account),
                action = stringResource(Res.string.action_log_in),
                enabled = true,
                onClick = onNavigateToLogin,
            )
        },
    ) {
        CircleIconButton(
            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(Res.string.action_back),
            onClick = onNavigateToLogin,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(Res.string.auth_signup_title),
            style = MaterialTheme.typography.displayMedium,
            color = NightTextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(Res.string.auth_signup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = NightTextSecondary,
        )
        Spacer(Modifier.height(28.dp))
        NightTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(Res.string.label_email),
            placeholder = stringResource(Res.string.auth_email_placeholder),
            stackedLabel = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        Spacer(Modifier.height(12.dp))
        NightTextField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(Res.string.label_password),
            stackedLabel = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Spacer(Modifier.height(12.dp))
        NightTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = stringResource(Res.string.label_confirm_password),
            stackedLabel = true,
            isError = isMismatching,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        // The live mismatch hint already covers the submit-time mismatch error.
        val errors = listOfNotNull(
            if (isMismatching) passwordMismatch else null,
            error?.takeUnless { isMismatching && it == passwordMismatch },
        )
        errors.forEach {
            Spacer(Modifier.height(10.dp))
            AuthErrorText(it)
        }
        Spacer(Modifier.height(20.dp))
        LoadingPrimaryButton(
            text = stringResource(Res.string.action_sign_up),
            loading = false,
            enabled = true,
            onClick = {
                error = null
                if (password != confirmPassword) {
                    error = passwordMismatch
                } else {
                    onSignup(email, password, onNavigateToLogin) { err -> error = err }
                }
            },
        )
        GoogleSection(
            enabled = !googleLoading,
            loading = googleLoading,
            onLoadingChange = { googleLoading = it },
            onIdToken = { token ->
                error = null
                onGoogleSignIn(token) { err -> error = err; googleLoading = false }
            },
            onError = { err -> error = err },
        )
    }
}
