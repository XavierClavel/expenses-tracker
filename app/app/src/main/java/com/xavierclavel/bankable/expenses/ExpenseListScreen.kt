package com.xavierclavel.bankable.expenses

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import androidx.compose.ui.platform.LocalConfiguration
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ExpenseListScreen(
    viewModel: ExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    navController: NavController,
    onLogout: () -> Unit,
) {
    val expenses by viewModel.expenses.collectAsState()
    val isLoading = viewModel.isLoading
    val categories by categoriesViewModel.categories.collectAsState()

    val subcategoryMap = remember(categories) {
        categories.flatMap { it.subcategories }.associateBy { it.id }
    }
    val locale = LocalConfiguration.current.locales[0]

    val grouped = remember(expenses) {
        expenses.groupBy { it.date }
    }

    // Start scrolled just past the logout button (item 0) so it's hidden on open.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 8
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) viewModel.loadMore()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.prepareNewExpense()
                navController.navigate("expense/edit")
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_expense))
            }
        }
    ) { padding ->
        if (expenses.isEmpty() && isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (expenses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                AccountActionsRow(
                    onLogout = onLogout,
                    onSettings = { navController.navigate("settings") },
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_expenses_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 80.dp),
            ) {
                item(key = "logout") {
                    AccountActionsRow(
                        onLogout = onLogout,
                        onSettings = { navController.navigate("settings") },
                    )
                }
                grouped.forEach { (dateStr, dayExpenses) ->
                    stickyHeader(key = "header_$dateStr") {
                        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
                            Text(
                                text = formatDate(dateStr, locale),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            )
                        }
                    }
                    items(dayExpenses, key = { it.id }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            subcategory = subcategoryMap[expense.categoryId],
                            onClick = {
                                viewModel.prepareEditExpense(expense, subcategoryMap[expense.categoryId])
                                navController.navigate("expense/edit")
                            },
                        )
                    }
                }
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountActionsRow(
    onLogout: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.cd_settings),
            )
        }
        IconButton(onClick = onLogout) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = stringResource(R.string.cd_logout),
            )
        }
    }
}

@Composable
internal fun ExpenseItem(
    expense: ExpenseOut,
    subcategory: SubcategoryOut?,
    onClick: () -> Unit,
) {
    val iconColor = colorHexByName(subcategory?.color)
    val locale = LocalConfiguration.current.locales[0]

    val amountColor = if (expense.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFE53935)
    val sign = if (expense.type == "INCOME") "+" else "-"

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
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconByName(subcategory?.icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (subcategory != null) {
                    Text(
                        text = subcategory.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "$sign${formatAmountDisplay(expense.amount, locale)} ${currencySymbol(expense.currency)}",
                color = amountColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
    }
}

internal fun formatDate(dateStr: String, locale: Locale): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: return dateStr
        SimpleDateFormat("d MMMM yyyy", locale).format(date)
    } catch (_: Exception) {
        dateStr
    }
}
