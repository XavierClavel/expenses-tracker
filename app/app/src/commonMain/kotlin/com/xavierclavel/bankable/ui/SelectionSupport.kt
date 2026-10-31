package com.xavierclavel.bankable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.batch_assign_tag
import com.xavierclavel.bankable.resources.batch_remove_tag
import com.xavierclavel.bankable.resources.batch_selected_count
import com.xavierclavel.bankable.resources.cd_exit_selection
import com.xavierclavel.bankable.resources.dialog_batch_delete_message
import com.xavierclavel.bankable.resources.dialog_confirm_delete_title
import com.xavierclavel.bankable.resources.tags_empty
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import org.jetbrains.compose.resources.stringResource

/**
 * Screen-local multi-select state. Hold it with [rememberSelectionController].
 * Selection is entered by long-pressing an item; emptying it exits the mode.
 */
class SelectionController<ID> {
    var active by mutableStateOf(false)
        private set
    var selectedIds by mutableStateOf<Set<ID>>(emptySet())
        private set

    fun enter(id: ID) {
        active = true
        selectedIds = setOf(id)
    }

    fun toggle(id: ID) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) active = false
    }

    fun clear() {
        active = false
        selectedIds = emptySet()
    }
}

@Composable
fun <ID> rememberSelectionController(): SelectionController<ID> = remember { SelectionController() }

/** Top row shown while selecting: a close button and the selected-item count. */
@Composable
fun SelectionHeaderRow(count: Int, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NightBackground)
            .padding(horizontal = ScreenGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(Res.string.cd_exit_selection),
            onClick = onClose,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(Res.string.batch_selected_count, count),
            style = MaterialTheme.typography.headlineSmall,
            color = NightTextPrimary,
        )
    }
}

/** Bottom action bar shown while selecting; place the action buttons via [content]. */
@Composable
fun SelectionActionBar(content: @Composable RowScope.() -> Unit) {
    Column(Modifier.background(NightBackground)) {
        CardDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Confirmation dialog for a batch deletion of [count] items. */
@Composable
fun ConfirmDeleteDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_confirm_delete_title)) },
        text = { Text(stringResource(Res.string.dialog_batch_delete_message, count)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.action_delete), color = Danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

/** Lets the user pick a tag to add ([add] = true) or remove ([add] = false) in batch. */
@Composable
fun TagPickerDialog(
    add: Boolean,
    tags: List<TagOut>,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (add) Res.string.batch_assign_tag else Res.string.batch_remove_tag))
        },
        text = {
            if (tags.isEmpty()) {
                Text(stringResource(Res.string.tags_empty))
            } else {
                Column {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(tag.id) }
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Sell,
                                contentDescription = null,
                                tint = Mint,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = tag.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = NightTextPrimary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
