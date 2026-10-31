package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.AccountType
import com.xavierclavel.bankable.model.AccountOut
import com.xavierclavel.bankable.model.AccountReportOut
import com.xavierclavel.bankable.model.InvestmentOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.showToast
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.accounts_net_contributions
import com.xavierclavel.bankable.resources.accounts_net_interest
import com.xavierclavel.bankable.resources.accounts_since_last_report
import com.xavierclavel.bankable.resources.accounts_subtitle_contributions
import com.xavierclavel.bankable.resources.accounts_subtitle_interest
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.cd_add_report
import com.xavierclavel.bankable.resources.cd_add_transfer
import com.xavierclavel.bankable.resources.cd_edit_account
import com.xavierclavel.bankable.resources.label_charts
import com.xavierclavel.bankable.resources.label_interest
import com.xavierclavel.bankable.resources.label_reports
import com.xavierclavel.bankable.resources.label_transfers
import com.xavierclavel.bankable.resources.no_reports_yet
import com.xavierclavel.bankable.resources.no_transfers_yet
import com.xavierclavel.bankable.resources.transfer_deposit
import com.xavierclavel.bankable.resources.transfer_fee
import com.xavierclavel.bankable.resources.transfer_interest
import com.xavierclavel.bankable.resources.transfer_withdrawal
import com.xavierclavel.bankable.ui.CheckDot
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.ConfirmDeleteDialog
import com.xavierclavel.bankable.ui.DangerButton
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightFab
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SelectionActionBar
import com.xavierclavel.bankable.ui.SelectionController
import com.xavierclavel.bankable.ui.SelectionHeaderRow
import com.xavierclavel.bankable.ui.UnderlineTabs
import com.xavierclavel.bankable.ui.rememberSelectionController
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.ui.theme.TransferBlue
import com.xavierclavel.bankable.util.formatIsoDateLong
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountViewScreen(
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    val selected = viewModel.selectedAccount ?: return
    val reports by viewModel.reports.collectAsState()
    val transfers by viewModel.transfers.collectAsState()
    // Track the up-to-date copy from the accounts list so the balance header
    // reflects a newly added/edited report.
    val accounts by viewModel.accounts.collectAsState()
    val account = remember(accounts, selected.id) {
        accounts.find { it.id == selected.id } ?: selected
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    // Held here (not inside the tabs) so the add-FAB can hide while selecting.
    val reportsSelection = rememberSelectionController<Int>()
    val transfersSelection = rememberSelectionController<Long>()

    val typeLabel = stringResource(AccountType.fromKey(account.type).labelRes)

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = account.name,
                subtitle = stringResource(
                    if (account.tracking == TRACKING_INTEREST) Res.string.accounts_subtitle_interest
                    else Res.string.accounts_subtitle_contributions,
                    typeLabel,
                ),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
                actions = {
                    CircleIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(Res.string.cd_edit_account),
                        onClick = {
                            viewModel.prepareEditAccount(account)
                            navController.navigate("account/edit")
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> if (!reportsSelection.active) NightFab(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.cd_add_report),
                    onClick = {
                        viewModel.prepareNewReport()
                        navController.navigate("account/report/edit")
                    },
                )
                1 -> if (!transfersSelection.active) NightFab(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.cd_add_transfer),
                    onClick = {
                        viewModel.prepareNewTransfer()
                        navController.navigate("account/transfer/edit")
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Balance header + accrued interest
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatAmount(account.amount.toDoubleOrNull() ?: 0.0),
                    style = MaterialTheme.typography.displayLarge,
                    color = Mint,
                    maxLines = 1,
                )
                InterestSummary(account)
            }

            UnderlineTabs(
                tabs = listOf(
                    stringResource(Res.string.label_reports),
                    stringResource(
                        if (account.tracking == TRACKING_INTEREST) Res.string.label_interest
                        else Res.string.label_transfers
                    ),
                    stringResource(Res.string.label_charts),
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = ScreenGutter).padding(top = 12.dp),
            )

            when (selectedTab) {
                0 -> ReportsTab(reports, viewModel, reportsSelection, navController)
                1 -> TransfersTab(account, transfers, viewModel, transfersSelection, navController)
                2 -> AccountChartsScreen(viewModel, accountId = account.id)
            }
        }
    }
}

@Composable
private fun ReportsTab(
    reports: List<AccountReportOut>,
    viewModel: AccountsViewModel,
    selection: SelectionController<Int>,
    navController: NavController,
) {
    var showConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selection.active) { selection.clear() }

    if (showConfirm) {
        ConfirmDeleteDialog(
            count = selection.selectedIds.size,
            onConfirm = {
                showConfirm = false
                viewModel.batchDeleteReports(
                    selection.selectedIds.toList(),
                    onError = { showToast(it) },
                )
                selection.clear()
            },
            onDismiss = { showConfirm = false },
        )
    }

    // Each report's change since the one before it (by date); the oldest has none.
    val chronological = remember(reports) { reports.sortedByDescending { it.date } }
    val deltas = remember(chronological) {
        chronological.zipWithNext { newer, older ->
            newer.id to (newer.amount.toDoubleOrNull() ?: 0.0) - (older.amount.toDoubleOrNull() ?: 0.0)
        }.toMap()
    }
    val latestId = chronological.firstOrNull()?.id

    if (reports.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_reports_yet), color = NightTextSecondary)
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            if (selection.active) {
                SelectionHeaderRow(selection.selectedIds.size) { selection.clear() }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = ScreenGutter, top = 14.dp, end = ScreenGutter, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(reports, key = { it.id }) { report ->
                    ReportRow(
                        report = report,
                        delta = deltas[report.id],
                        isLatest = report.id == latestId,
                        selected = selection.selectedIds.contains(report.id),
                        selecting = selection.active,
                        onClick = {
                            if (selection.active) {
                                selection.toggle(report.id)
                            } else {
                                viewModel.prepareEditReport(report)
                                navController.navigate("account/report/edit")
                            }
                        },
                        onLongClick = { selection.enter(report.id) },
                    )
                }
            }
            if (selection.active) {
                DeleteSelectionBar(enabled = selection.selectedIds.isNotEmpty()) { showConfirm = true }
            }
        }
    }
}

@Composable
private fun DeleteSelectionBar(enabled: Boolean, onDelete: () -> Unit) {
    SelectionActionBar {
        DangerButton(
            text = stringResource(Res.string.action_delete),
            onClick = onDelete,
            icon = Icons.Outlined.Delete,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReportRow(
    report: AccountReportOut,
    delta: Double?,
    isLatest: Boolean,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val locale = LocalAppLocale.current
    NightCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selecting) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { CheckDot(selected) }
            } else {
                IconTile(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    color = if (isLatest) Mint else NightTextSecondary,
                    size = 40.dp,
                    iconSize = 20.dp,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = formatReportDate(report.date, locale),
                    style = MaterialTheme.typography.titleSmall,
                    color = NightTextPrimary,
                )
                if (delta != null) {
                    val deltaText = signedAmountWithPercent(delta, null)
                    Text(
                        text = if (isLatest) stringResource(Res.string.accounts_since_last_report, deltaText) else deltaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = gainColor(delta),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatAmount(report.amount.toDoubleOrNull() ?: 0.0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = NightTextPrimary,
            )
        }
    }
}

@Composable
private fun InterestSummary(account: AccountOut) {
    // Interest is only meaningful once transfers have been declared; without any
    // net contribution recorded, the whole balance would misleadingly read as gain.
    if ((account.contributions.toDoubleOrNull() ?: 0.0) == 0.0) return
    val info = accountInterest(account.amount, account.contributions)

    Spacer(Modifier.height(4.dp))
    Text(
        text = "${stringResource(Res.string.label_interest)}: ${signedAmountWithPercent(info.value, info.percent)}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = gainColor(info.value),
    )
    AnnualReturnLabel(
        latestAnnualReturn = account.latestAnnualReturn,
        latestAnnualReturnYear = account.latestAnnualReturnYear,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun TransfersTab(
    account: AccountOut,
    transfers: List<InvestmentOut>,
    viewModel: AccountsViewModel,
    selection: SelectionController<Long>,
    navController: NavController,
) {
    var showConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selection.active) { selection.clear() }

    if (showConfirm) {
        ConfirmDeleteDialog(
            count = selection.selectedIds.size,
            onConfirm = {
                showConfirm = false
                viewModel.batchDeleteTransfers(
                    selection.selectedIds.toList(),
                    onError = { showToast(it) },
                )
                selection.clear()
            },
            onDismiss = { showConfirm = false },
        )
    }

    if (transfers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_transfers_yet), color = NightTextSecondary)
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            if (selection.active) {
                SelectionHeaderRow(selection.selectedIds.size) { selection.clear() }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = ScreenGutter, top = 14.dp, end = ScreenGutter, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "net") {
                    // Contributions accounts sum their deposits; interest accounts sum what
                    // the balance earned beyond them.
                    val interestTracked = account.tracking == TRACKING_INTEREST
                    val net = if (interestTracked) accountInterest(account.amount, account.contributions).value
                    else account.contributions.toDoubleOrNull() ?: 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                if (interestTracked) Res.string.accounts_net_interest
                                else Res.string.accounts_net_contributions
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NightTextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatAmount(net),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NightTextPrimary,
                        )
                    }
                }
                items(transfers, key = { it.id }) { transfer ->
                    TransferRow(
                        transfer = transfer,
                        selected = selection.selectedIds.contains(transfer.id),
                        selecting = selection.active,
                        onClick = {
                            if (selection.active) {
                                selection.toggle(transfer.id)
                            } else {
                                viewModel.prepareEditTransfer(transfer)
                                navController.navigate("account/transfer/edit")
                            }
                        },
                        onLongClick = { selection.enter(transfer.id) },
                    )
                }
            }
            if (selection.active) {
                DeleteSelectionBar(enabled = selection.selectedIds.isNotEmpty()) { showConfirm = true }
            }
        }
    }
}

// How a transfer type reads in the list: its tile icon, accent color and label.
private data class TransferLook(val icon: ImageVector, val color: Color, val label: org.jetbrains.compose.resources.StringResource)

private fun transferLook(type: String): TransferLook = when (type) {
    TRANSFER_IN       -> TransferLook(Icons.Outlined.ArrowDownward, TransferBlue, Res.string.transfer_deposit)
    TRANSFER_INTEREST -> TransferLook(Icons.Outlined.ArrowDownward, Mint, Res.string.transfer_interest)
    TRANSFER_FEE      -> TransferLook(Icons.Outlined.ArrowUpward, Danger, Res.string.transfer_fee)
    else              -> TransferLook(Icons.Outlined.ArrowUpward, Danger, Res.string.transfer_withdrawal)
}

@Composable
private fun TransferRow(
    transfer: InvestmentOut,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val locale = LocalAppLocale.current
    val positive = transfer.type == TRANSFER_IN || transfer.type == TRANSFER_INTEREST
    val sign = if (positive) "+" else "−"
    val look = transferLook(transfer.type)
    NightCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selecting) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { CheckDot(selected) }
            } else {
                IconTile(icon = look.icon, color = look.color, size = 40.dp, iconSize = 20.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(look.label),
                    style = MaterialTheme.typography.titleSmall,
                    color = NightTextPrimary,
                )
                Text(
                    text = formatReportDate(transfer.date, locale),
                    style = MaterialTheme.typography.bodySmall,
                    color = NightTextTertiary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$sign${formatAmount(transfer.amount.toDoubleOrNull() ?: 0.0)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = look.color,
            )
        }
    }
}

private fun formatReportDate(dateStr: String, locale: String): String =
    formatIsoDateLong(dateStr, locale)
