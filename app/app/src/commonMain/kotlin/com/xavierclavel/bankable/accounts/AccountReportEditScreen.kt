package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.action_ok
import com.xavierclavel.bankable.resources.action_save
import com.xavierclavel.bankable.resources.dialog_delete_report_message
import com.xavierclavel.bankable.resources.dialog_delete_report_title
import com.xavierclavel.bankable.resources.label_amount
import com.xavierclavel.bankable.resources.label_date
import com.xavierclavel.bankable.resources.screen_edit_report
import com.xavierclavel.bankable.resources.screen_new_report
import com.xavierclavel.bankable.util.formatIsoDateLong
import com.xavierclavel.bankable.util.isoDateToUtcMillis
import com.xavierclavel.bankable.util.todayIsoDate
import com.xavierclavel.bankable.util.utcMillisToIsoDate
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountReportEditScreen(
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    val report = viewModel.selectedReport
    val isEditing = report != null

    var amount by rememberSaveable { mutableStateOf(report?.amount ?: "") }
    var date by rememberSaveable { mutableStateOf(report?.date ?: todayString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateToMillis(date))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) stringResource(Res.string.screen_edit_report) else stringResource(Res.string.screen_new_report)) },
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
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(Res.string.label_amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            val locale = LocalAppLocale.current
            OutlinedTextField(
                value = formatDateDisplay(date, locale),
                onValueChange = {},
                label = { Text(stringResource(Res.string.label_date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor          = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor        = MaterialTheme.colorScheme.outline,
                    disabledLabelColor         = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
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
                    viewModel.saveReport(
                        amount = amount,
                        date = date,
                        onSuccess = { navController.popBackStack() },
                        onError = { errorMessage = it },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank(),
            ) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = millisToDate(it) }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.dialog_delete_report_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_report_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteReport(
                            onSuccess = { navController.popBackStack() },
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

private fun todayString(): String = todayIsoDate()

private fun formatDateDisplay(dateStr: String, locale: String): String =
    formatIsoDateLong(dateStr, locale)

private fun dateToMillis(dateStr: String): Long = isoDateToUtcMillis(dateStr)

private fun millisToDate(millis: Long): String = utcMillisToIsoDate(millis)
