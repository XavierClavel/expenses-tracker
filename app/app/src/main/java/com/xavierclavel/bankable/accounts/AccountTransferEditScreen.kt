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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.ui.SlidingToggle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val TRANSFER_IN = "IN"
const val TRANSFER_OUT = "OUT"
const val TRANSFER_INTEREST = "INTEREST"
const val TRANSFER_FEE = "FEE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransferEditScreen(
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    val transfer = viewModel.selectedTransfer
    val isEditing = transfer != null
    val interestMode = (viewModel.selectedAccount?.tracking ?: TRACKING_CONTRIBUTIONS) == TRACKING_INTEREST

    var amount by rememberSaveable { mutableStateOf(transfer?.amount ?: "") }
    var type by rememberSaveable {
        mutableStateOf(transfer?.type ?: if (interestMode) TRANSFER_INTEREST else TRANSFER_IN)
    }
    var date by rememberSaveable { mutableStateOf(transfer?.date ?: transferTodayString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transferDateToMillis(date))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) stringResource(R.string.screen_edit_transfer) else stringResource(R.string.screen_new_transfer)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
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

            SlidingToggle(
                options = if (interestMode) listOf(
                    TRANSFER_INTEREST to stringResource(R.string.transfer_interest),
                    TRANSFER_FEE to stringResource(R.string.transfer_fee),
                ) else listOf(
                    TRANSFER_IN to stringResource(R.string.transfer_deposit),
                    TRANSFER_OUT to stringResource(R.string.transfer_withdrawal),
                ),
                selected = type,
                onSelect = { type = it },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.label_amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            val locale = LocalConfiguration.current.locales[0]
            OutlinedTextField(
                value = transferFormatDateDisplay(date, locale),
                onValueChange = {},
                label = { Text(stringResource(R.string.label_date)) },
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
                    viewModel.saveTransfer(
                        amount = amount,
                        type = type,
                        date = date,
                        onSuccess = { navController.popBackStack() },
                        onError = { errorMessage = it },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = transferMillisToDate(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_transfer_title)) },
            text = { Text(stringResource(R.string.dialog_delete_transfer_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransfer(
                            onSuccess = { navController.popBackStack() },
                            onError = { errorMessage = it },
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun transferTodayString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

private fun transferFormatDateDisplay(dateStr: String, locale: Locale): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: return dateStr
        SimpleDateFormat("d MMMM yyyy", locale).format(date)
    } catch (_: Exception) { dateStr }
}

private fun transferDateToMillis(dateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) { System.currentTimeMillis() }
}

private fun transferMillisToDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}
