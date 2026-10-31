package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.AccountType
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.accounts_delete_hint
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
import com.xavierclavel.bankable.ui.DangerButton
import com.xavierclavel.bankable.ui.FormRow
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SectionLabel
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

const val TRACKING_CONTRIBUTIONS = "CONTRIBUTIONS"
const val TRACKING_INTEREST = "INTEREST"

// Savings accounts (Livret A) default to recording known interest; everything else
// records contributions and infers interest.
private fun defaultTracking(typeKey: String) =
    if (typeKey == AccountType.SAVINGS.key) TRACKING_INTEREST else TRACKING_CONTRIBUTIONS

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
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = if (isEditing) stringResource(Res.string.screen_edit_account) else stringResource(Res.string.screen_new_account),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = ScreenGutter),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                NightTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.label_account_name),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = MaterialTheme.typography.titleMedium,
                )

                AccountTypeSelector(
                    selected = AccountType.fromKey(type),
                    onSelect = { type = it.key },
                )

                SectionLabel(
                    text = stringResource(Res.string.label_tracking),
                    modifier = Modifier.padding(top = 14.dp, start = 4.dp),
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
                NightCard(
                    modifier = Modifier.fillMaxWidth(),
                    selected = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Mint,
                            modifier = Modifier.padding(top = 1.dp).size(18.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(
                                if (tracking == TRACKING_INTEREST) Res.string.tracking_interest_hint
                                else Res.string.tracking_contributions_hint
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NightTextPrimary.copy(alpha = 0.85f),
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.action_save),
                    onClick = {
                        viewModel.saveAccount(
                            name = name,
                            type = type,
                            tracking = tracking,
                            onSuccess = { navController.popBackStack() },
                            onError = { errorMessage = it },
                        )
                    },
                    enabled = name.isNotBlank(),
                )
                if (isEditing) {
                    DangerButton(
                        text = stringResource(Res.string.dialog_delete_account_title),
                        onClick = { showDeleteDialog = true },
                        icon = Icons.Outlined.Delete,
                    )
                    Text(
                        text = stringResource(Res.string.accounts_delete_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = NightTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger),
                ) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun AccountTypeSelector(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FormRow(
            label = stringResource(Res.string.label_account_type),
            onClick = { expanded = true },
            showChevron = false,
            trailing = {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = NightTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            },
        ) {
            Icon(selected.icon, contentDescription = null, tint = selected.accentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(selected.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = NightTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AccountType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.labelRes)) },
                    leadingIcon = { Icon(type.icon, contentDescription = null, tint = type.accentColor) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    },
                )
            }
        }
    }
}
