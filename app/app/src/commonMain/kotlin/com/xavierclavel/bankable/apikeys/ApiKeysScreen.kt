package com.xavierclavel.bankable.apikeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.xavierclavel.bankable.api.API_KEY_EXPENSES_URL
import com.xavierclavel.bankable.model.ApiKeyOut
import com.xavierclavel.bankable.model.CreatedApiKeyOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.plainTextClipEntry
import com.xavierclavel.bankable.platform.showToast
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_copied
import com.xavierclavel.bankable.resources.action_copy
import com.xavierclavel.bankable.resources.action_create
import com.xavierclavel.bankable.resources.action_done
import com.xavierclavel.bankable.resources.action_revoke
import com.xavierclavel.bankable.resources.api_key_created_on
import com.xavierclavel.bankable.resources.api_key_created_title
import com.xavierclavel.bankable.resources.api_key_created_warning
import com.xavierclavel.bankable.resources.api_key_last_used_on
import com.xavierclavel.bankable.resources.api_key_name_hint
import com.xavierclavel.bankable.resources.api_key_never_used
import com.xavierclavel.bankable.resources.api_keys_empty
import com.xavierclavel.bankable.resources.api_keys_intro
import com.xavierclavel.bankable.resources.api_keys_usage
import com.xavierclavel.bankable.resources.api_keys_usage_header
import com.xavierclavel.bankable.resources.cd_add_api_key
import com.xavierclavel.bankable.resources.cd_revoke_api_key
import com.xavierclavel.bankable.resources.dialog_revoke_api_key_message
import com.xavierclavel.bankable.resources.dialog_revoke_api_key_title
import com.xavierclavel.bankable.resources.label_name
import com.xavierclavel.bankable.resources.screen_api_keys
import com.xavierclavel.bankable.resources.screen_new_api_key
import com.xavierclavel.bankable.util.formatIsoDateLong
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    viewModel: ApiKeysViewModel,
    navController: NavController,
) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var keyToRevoke by remember { mutableStateOf<ApiKeyOut?>(null) }

    // "Last used" dates move as other apps use the keys; refresh on each visit.
    LaunchedEffect(Unit) { viewModel.loadApiKeys() }
    // However the user leaves the screen, the one-time secret doesn't outlive it.
    DisposableEffect(Unit) {
        onDispose { viewModel.clearCreatedApiKey() }
    }

    if (showCreateDialog) {
        CreateApiKeyDialog(viewModel, onDismiss = { showCreateDialog = false })
    }

    viewModel.createdApiKey?.let { created ->
        CreatedApiKeyDialog(created, onDone = viewModel::clearCreatedApiKey)
    }

    keyToRevoke?.let { apiKey ->
        AlertDialog(
            onDismissRequest = { keyToRevoke = null },
            title = { Text(stringResource(Res.string.dialog_revoke_api_key_title)) },
            text = { Text(stringResource(Res.string.dialog_revoke_api_key_message, apiKey.name)) },
            confirmButton = {
                TextButton(onClick = {
                    keyToRevoke = null
                    viewModel.revokeApiKey(apiKey, onError = { msg -> showToast(msg) })
                }) {
                    Text(stringResource(Res.string.action_revoke), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToRevoke = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.screen_api_keys)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_add_api_key))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (viewModel.isLoading && apiKeys.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 80.dp),
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.api_keys_intro),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ApiKeyUsage()
                        }
                    }
                    if (apiKeys.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.api_keys_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp),
                            )
                        }
                    }
                    items(apiKeys, key = { it.id }) { apiKey ->
                        ApiKeyRow(apiKey = apiKey, onRevoke = { keyToRevoke = apiKey })
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiKeyRow(
    apiKey: ApiKeyOut,
    onRevoke: () -> Unit,
) {
    val locale = LocalAppLocale.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = apiKey.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = apiKey.hint,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        Res.string.api_key_created_on,
                        formatIsoDateLong(apiKey.createdAt, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = apiKey.lastUsedAt
                        ?.let { stringResource(Res.string.api_key_last_used_on, formatIsoDateLong(it, locale)) }
                        ?: stringResource(Res.string.api_key_never_used),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRevoke) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.cd_revoke_api_key),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CreateApiKeyDialog(
    viewModel: ApiKeysViewModel,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.screen_new_api_key)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.label_name)) },
                    supportingText = { Text(stringResource(Res.string.api_key_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = null
                    viewModel.createApiKey(
                        name = name,
                        onSuccess = onDismiss,
                        onError = { e -> error = e },
                    )
                },
                enabled = name.isNotBlank() && !viewModel.isCreating,
            ) {
                Text(stringResource(Res.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

/** Shows a just-created key's secret, the one and only time it is available. */
@Composable
private fun CreatedApiKeyDialog(
    created: CreatedApiKeyOut,
    onDone: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(created) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDone,
        // A stray tap outside would throw away a secret that can't be shown again.
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(stringResource(Res.string.api_key_created_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.api_key_created_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    SelectionContainer {
                        Text(
                            text = created.key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            try {
                                clipboard.setClipEntry(plainTextClipEntry(created.key, sensitive = true))
                                copied = true
                            } catch (_: Exception) {
                                // The key stays selectable above, so it can still be copied by hand.
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (copied) Res.string.action_copied else Res.string.action_copy))
                }
                ApiKeyUsage()
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) {
                Text(stringResource(Res.string.action_done))
            }
        },
    )
}

/** How another app uses a key: the request to send and the header that carries the key. */
@Composable
private fun ApiKeyUsage() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.api_keys_usage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            SelectionContainer {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Protocol syntax, identical in every language, so not a string resource.
                    Text(
                        text = "POST $API_KEY_EXPENSES_URL",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = stringResource(Res.string.api_keys_usage_header),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
