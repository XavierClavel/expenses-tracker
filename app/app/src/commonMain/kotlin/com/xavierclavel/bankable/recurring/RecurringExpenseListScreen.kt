package com.xavierclavel.bankable.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.RecurringExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.platformFormatDecimal
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.cd_add_recurring_expense
import com.xavierclavel.bankable.resources.recurring_every_month
import com.xavierclavel.bankable.resources.recurring_expenses_empty
import com.xavierclavel.bankable.resources.recurring_expenses_empty_hint
import com.xavierclavel.bankable.resources.recurring_schedule
import com.xavierclavel.bankable.resources.recurring_subtitle
import com.xavierclavel.bankable.resources.screen_recurring_expenses
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightFab
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.amountColor
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.util.formatIsoDateShort
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecurringExpenseListScreen(
    viewModel: RecurringExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    navController: NavController,
) {
    val recurringExpenses by viewModel.recurringExpenses.collectAsState()
    val categories by categoriesViewModel.categories.collectAsState()
    val subcategoryMap = remember(categories) {
        categories.flatMap { it.subcategories }.associateBy { it.id }
    }

    // Next dates move forward as the backend creates expenses; refresh on each visit.
    LaunchedEffect(Unit) { viewModel.loadRecurringExpenses() }

    Scaffold(
        containerColor = NightBackground,
        floatingActionButton = {
            NightFab(
                icon = Icons.Default.Add,
                contentDescription = stringResource(Res.string.cd_add_recurring_expense),
                onClick = {
                    viewModel.prepareNewRecurringExpense()
                    navController.navigate("recurring/edit")
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScreenHeader(
                title = stringResource(Res.string.screen_recurring_expenses),
                subtitle = stringResource(Res.string.recurring_subtitle),
                navigationIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    viewModel.isLoading && recurringExpenses.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = Mint)

                    recurringExpenses.isEmpty() ->
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.recurring_expenses_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = NightTextPrimary,
                            )
                            Text(
                                text = stringResource(Res.string.recurring_expenses_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = NightTextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = ScreenGutter, top = 4.dp, end = ScreenGutter, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item(key = "summary") { MonthlySummaryCard(recurringExpenses) }
                        items(recurringExpenses, key = { it.id }) { recurringExpense ->
                            val subcategory = subcategoryMap[recurringExpense.categoryId]
                            RecurringExpenseRow(
                                recurringExpense = recurringExpense,
                                subcategory = subcategory,
                                onClick = {
                                    viewModel.prepareEditRecurringExpense(recurringExpense, subcategory)
                                    navController.navigate("recurring/edit")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Every month" card with the monthly totals of the recurring expenses and incomes. */
@Composable
private fun MonthlySummaryCard(recurringExpenses: List<RecurringExpenseOut>) {
    val locale = LocalAppLocale.current
    val (incomes, expenses) = recurringExpenses.partition { it.type == "INCOME" }
    val symbol = currencySymbol(recurringExpenses.firstOrNull()?.currency ?: "EUR")
    fun total(items: List<RecurringExpenseOut>): String =
        platformFormatDecimal(
            items.sumOf { it.amount.toDoubleOrNull() ?: 0.0 },
            locale,
            minFractionDigits = 2,
            maxFractionDigits = 2,
        )

    NightCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.recurring_every_month),
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextSecondary,
                modifier = Modifier.weight(1f),
            )
            if (expenses.isNotEmpty()) {
                Text(
                    text = "-${total(expenses)} $symbol",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NightTextPrimary,
                )
            }
            if (expenses.isNotEmpty() && incomes.isNotEmpty()) Spacer(Modifier.width(14.dp))
            if (incomes.isNotEmpty()) {
                Text(
                    text = "+${total(incomes)} $symbol",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Mint,
                )
            }
        }
    }
}

@Composable
private fun RecurringExpenseRow(
    recurringExpense: RecurringExpenseOut,
    subcategory: SubcategoryOut?,
    onClick: () -> Unit,
) {
    val locale = LocalAppLocale.current
    val isIncome = recurringExpense.type == "INCOME"
    val sign = if (isIncome) "+" else "-"
    val schedule = stringResource(
        Res.string.recurring_schedule,
        recurringExpense.dayOfMonth,
        formatIsoDateShort(recurringExpense.nextDate, locale),
    )

    NightCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = iconByName(subcategory?.icon),
                color = colorHexByName(subcategory?.color),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recurringExpense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(subcategory?.name, schedule).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = NightTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$sign${formatAmountDisplay(recurringExpense.amount, locale)} ${currencySymbol(recurringExpense.currency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor(isIncome),
            )
        }
    }
}
