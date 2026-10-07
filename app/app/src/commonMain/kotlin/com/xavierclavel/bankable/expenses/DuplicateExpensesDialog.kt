package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.batch_duplicate
import com.xavierclavel.bankable.resources.dialog_batch_duplicate_message
import com.xavierclavel.bankable.util.isoDateToUtcMillis
import com.xavierclavel.bankable.util.utcMillisToIsoDate
import org.jetbrains.compose.resources.stringResource

/**
 * Lets the user pick the date that [count] selected expenses are copied onto.
 * Starts on [initialDate] ("yyyy-MM-dd").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateExpensesDialog(
    count: Int,
    initialDate: String,
    onConfirm: (date: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = isoDateToUtcMillis(initialDate),
    )
    val selectedMillis = datePickerState.selectedDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { selectedMillis?.let { onConfirm(utcMillisToIsoDate(it)) } },
                enabled = selectedMillis != null,
            ) {
                Text(stringResource(Res.string.batch_duplicate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(Res.string.dialog_batch_duplicate_message, count),
                    // Material's default DatePicker title padding.
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
        )
    }
}
