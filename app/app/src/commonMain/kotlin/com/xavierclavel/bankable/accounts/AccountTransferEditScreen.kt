package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.accounts_amount
import com.xavierclavel.bankable.resources.accounts_net_contributions_after
import com.xavierclavel.bankable.resources.accounts_save_transfer
import com.xavierclavel.bankable.resources.accounts_transfer_hint_contributions
import com.xavierclavel.bankable.resources.accounts_transfer_hint_interest
import com.xavierclavel.bankable.resources.accounts_transfer_subtitle
import com.xavierclavel.bankable.resources.accounts_transfer_subtitle_interest
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.action_ok
import com.xavierclavel.bankable.resources.dialog_delete_transfer_message
import com.xavierclavel.bankable.resources.dialog_delete_transfer_title
import com.xavierclavel.bankable.resources.label_date
import com.xavierclavel.bankable.resources.screen_edit_transfer
import com.xavierclavel.bankable.resources.screen_new_transfer
import com.xavierclavel.bankable.resources.transfer_deposit
import com.xavierclavel.bankable.resources.transfer_fee
import com.xavierclavel.bankable.resources.transfer_interest
import com.xavierclavel.bankable.resources.transfer_withdrawal
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.DangerBorder
import com.xavierclavel.bankable.ui.theme.DangerContainer
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.MintContainer
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightCardRaised
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.TransferBlue
import com.xavierclavel.bankable.ui.theme.TransferBlueContainer
import com.xavierclavel.bankable.util.formatIsoDateLong
import com.xavierclavel.bankable.util.isoDateToUtcMillis
import com.xavierclavel.bankable.util.todayIsoDate
import com.xavierclavel.bankable.util.utcMillisToIsoDate
import org.jetbrains.compose.resources.stringResource

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
    // The up-to-date copy of the account, for its current net contributions.
    val accounts by viewModel.accounts.collectAsState()
    val account = accounts.find { it.id == viewModel.selectedAccount?.id } ?: viewModel.selectedAccount

    var amount by rememberSaveable { mutableStateOf(transfer?.amount ?: "") }
    var type by rememberSaveable {
        mutableStateOf(transfer?.type ?: if (interestMode) TRANSFER_INTEREST else TRANSFER_IN)
    }
    var date by rememberSaveable { mutableStateOf(transfer?.date ?: transferTodayString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transferDateToMillis(date))
    val locale = LocalAppLocale.current

    val positive = type == TRANSFER_IN || type == TRANSFER_INTEREST
    val (accent, accentContainer) = when (type) {
        TRANSFER_IN       -> TransferBlue to TransferBlueContainer
        TRANSFER_INTEREST -> Mint to MintContainer
        TRANSFER_FEE      -> Danger to DangerContainer
        else              -> NightTextPrimary to NightCardRaised
    }

    // What the account's net contributions become once this transfer is saved
    // (replacing the edited transfer's own contribution). Contributions accounts only.
    val netAfterSaving: Double? = run {
        if (interestMode) return@run null
        val entered = parseAmountInput(amount) ?: return@run null
        val current = account?.contributions?.toDoubleOrNull() ?: return@run null
        fun signed(t: String, v: Double) = if (t == TRANSFER_OUT) -v else v
        val previous = transfer?.let { signed(it.type, it.amount.toDoubleOrNull() ?: 0.0) } ?: 0.0
        current - previous + signed(type, entered)
    }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = if (isEditing) stringResource(Res.string.screen_edit_transfer) else stringResource(Res.string.screen_new_transfer),
                subtitle = account?.let {
                    stringResource(
                        if (interestMode) Res.string.accounts_transfer_subtitle_interest
                        else Res.string.accounts_transfer_subtitle,
                        it.name,
                    )
                },
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
                Spacer(Modifier.height(4.dp))

                SlidingToggle(
                    options = if (interestMode) listOf(
                        TRANSFER_INTEREST to "↓ ${stringResource(Res.string.transfer_interest)}",
                        TRANSFER_FEE to "↑ ${stringResource(Res.string.transfer_fee)}",
                    ) else listOf(
                        TRANSFER_IN to "↓ ${stringResource(Res.string.transfer_deposit)}",
                        TRANSFER_OUT to "↑ ${stringResource(Res.string.transfer_withdrawal)}",
                    ),
                    selected = type,
                    onSelect = { type = it },
                    modifier = Modifier.fillMaxWidth(),
                    selectedColor = accent,
                    pillColor = accentContainer,
                )

                Spacer(Modifier.height(10.dp))
                AmountHeroField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = stringResource(Res.string.accounts_amount),
                    sign = if (positive) "+" else "−",
                    signColor = if (type == TRANSFER_OUT) Danger else accent,
                )
                if (netAfterSaving != null) {
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(Res.string.accounts_net_contributions_after))
                            append(" ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = NightTextPrimary)) {
                                append(formatAmount(netAfterSaving))
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NightTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(8.dp))

                DateFormRow(
                    label = stringResource(Res.string.label_date),
                    text = transferFormatDateDisplay(date, locale),
                    onClick = { showDatePicker = true },
                )

                Text(
                    text = stringResource(
                        if (interestMode) Res.string.accounts_transfer_hint_interest
                        else Res.string.accounts_transfer_hint_contributions
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = NightTextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
                text = stringResource(Res.string.accounts_save_transfer),
                onClick = {
                    viewModel.saveTransfer(
                        amount = amount,
                        type = type,
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
                    datePickerState.selectedDateMillis?.let { date = transferMillisToDate(it) }
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
            title = { Text(stringResource(Res.string.dialog_delete_transfer_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_transfer_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransfer(
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

private fun transferTodayString(): String = todayIsoDate()

private fun transferFormatDateDisplay(dateStr: String, locale: String): String =
    formatIsoDateLong(dateStr, locale)

private fun transferDateToMillis(dateStr: String): Long = isoDateToUtcMillis(dateStr)

private fun transferMillisToDate(millis: Long): String = utcMillisToIsoDate(millis)
