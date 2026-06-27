package com.xavierclavel.bankable.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.expenses.ExpenseItem
import com.xavierclavel.bankable.expenses.ExpensesViewModel
import com.xavierclavel.bankable.expenses.formatDate
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryViewScreen(
    viewModel: CategoriesViewModel,
    expensesViewModel: ExpensesViewModel,
    navController: NavController,
) {
    val selected = viewModel.selectedCategory ?: return
    // Track the up-to-date copy from the list so subcategory edits/additions reflect on return.
    val categories by viewModel.categories.collectAsState()
    val category = remember(categories, selected.id) {
        categories.find { it.id == selected.id } ?: selected
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.prepareEditCategory(category)
                        navController.navigate("category/edit")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_category))
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = {
                    viewModel.prepareNewSubcategory(category)
                    navController.navigate("subcategory/edit")
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_subcategory))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.label_expenses)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.label_subcategories)) },
                )
            }

            when (selectedTab) {
                0 -> ExpensesTab(viewModel, expensesViewModel, category, navController)
                1 -> SubcategoriesTab(category, viewModel, navController)
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    viewModel: CategoriesViewModel,
    expensesViewModel: ExpensesViewModel,
    category: CategoryOut,
    navController: NavController,
) {
    val expenses by viewModel.categoryExpenses.collectAsState()
    val subcategoryMap = remember(category) {
        category.subcategories.associateBy { it.id }
    }
    ExpenseHistoryList(
        expenses = expenses,
        isLoading = viewModel.isLoadingExpenses,
        emptyText = stringResource(R.string.no_expenses_in_category),
        onLoadMore = { viewModel.loadMoreCategoryExpenses() },
        subcategoryFor = { subcategoryMap[it.categoryId] },
        onExpenseClick = { expense ->
            expensesViewModel.prepareEditExpense(expense, subcategoryMap[expense.categoryId])
            navController.navigate("expense/edit")
        },
    )
}

/** Date-grouped, paginated expense list shared by the category and subcategory detail views. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpenseHistoryList(
    expenses: List<ExpenseOut>,
    isLoading: Boolean,
    emptyText: String,
    onLoadMore: () -> Unit,
    subcategoryFor: (ExpenseOut) -> SubcategoryOut?,
    onExpenseClick: (ExpenseOut) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val grouped = remember(expenses) { expenses.groupBy { it.date } }

    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 8
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) onLoadMore()
    }

    if (expenses.isEmpty() && isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (expenses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 80.dp),
        ) {
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
                        subcategory = subcategoryFor(expense),
                        onClick = { onExpenseClick(expense) },
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

@Composable
private fun SubcategoriesTab(
    category: CategoryOut,
    viewModel: CategoriesViewModel,
    navController: NavController,
) {
    val subcategories = remember(category) {
        category.subcategories.filter { !it.isDefault }
    }

    if (subcategories.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_subcategories_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
        ) {
            items(subcategories, key = { it.id }) { subcategory ->
                SubcategoryRow(
                    subcategory = subcategory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    onClick = {
                        viewModel.prepareViewSubcategory(subcategory)
                        navController.navigate("subcategory/view")
                    },
                )
            }
        }
    }
}
