package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.model.AccountOut
import com.xavierclavel.bankable.model.AccountReportOut
import com.xavierclavel.bankable.model.InvestmentOut
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.prepareEditAccount(account)
                        navController.navigate("account/edit")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_account))
                    }
                }
            )
        },
        floatingActionButton = {
            when (selectedTab) {
                0 -> FloatingActionButton(onClick = {
                    viewModel.prepareNewReport()
                    navController.navigate("account/report/edit")
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_report))
                }
                1 -> FloatingActionButton(onClick = {
                    viewModel.prepareNewTransfer()
                    navController.navigate("account/transfer/edit")
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_transfer))
                }
            }
        }
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
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatAmount(account.amount.toDoubleOrNull() ?: 0.0),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                InterestSummary(account)
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.label_reports)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.label_transfers)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.label_charts)) },
                )
            }

            when (selectedTab) {
                0 -> ReportsTab(reports, viewModel, navController)
                1 -> TransfersTab(transfers, viewModel, navController)
                2 -> AccountChartsScreen(viewModel, accountId = account.id)
            }
        }
    }
}

@Composable
private fun ReportsTab(
    reports: List<AccountReportOut>,
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    if (reports.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_reports_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(reports, key = { it.id }) { report ->
                ReportRow(
                    report = report,
                    onClick = {
                        viewModel.prepareEditReport(report)
                        navController.navigate("account/report/edit")
                    },
                )
            }
        }
    }
}

@Composable
private fun ReportRow(report: AccountReportOut, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatReportDate(report.date, locale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatAmount(report.amount.toDoubleOrNull() ?: 0.0),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val GAIN_COLOR = Color(0xFF4CAF50)
private val LOSS_COLOR = Color(0xFFE53935)

@Composable
private fun InterestSummary(account: AccountOut) {
    // Interest is only meaningful once transfers have been declared; without any
    // net contribution recorded, the whole balance would misleadingly read as gain.
    if ((account.contributions.toDoubleOrNull() ?: 0.0) == 0.0) return
    val info = accountInterest(account.amount, account.contributions)
    val color = if (info.value >= 0.0) GAIN_COLOR else LOSS_COLOR
    val sign = if (info.value > 0.0) "+" else ""
    val percentText = info.percent?.let { " (${if (it > 0.0) "+" else ""}%.1f%%)".format(it) } ?: ""

    Spacer(Modifier.height(4.dp))
    Text(
        text = "${stringResource(R.string.label_interest)}: $sign${formatAmount(info.value)}$percentText",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = color,
    )
    AnnualReturnLabel(
        latestAnnualReturn = account.latestAnnualReturn,
        latestAnnualReturnYear = account.latestAnnualReturnYear,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun TransfersTab(
    transfers: List<InvestmentOut>,
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    if (transfers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_transfers_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(transfers, key = { it.id }) { transfer ->
                TransferRow(
                    transfer = transfer,
                    onClick = {
                        viewModel.prepareEditTransfer(transfer)
                        navController.navigate("account/transfer/edit")
                    },
                )
            }
        }
    }
}

@Composable
private fun TransferRow(transfer: InvestmentOut, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val isIn = transfer.type == TRANSFER_IN
    val color = if (isIn) GAIN_COLOR else LOSS_COLOR
    val sign = if (isIn) "+" else "-"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatReportDate(transfer.date, locale),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$sign${formatAmount(transfer.amount.toDoubleOrNull() ?: 0.0)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

private fun formatReportDate(dateStr: String, locale: Locale): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)!!
        SimpleDateFormat("d MMMM yyyy", locale).format(date)
    } catch (_: Exception) { dateStr }
}
