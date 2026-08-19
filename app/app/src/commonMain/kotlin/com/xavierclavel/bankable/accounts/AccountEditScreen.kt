package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.AccountType
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.action_save
import com.xavierclavel.bankable.resources.dialog_delete_account_message
import com.xavierclavel.bankable.resources.dialog_delete_account_title
import com.xavierclavel.bankable.resources.label_account_name
import com.xavierclavel.bankable.resources.label_account_type
import com.xavierclavel.bankable.resources.label_tracking
import com.xavierclavel.bankable.resources.screen_edit_account
import com.xavierclavel.bankable.resources.screen_new_account
import com.xavierclavel.bankable.resources.tracking_contributions
import com.xavierclavel.bankable.resources.tracking_contributions_hint
import com.xavierclavel.bankable.resources.tracking_interest
import com.xavierclavel.bankable.resources.tracking_interest_hint
import com.xavierclavel.bankable.ui.SlidingToggle
import org.jetbrains.compose.resources.stringResource

const val TRACKING_CONTRIBUTIONS = "CONTRIBUTIONS"
const val TRACKING_INTEREST = "INTEREST"

// Savings accounts (Livret A) default to recording known interest; everything else
// records contributions and infers interest.
private fun defaultTracking(typeKey: String) =
    if (typeKey == AccountType.SAVINGS.key) TRACKING_INTEREST else TRACKING_CONTRIBUTIONS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    val isEditing = viewModel.selectedAccount != null
    var name by rememberSaveable { mutableStateOf(viewModel.selectedAccount?.name ?: "") }
    var type by rememberSaveable {
        mutableStateOf(viewModel.selectedAccount?.type ?: AccountType.CHECKING.key)
    }
    var tracking by rememberSaveable {
        mutableStateOf(viewModel.selectedAccount?.tracking ?: defaultTracking(type))
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) stringResource(Res.string.screen_edit_account) else stringResource(Res.string.screen_new_account)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.label_account_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            AccountTypeSelector(
                selected = AccountType.fromKey(type),
                onSelect = { type = it.key },
            )

            Text(
                text = stringResource(Res.string.label_tracking),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SlidingToggle(
                options = listOf(
                    TRACKING_CONTRIBUTIONS to stringResource(Res.string.tracking_contributions),
                    TRACKING_INTEREST to stringResource(Res.string.tracking_interest),
                ),
                selected = tracking,
                onSelect = { tracking = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    if (tracking == TRACKING_INTEREST) Res.string.tracking_interest_hint
                    else Res.string.tracking_contributions_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    viewModel.saveAccount(
                        name = name,
                        type = type,
                        tracking = tracking,
                        onSuccess = { navController.popBackStack() },
                        onError = { errorMessage = it },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.dialog_delete_account_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_account_message, viewModel.selectedAccount?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount(
                            onSuccess = {
                                navController.popBackStack("accounts", false)
                            },
                            onError = { errorMessage = it },
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeSelector(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = stringResource(selected.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.label_account_type)) },
            leadingIcon = { Icon(selected.icon, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor          = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor        = MaterialTheme.colorScheme.outline,
                disabledLabelColor         = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor  = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AccountType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.labelRes)) },
                    leadingIcon = { Icon(type.icon, contentDescription = null) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                )
            }
        }
    }
}
