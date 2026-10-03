package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.platformFormatMonthYear
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_next
import com.xavierclavel.bankable.resources.action_previous
import com.xavierclavel.bankable.resources.batch_duplicate
import com.xavierclavel.bankable.resources.dialog_batch_duplicate_message
import org.jetbrains.compose.resources.stringResource

/**
 * Lets the user pick the month that [count] selected expenses are copied into.
 * Starts on [initialYear]/[initialMonth] (month is 1-12).
 */
@Composable
fun DuplicateExpensesDialog(
    count: Int,
    initialYear: Int,
    initialMonth: Int,
    onConfirm: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Months since year 0, so stepping across a year boundary is plain arithmetic.
    var monthIndex by remember { mutableStateOf(initialYear * 12 + initialMonth - 1) }
    val year = monthIndex / 12
    val month = monthIndex % 12 + 1
    val locale = LocalAppLocale.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.batch_duplicate)) },
        text = {
            Column {
                Text(stringResource(Res.string.dialog_batch_duplicate_message, count))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { monthIndex-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(Res.string.action_previous))
                    }
                    Text(
                        text = platformFormatMonthYear(year, month, locale).replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { monthIndex++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = stringResource(Res.string.action_next))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(year, month) }) {
                Text(stringResource(Res.string.batch_duplicate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
