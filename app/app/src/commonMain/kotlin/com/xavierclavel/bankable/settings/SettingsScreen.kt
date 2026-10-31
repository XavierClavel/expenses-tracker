package com.xavierclavel.bankable.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.platform.applyAppLanguage
import com.xavierclavel.bankable.platform.languageChangeAppliesImmediately
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.app_name
import com.xavierclavel.bankable.resources.cd_delete_account
import com.xavierclavel.bankable.resources.dialog_delete_account_self_message
import com.xavierclavel.bankable.resources.dialog_delete_account_self_title
import com.xavierclavel.bankable.resources.logout
import com.xavierclavel.bankable.resources.settings_account
import com.xavierclavel.bankable.resources.settings_delete_account_hint
import com.xavierclavel.bankable.resources.settings_language
import com.xavierclavel.bankable.resources.settings_language_restart_hint
import com.xavierclavel.bankable.resources.settings_title
import com.xavierclavel.bankable.ui.CardDivider
import com.xavierclavel.bankable.ui.GroupCard
import com.xavierclavel.bankable.ui.RadioDot
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SectionLabel
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onDeleteAccount: (onError: (String) -> Unit) -> Unit,
) {
    var selected by remember { mutableStateOf(LocaleManager.currentLanguage()) }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text(stringResource(Res.string.dialog_delete_account_self_title)) },
            text = {
                Column {
                    Text(stringResource(Res.string.dialog_delete_account_self_message))
                    deleteAccountError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Danger)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteAccountError = null
                    onDeleteAccount { error -> deleteAccountError = error }
                }) {
                    Text(stringResource(Res.string.action_delete), color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(containerColor = NightBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScreenHeader(
                title = stringResource(Res.string.settings_title),
                navigationIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenGutter),
            ) {
                SectionLabel(
                    stringResource(Res.string.settings_language),
                    modifier = Modifier.padding(top = 8.dp, bottom = 10.dp, start = 4.dp),
                )
                GroupCard {
                    AppLanguage.entries.forEachIndexed { index, language ->
                        if (index > 0) CardDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clickable {
                                    if (language != selected) {
                                        selected = language
                                        applyAppLanguage(language.tag)
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(language.labelRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = NightTextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            RadioDot(selected = language == selected)
                        }
                    }
                }
                if (!languageChangeAppliesImmediately) {
                    HelperText(stringResource(Res.string.settings_language_restart_hint))
                }

                SectionLabel(
                    stringResource(Res.string.settings_account),
                    modifier = Modifier.padding(top = 24.dp, bottom = 10.dp, start = 4.dp),
                )
                GroupCard {
                    AccountRow(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        text = stringResource(Res.string.logout),
                        color = NightTextPrimary,
                        iconTint = NightTextSecondary,
                        onClick = onLogout,
                    )
                    CardDivider()
                    AccountRow(
                        icon = Icons.Outlined.Delete,
                        text = stringResource(Res.string.cd_delete_account),
                        color = Danger,
                        iconTint = Danger,
                        onClick = {
                            deleteAccountError = null
                            showDeleteAccountDialog = true
                        },
                    )
                }
                HelperText(stringResource(Res.string.settings_delete_account_hint))
                Spacer(Modifier.height(24.dp))
            }
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextTertiary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 20.dp),
            )
        }
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    text: String,
    color: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun HelperText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = NightTextSecondary,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp),
    )
}
