package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.accounts_report_subtitle
import com.xavierclavel.bankable.resources.accounts_save_report
import com.xavierclavel.bankable.resources.accounts_since_date
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.action_ok
import com.xavierclavel.bankable.resources.dialog_delete_report_message
import com.xavierclavel.bankable.resources.dialog_delete_report_title
import com.xavierclavel.bankable.resources.label_balance
import com.xavierclavel.bankable.resources.label_date
import com.xavierclavel.bankable.resources.screen_edit_report
import com.xavierclavel.bankable.resources.screen_new_report
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.FormRow
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.DangerBorder
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
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
    val reports by viewModel.reports.collectAsState()

    var amount by rememberSaveable { mutableStateOf(report?.amount ?: "") }
    var date by rememberSaveable { mutableStateOf(report?.date ?: todayString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateToMillis(date))
    val locale = LocalAppLocale.current

    // The report this one follows (the latest other report on or before its date),
    // to show how much the balance moved since then.
    val previous = remember(reports, date, report?.id) {
        reports.filter { it.id != report?.id && it.date <= date }.maxByOrNull { it.date }
    }
    val enteredAmount = parseAmountInput(amount)

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = if (isEditing) stringResource(Res.string.screen_edit_report) else stringResource(Res.string.screen_new_report),
                subtitle = viewModel.selectedAccount?.let { stringResource(Res.string.accounts_report_subtitle, it.name) },
                navigationIcon = Icons.Default.Close,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
                actions = {
                    if (isEditing) {
                        CircleIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = stringResource(Res.string.action_delete),
                            onClick = { showDeleteDialog = true },
                            tint = Danger,
                            borderColor = DangerBorder,
                        )
                    }
                },
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
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                AmountHeroField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = stringResource(Res.string.label_balance),
                )
                if (previous != null && enteredAmount != null) {
                    val delta = enteredAmount - (previous.amount.toDoubleOrNull() ?: 0.0)
                    Text(
                        text = stringResource(
                            Res.string.accounts_since_date,
                            signedAmountWithPercent(delta, null),
                            formatDateDisplay(previous.date, locale),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = gainColor(delta),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(8.dp))

                DateFormRow(
                    label = stringResource(Res.string.label_date),
                    text = formatDateDisplay(date, locale),
                    onClick = { showDatePicker = true },
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            PrimaryButton(
                text = stringResource(Res.string.accounts_save_report),
                onClick = {
                    viewModel.saveReport(
                        amount = amount,
                        date = date,
                        onSuccess = { navController.popBackStack() },
                        onError = { errorMessage = it },
                    )
                },
                enabled = amount.isNotBlank(),
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
            )
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Danger),
                ) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

/**
 * The big centred amount entry of the report/transfer forms: a tracked label, the
 * amount in display type with a muted "€", and an optional colored sign before it.
 */
@Composable
internal fun AmountHeroField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    sign: String? = null,
    signColor: Color = NightTextSecondary,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = NightTextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (sign != null) {
                Text(sign, style = MaterialTheme.typography.headlineMedium, color = signColor)
                Spacer(Modifier.width(10.dp))
            }
            val style = MaterialTheme.typography.displayLarge
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = style.merge(TextStyle(color = NightTextPrimary, textAlign = TextAlign.Center)),
                cursorBrush = SolidColor(Mint),
                modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 48.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) {
                            Text("0", style = style, color = NightTextTertiary, textAlign = TextAlign.Center)
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
            Text("€", style = MaterialTheme.typography.headlineMedium, color = NightTextTertiary)
        }
    }
}

/** A tappable "Date" form row showing the chosen date and a calendar icon. */
@Composable
internal fun DateFormRow(label: String, text: String, onClick: () -> Unit) {
    FormRow(
        label = label,
        onClick = onClick,
        showChevron = false,
        labelWidth = 72.dp,
        trailing = {
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = NightTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = NightTextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Parses what the user typed in an amount field, accepting a comma decimal separator. */
internal fun parseAmountInput(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

private fun todayString(): String = todayIsoDate()

private fun formatDateDisplay(dateStr: String, locale: String): String =
    formatIsoDateLong(dateStr, locale)

private fun dateToMillis(dateStr: String): Long = isoDateToUtcMillis(dateStr)

private fun millisToDate(millis: Long): String = utcMillisToIsoDate(millis)
