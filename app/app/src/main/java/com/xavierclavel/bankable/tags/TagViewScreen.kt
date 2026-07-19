package com.xavierclavel.bankable.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.categories.ExpenseHistoryList
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.expenses.ExpensesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagViewScreen(
    tagsViewModel: TagsViewModel,
    expensesViewModel: ExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    navController: NavController,
) {
    val selected = tagsViewModel.selectedTag ?: return
    // Track the up-to-date copy from the list so total/label edits reflect on return.
    val tags by tagsViewModel.tags.collectAsState()
    val tag = remember(tags, selected.id) { tags.find { it.id == selected.id } ?: selected }

    val expenses by tagsViewModel.tagExpenses.collectAsState()
    val categories by categoriesViewModel.categories.collectAsState()
    val subcategoryMap = remember(categories) {
        categories.flatMap { it.subcategories }.associateBy { it.id }
    }
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tag.label) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        tagsViewModel.prepareEditTag(tag)
                        navController.navigate("tag/edit")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_tag))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.tag_total),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.tag_expense_count, tag.expenseCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "${formatAmountDisplay(tag.total, locale)} ${currencySymbol("EUR")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            ExpenseHistoryList(
                expenses = expenses,
                isLoading = tagsViewModel.isLoadingExpenses,
                emptyText = stringResource(R.string.no_expenses_with_tag),
                onLoadMore = { tagsViewModel.loadMoreTagExpenses() },
                subcategoryFor = { subcategoryMap[it.categoryId] },
                onExpenseClick = { expense ->
                    expensesViewModel.prepareEditExpense(expense, subcategoryMap[expense.categoryId])
                    navController.navigate("expense/edit")
                },
                tags = tags,
                onBatchTag = { ids, tagId, add -> tagsViewModel.batchTagTagExpenses(ids, tagId, add) {} },
                onBatchDelete = { ids -> tagsViewModel.batchDeleteTagExpenses(ids) {} },
            )
        }
    }
}
