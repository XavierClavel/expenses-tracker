package com.xavierclavel.bankable.expenses

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.batch_make_recurring
import com.xavierclavel.bankable.resources.dialog_batch_make_recurring_message
import org.jetbrains.compose.resources.stringResource

/** Confirms turning [count] selected expenses into recurring expenses. */
@Composable
fun MakeRecurringDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.batch_make_recurring)) },
        text = { Text(stringResource(Res.string.dialog_batch_make_recurring_message, count)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.batch_make_recurring))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
