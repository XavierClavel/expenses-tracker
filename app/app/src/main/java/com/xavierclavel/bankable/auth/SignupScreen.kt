package com.xavierclavel.bankable.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.R

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
    val passwordMismatch = stringResource(R.string.error_password_mismatch)

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.action_sign_up), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text(stringResource(R.string.label_email)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text(stringResource(R.string.label_password)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it },
                label = { Text(stringResource(R.string.label_confirm_password)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPassword.isNotEmpty() && confirmPassword != password,
            )
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    error = null
                    if (password != confirmPassword) {
                        error = passwordMismatch
                    } else {
                        onSignup(email, password, onNavigateToLogin) { err -> error = err }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_sign_up)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_log_in))
            }
            Spacer(Modifier.height(8.dp))
            GoogleSignInButton(
                enabled = true,
                onIdToken = { token -> error = null; onGoogleSignIn(token) { err -> error = err } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
