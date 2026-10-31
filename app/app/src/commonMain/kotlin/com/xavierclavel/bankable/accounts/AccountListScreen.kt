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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.AccountType
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.model.AccountOut
import com.xavierclavel.bankable.model.AccountTrendDto
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.accounts_interest_in_year
import com.xavierclavel.bankable.resources.accounts_interest_mean
import com.xavierclavel.bankable.resources.accounts_interest_total
import com.xavierclavel.bankable.resources.annual_return_format
import com.xavierclavel.bankable.resources.cd_add_account
import com.xavierclavel.bankable.resources.interest_view_label
import com.xavierclavel.bankable.resources.interest_view_mean
import com.xavierclavel.bankable.resources.interest_view_total
import com.xavierclavel.bankable.resources.interest_view_year
import com.xavierclavel.bankable.resources.label_charts
import com.xavierclavel.bankable.resources.label_distribution
import com.xavierclavel.bankable.resources.label_total_balance
import com.xavierclavel.bankable.resources.nav_accounts
import com.xavierclavel.bankable.resources.no_accounts_yet
import com.xavierclavel.bankable.resources.tab_balance
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.PillChip
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SectionLabel
import com.xavierclavel.bankable.ui.UnderlineTabs
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.util.formatFixed
import org.jetbrains.compose.resources.stringResource

// Which interest figure the account cards show — one at a time to keep them compact.
private const val INTEREST_VIEW_TOTAL = "total"
private const val INTEREST_VIEW_YEAR = "year"
private const val INTEREST_VIEW_MEAN = "mean"

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
        fun interestOf(t: AccountTrendDto) =
            (t.balance.toDoubleOrNull() ?: 0.0) - (t.contributions?.toDoubleOrNull() ?: 0.0)
        val gain = interestOf(last) - interestOf(prev)
        // Percentage is the backend's Modified-Dietz return (transfers weighted by
        // date), so mid-year deposits don't inflate the rate.
        val pct = last.returnRate?.toDoubleOrNull()?.times(100.0)
        CurrentYearInterest(last.year, gain, pct)
    }

    // Mean interest per year across all accounts: the average of the yearly gains and
    // of the yearly returns, over the years the all-accounts trends could measure.
    // Null when no year is measurable.
    val meanAnnualInterest: MeanAnnualInterest? = remember(userYearTrends) {
        fun accruedOf(t: AccountTrendDto) =
            (t.balance.toDoubleOrNull() ?: 0.0) - (t.contributions?.toDoubleOrNull() ?: 0.0)
        // A year counts only when the backend could measure its return; its gain is
        // the accrued interest minus the previous year's (zero before the series).
        val measured = userYearTrends.mapIndexedNotNull { i, trend ->
            val rate = trend.returnRate?.toDoubleOrNull() ?: return@mapIndexedNotNull null
            val previous = if (i > 0) accruedOf(userYearTrends[i - 1]) else 0.0
            MeasuredYear(trend.year, accruedOf(trend) - previous, rate)
        }
        if (measured.isEmpty()) return@remember null
        MeanAnnualInterest(
            firstYear = measured.first().year,
            lastYear = measured.last().year,
            gain = measured.sumOf { it.gain } / measured.size,
            percent = measured.sumOf { it.rate } / measured.size * 100.0,
        )
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var interestView by rememberSaveable { mutableStateOf(INTEREST_VIEW_TOTAL) }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.nav_accounts),
                actions = {
                    CircleIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.cd_add_account),
                        onClick = {
                            viewModel.prepareNewAccount()
                            navController.navigate("account/edit")
                        },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (selectedTab == 0) {
                // Same Total / This-year / Mean switch as the cards; value only, hidden when zero.
                val headerInterest: Pair<Double, Double?>? = when (interestView) {
                    INTEREST_VIEW_YEAR -> currentYearInterest?.let { it.gain to it.percent }
                    INTEREST_VIEW_MEAN -> meanAnnualInterest?.let { it.gain to it.percent }
                    else -> totalInterest?.let {
                        it to (if (totalContributions > 0.0) it / totalContributions * 100.0 else null)
                    }
                }?.takeIf { it.first != 0.0 }
                val caption = when (interestView) {
                    INTEREST_VIEW_YEAR -> currentYearInterest?.let {
                        stringResource(Res.string.accounts_interest_in_year, it.year)
                    }
                    // The mean is only readable if you know the span it covers.
                    INTEREST_VIEW_MEAN -> meanAnnualInterest?.let {
                        stringResource(Res.string.accounts_interest_mean, it.spanLabel())
                    }
                    else -> stringResource(Res.string.accounts_interest_total)
                }
                TotalBalanceCard(total, headerInterest, caption)
            } else {
                CompactTotalBalanceCard(total)
            }

            UnderlineTabs(
                tabs = listOf(
                    stringResource(Res.string.tab_balance),
                    stringResource(Res.string.label_distribution),
                    stringResource(Res.string.label_charts),
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                fillWidth = false,
                modifier = Modifier.padding(horizontal = ScreenGutter).padding(top = 12.dp),
            )

            when (selectedTab) {
                0 -> BalanceTab(accounts, isLoading, viewModel, navController, interestView) { interestView = it }
                1 -> AccountDistributionScreen(viewModel)
                2 -> AccountChartsScreen(viewModel, accountId = null)
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(total: Double, interest: Pair<Double, Double?>?, caption: String?) {
    NightCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(Res.string.label_total_balance).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NightTextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatAmount(total),
            style = MaterialTheme.typography.displayLarge,
            color = NightTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (interest != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = gainColor(interest.first), fontWeight = FontWeight.Bold)) {
                        append(signedAmountWithPercent(interest.first, interest.second))
                    }
                    if (caption != null) {
                        withStyle(SpanStyle(color = NightTextSecondary)) { append("  $caption") }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CompactTotalBalanceCard(total: Double) {
    NightCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.label_total_balance).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = NightTextSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatAmount(total),
                style = MaterialTheme.typography.headlineMedium,
                color = NightTextPrimary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BalanceTab(
    accounts: List<AccountOut>,
    isLoading: Boolean,
    viewModel: AccountsViewModel,
    navController: NavController,
    interestView: String,
    onInterestViewChange: (String) -> Unit,
) {
    if (isLoading && accounts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Mint)
        }
    } else if (accounts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_accounts_yet), color = NightTextSecondary)
        }
    } else {
        val grouped = AccountType.entries.mapNotNull { type ->
            val group = accounts.filter { AccountType.fromKey(it.type) == type }
            if (group.isEmpty()) null else type to group
        }
        // Show the interest switch only when some account actually has interest to show.
        val anyInterest = accounts.any {
            (it.contributions.toDoubleOrNull() ?: 0.0) != 0.0 ||
                it.latestYearInterest != null || it.meanAnnualInterest != null
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 14.dp, bottom = 24.dp),
        ) {
            if (anyInterest) {
                item(key = "interest_view") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.interest_view_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = NightTextSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        listOf(
                            INTEREST_VIEW_TOTAL to stringResource(Res.string.interest_view_total),
                            INTEREST_VIEW_YEAR to stringResource(Res.string.interest_view_year),
                            INTEREST_VIEW_MEAN to stringResource(Res.string.interest_view_mean),
                        ).forEach { (value, label) ->
                            PillChip(
                                text = label,
                                selected = interestView == value,
                                onClick = { onInterestViewChange(value) },
                            )
                        }
                    }
                }
            }
            grouped.forEach { (type, group) ->
                item(key = "header_${type.key}") {
                    SectionLabel(
                        text = stringResource(type.labelRes),
                        trailing = formatAmount(group.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }),
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                    )
                }
                items(group, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        interestView = interestView,
                        onClick = {
                            viewModel.selectAccount(account)
                            navController.navigate("account/view")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(account: AccountOut, interestView: String, onClick: () -> Unit) {
    // Only meaningful once transfers/interest have been declared for the account.
    val total = if ((account.contributions.toDoubleOrNull() ?: 0.0) != 0.0)
        accountInterest(account.amount, account.contributions) else null
    val yearInterest = account.latestYearInterest?.toDoubleOrNull()
    val yearPercent = account.latestAnnualReturn?.toDoubleOrNull()?.times(100.0)
    val meanInterest = account.meanAnnualInterest?.toDoubleOrNull()
    val meanPercent = account.meanAnnualReturn?.toDoubleOrNull()?.times(100.0)

    // The interest figure to show, per the selected view (value + optional percent);
    // hidden when it's exactly zero.
    val interest: Pair<Double, Double?>? = when (interestView) {
        INTEREST_VIEW_YEAR ->
            if (yearInterest != null && account.latestAnnualReturnYear != null) yearInterest to yearPercent else null
        INTEREST_VIEW_MEAN -> meanInterest?.let { it to meanPercent }
        else -> total?.let { it.value to it.percent }
    }?.takeIf { it.first != 0.0 }

    NightCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = NightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AnnualReturnLabel(
                    latestAnnualReturn = account.latestAnnualReturn,
                    latestAnnualReturnYear = account.latestAnnualReturnYear,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(account.amount.toDoubleOrNull() ?: 0.0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NightTextPrimary,
                )
                if (interest != null) {
                    // Colour-coded interest figure, no label: the Total/This-year/Mean
                    // switch says which figure it is.
                    Text(
                        text = signedAmountWithPercent(interest.first, interest.second),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = gainColor(interest.first),
                    )
                }
            }
        }
    }
}

/** Mint for a gain (or nothing), pink for a loss. */
internal fun gainColor(value: Double): Color = if (value >= 0.0) Mint else Danger

// "+1 940 € (+9.3%)": a signed amount with an optional signed percentage.
internal fun signedAmountWithPercent(value: Double, percent: Double?): String {
    val sign = if (value > 0.0) "+" else ""
    val percentText = percent?.let { " (${if (it > 0.0) "+" else ""}${formatFixed(it, 1)}%)" } ?: ""
    return "$sign${formatAmount(value)}$percentText"
}

internal fun formatAmount(value: Double): String = "${formatRoundedAmount(value)} €"

// Accrued interest for an account: the balance beyond what was put in via transfers.
// `percent` is the return relative to net contributions, or null when contributions
// aren't positive (so a percentage would be meaningless).
internal data class InterestInfo(val value: Double, val percent: Double?)

// Interest earned across all accounts during a single year (the latest year in the
// yearly trends): the € gained and the return relative to the prior year's balance.
internal data class CurrentYearInterest(val year: Int, val gain: Double, val percent: Double?)

// One year the all-accounts trends could measure: the interest earned during it and
// that year's return.
private data class MeasuredYear(val year: Int, val gain: Double, val rate: Double)

// Mean interest earned per year across all accounts, and the years it averages over.
internal data class MeanAnnualInterest(
    val firstYear: Int,
    val lastYear: Int,
    val gain: Double,
    val percent: Double?,
) {
    // Language-neutral span, e.g. "2021–2024" (or just "2024" for a single year).
    fun spanLabel(): String = if (firstYear == lastYear) "$firstYear" else "$firstYear–$lastYear"
}

internal fun accountInterest(amount: String, contributions: String): InterestInfo {
    val balance = amount.toDoubleOrNull() ?: 0.0
    val contrib = contributions.toDoubleOrNull() ?: 0.0
    val interest = balance - contrib
    val percent = if (contrib > 0.0) interest / contrib * 100.0 else null
    return InterestInfo(interest, percent)
}

// Renders the most recent full-year return (e.g. "Return 2024: +6.8% / yr"), or
// nothing when the backend hasn't provided one. Shared by the account header and
// the account cards.
@Composable
internal fun AnnualReturnLabel(
    latestAnnualReturn: String?,
    latestAnnualReturnYear: Int?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    textAlign: TextAlign? = null,
    color: Color = NightTextSecondary,
) {
    val pct = latestAnnualReturn?.toDoubleOrNull()?.times(100.0) ?: return
    val year = latestAnnualReturnYear ?: return
    val sign = if (pct > 0.0) "+" else ""
    Text(
        text = stringResource(Res.string.annual_return_format, year, "$sign${formatFixed(pct, 1)}%"),
        style = style,
        color = color,
        textAlign = textAlign,
        modifier = modifier,
    )
}
