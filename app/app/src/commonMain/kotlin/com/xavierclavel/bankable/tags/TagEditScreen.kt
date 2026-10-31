package com.xavierclavel.bankable.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.categories.EditActions
import com.xavierclavel.bankable.categories.EditPreviewTile
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.dialog_confirm_delete_title
import com.xavierclavel.bankable.resources.dialog_delete_tag_message
import com.xavierclavel.bankable.resources.label_tag_label
import com.xavierclavel.bankable.resources.screen_edit_tag
import com.xavierclavel.bankable.resources.screen_new_tag
import com.xavierclavel.bankable.resources.tag_delete
import com.xavierclavel.bankable.resources.tag_edit_summary
import com.xavierclavel.bankable.resources.tag_rename_hint
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.TagPill
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun TagEditScreen(
    viewModel: TagsViewModel,
    navController: NavController,
) {
    val isEditing = viewModel.selectedTag != null
    var label by remember { mutableStateOf(viewModel.selectedTag?.label ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val locale = LocalAppLocale.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.dialog_confirm_delete_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_tag_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTag(
                        onSuccess = { navController.popBackStack("tags", false) },
                        onError = { e -> error = e },
                    )
                }) {
                    Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(if (isEditing) Res.string.screen_edit_tag else Res.string.screen_new_tag),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
        },
        bottomBar = {
            EditActions(
                error = error,
                onSave = {
                    viewModel.saveTag(
                        label = label,
                        onSuccess = { navController.popBackStack() },
                        onError = { e -> error = e },
                    )
                },
                saveEnabled = label.isNotBlank(),
                deleteText = if (isEditing) stringResource(Res.string.tag_delete) else null,
                onDelete = { showDeleteDialog = true },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (label.isNotBlank()) {
                EditPreviewTile(
                    icon = { TagPill(text = label.trim(), icon = Icons.Outlined.Sell) },
                    caption = viewModel.selectedTag?.let { tag ->
                        stringResource(
                            Res.string.tag_edit_summary,
                            tag.expenseCount,
                            "${formatAmountDisplay(tag.total, locale)} ${currencySymbol("EUR")}",
                        )
                    },
                )
            }

            NightTextField(
                value = label,
                onValueChange = { label = it },
                label = stringResource(Res.string.label_tag_label),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.titleMedium,
            )

            if (isEditing) {
                Text(
                    text = stringResource(Res.string.tag_rename_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NightTextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}
