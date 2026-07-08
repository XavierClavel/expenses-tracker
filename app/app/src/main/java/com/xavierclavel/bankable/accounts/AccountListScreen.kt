package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.constants.AccountType
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.model.AccountOut

@Composable
fun AccountListScreen(
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    val accounts by viewModel.accounts.collectAsState()
    val userYearTrends by viewModel.userYearTrends.collectAsState()
    val isLoading = viewModel.isLoading
    val total = accounts.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

    // Total accrued interest, summed only over accounts that have declared transfers
    // (otherwise their whole balance would count as interest). Null when none do.
    val withContributions = accounts.filter { (it.contributions.toDoubleOrNull() ?: 0.0) > 0.0 }
    val totalContributions = withContributions.sumOf { it.contributions.toDoubleOrNull() ?: 0.0 }
    val totalInterest =
        if (withContributions.isEmpty()) null
        else withContributions.sumOf { (it.amount.toDoubleOrNull() ?: 0.0) - (it.contributions.toDoubleOrNull() ?: 0.0) }

    // Interest earned across all accounts during the latest year (year-to-date for the
    // current year): this year's cumulative interest minus last year's, over last
    // year's balance. Uses the all-accounts yearly trends. Null when not computable.
    val currentYearInterest: CurrentYearInterest? = remember(userYearTrends, totalInterest) {
        if (userYearTrends.size < 2 || totalInterest == null) return@remember null
        val last = userYearTrends.last()
        val prev = userYearTrends[userYearTrends.size - 2]
        fun interestOf(t: com.xavierclavel.bankable.model.AccountTrendDto) =
            (t.balance.toDoubleOrNull() ?: 0.0) - (t.contributions?.toDoubleOrNull() ?: 0.0)
        val gain = interestOf(last) - interestOf(prev)
        // Percentage is the backend's Modified-Dietz return (transfers weighted by
        // date), so mid-year deposits don't inflate the rate.
        val pct = last.returnRate?.toDoubleOrNull()?.times(100.0)
        CurrentYearInterest(last.year, gain, pct)
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = {
                    viewModel.prepareNewAccount()
                    navController.navigate("account/edit")
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_account))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Total balance header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.label_total_balance),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatAmount(total),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (totalInterest != null) {
                    val pct = if (totalContributions > 0.0) totalInterest / totalContributions * 100.0 else null
                    val sign = if (totalInterest > 0.0) "+" else ""
                    val pctText = pct?.let { " (${if (it > 0.0) "+" else ""}%.1f%%)".format(it) } ?: ""
                    Text(
                        text = "${stringResource(R.string.label_interest)}: $sign${formatAmount(totalInterest)}$pctText",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (totalInterest >= 0.0) GAIN else LOSS,
                    )
                }
                if (currentYearInterest != null) {
                    val sign = if (currentYearInterest.gain > 0.0) "+" else ""
                    val pctText = currentYearInterest.percent
                        ?.let { " (${if (it > 0.0) "+" else ""}%.1f%%)".format(it) } ?: ""
                    Text(
                        text = "${currentYearInterest.year}: $sign${formatAmount(currentYearInterest.gain)}$pctText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentYearInterest.gain >= 0.0) GAIN else LOSS,
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_balance)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.label_distribution)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.label_charts)) },
                )
            }

            when (selectedTab) {
                0 -> BalanceTab(accounts, isLoading, viewModel, navController)
                1 -> AccountDistributionScreen(viewModel)
                2 -> AccountChartsScreen(viewModel, accountId = null)
            }
        }
    }
}

@Composable
private fun BalanceTab(
    accounts: List<AccountOut>,
    isLoading: Boolean,
    viewModel: AccountsViewModel,
    navController: NavController,
) {
    if (isLoading && accounts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (accounts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_accounts_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        val grouped = AccountType.entries.mapNotNull { type ->
            val group = accounts.filter { AccountType.fromKey(it.type) == type }
            if (group.isEmpty()) null else type to group
        }
        LazyColumn(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            grouped.forEach { (type, group) ->
                item(key = "header_${type.key}") { AccountTypeHeader(type) }
                items(group, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        onClick = {
                            viewModel.selectAccount(account)
                            navController.navigate("account/view")
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AccountTypeHeader(type: AccountType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = type.accentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(type.labelRes),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = type.accentColor,
        )
    }
}

@Composable
private fun AccountRow(account: AccountOut, onClick: () -> Unit) {
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
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatAmount(account.amount.toDoubleOrNull() ?: 0.0),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

internal fun formatAmount(value: Double): String = "${formatRoundedAmount(value)} €"

// Accrued interest for an account: the balance beyond what was put in via transfers.
// `percent` is the return relative to net contributions, or null when contributions
// aren't positive (so a percentage would be meaningless).
internal data class InterestInfo(val value: Double, val percent: Double?)

// Interest earned across all accounts during a single year (the latest year in the
// yearly trends): the € gained and the return relative to the prior year's balance.
internal data class CurrentYearInterest(val year: Int, val gain: Double, val percent: Double?)

internal fun accountInterest(amount: String, contributions: String): InterestInfo {
    val balance = amount.toDoubleOrNull() ?: 0.0
    val contrib = contributions.toDoubleOrNull() ?: 0.0
    val interest = balance - contrib
    val percent = if (contrib > 0.0) interest / contrib * 100.0 else null
    return InterestInfo(interest, percent)
}

private val GAIN = androidx.compose.ui.graphics.Color(0xFF4CAF50)
private val LOSS = androidx.compose.ui.graphics.Color(0xFFE53935)

// Renders the most recent full-year return (e.g. "Return 2024: +6.8% / yr"), or
// nothing when the backend hasn't provided one. Shared by the account header and
// the distribution rows.
@Composable
internal fun AnnualReturnLabel(
    latestAnnualReturn: String?,
    latestAnnualReturnYear: Int?,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelSmall,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    val pct = latestAnnualReturn?.toDoubleOrNull()?.times(100.0) ?: return
    val year = latestAnnualReturnYear ?: return
    val sign = if (pct > 0.0) "+" else ""
    Text(
        text = stringResource(R.string.annual_return_format, year, "$sign${"%.1f".format(pct)}%"),
        style = style,
        color = if (pct >= 0.0) GAIN else LOSS,
        textAlign = textAlign,
        modifier = modifier,
    )
}
