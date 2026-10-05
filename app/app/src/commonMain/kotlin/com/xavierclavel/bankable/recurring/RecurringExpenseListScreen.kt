package com.xavierclavel.bankable.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.RecurringExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.cd_add_recurring_expense
import com.xavierclavel.bankable.resources.recurring_expenses_empty
import com.xavierclavel.bankable.resources.recurring_expenses_empty_hint
import com.xavierclavel.bankable.resources.recurring_schedule
import com.xavierclavel.bankable.resources.screen_recurring_expenses
import com.xavierclavel.bankable.util.formatIsoDateShort
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.screen_recurring_expenses)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.prepareNewRecurringExpense()
                navController.navigate("recurring/edit")
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_add_recurring_expense))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                viewModel.isLoading && recurringExpenses.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

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
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(Res.string.recurring_expenses_empty_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 80.dp),
                ) {
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

@Composable
private fun RecurringExpenseRow(
    recurringExpense: RecurringExpenseOut,
    subcategory: SubcategoryOut?,
    onClick: () -> Unit,
) {
    val locale = LocalAppLocale.current
    val amountColor = if (recurringExpense.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFE53935)
    val sign = if (recurringExpense.type == "INCOME") "+" else "-"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconByName(subcategory?.icon),
                contentDescription = null,
                tint = colorHexByName(subcategory?.color),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recurringExpense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        Res.string.recurring_schedule,
                        recurringExpense.dayOfMonth,
                        formatIsoDateShort(recurringExpense.nextDate, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subcategory != null) {
                    Text(
                        text = subcategory.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = "$sign${formatAmountDisplay(recurringExpense.amount, locale)} ${currencySymbol(recurringExpense.currency)}",
                color = amountColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}
