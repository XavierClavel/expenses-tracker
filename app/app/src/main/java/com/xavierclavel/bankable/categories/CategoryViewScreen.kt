package com.xavierclavel.bankable.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
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
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.ConfirmDeleteDialog
import com.xavierclavel.bankable.ui.SelectionActionBar
import com.xavierclavel.bankable.ui.SelectionHeaderRow
import com.xavierclavel.bankable.ui.TagPickerDialog
import com.xavierclavel.bankable.ui.rememberSelectionController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryViewScreen(
    viewModel: CategoriesViewModel,
    expensesViewModel: ExpensesViewModel,
    tagsViewModel: TagsViewModel,
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
                0 -> ExpensesTab(viewModel, expensesViewModel, tagsViewModel, category, navController)
                1 -> SubcategoriesTab(category, viewModel, navController)
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    viewModel: CategoriesViewModel,
    expensesViewModel: ExpensesViewModel,
    tagsViewModel: TagsViewModel,
    category: CategoryOut,
    navController: NavController,
) {
    val expenses by viewModel.categoryExpenses.collectAsState()
    val tags by tagsViewModel.tags.collectAsState()
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
        tags = tags,
        onBatchTag = { ids, tagId, add -> viewModel.batchTagCategoryExpenses(ids, tagId, add) },
        onBatchDelete = { ids -> viewModel.batchDeleteCategoryExpenses(ids) },
    )
}

/**
 * Date-grouped, paginated expense list shared by the category, subcategory and tag detail views.
 * When [onBatchDelete] is provided, long-pressing a row enters multi-select mode with batch
 * tag assign/remove (via [onBatchTag] + [tags]) and batch delete (with a confirm dialog).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpenseHistoryList(
    expenses: List<ExpenseOut>,
    isLoading: Boolean,
    emptyText: String,
    onLoadMore: () -> Unit,
    subcategoryFor: (ExpenseOut) -> SubcategoryOut?,
    onExpenseClick: (ExpenseOut) -> Unit,
    tags: List<TagOut> = emptyList(),
    onBatchTag: ((ids: List<Int>, tagId: Int, add: Boolean) -> Unit)? = null,
    onBatchDelete: ((ids: List<Int>) -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val grouped = remember(expenses) { expenses.groupBy { it.date } }
    val selectionEnabled = onBatchDelete != null
    val selection = rememberSelectionController<Int>()
    var tagPickerAdd by remember { mutableStateOf<Boolean?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selection.active) { selection.clear() }

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

    if (tagPickerAdd != null) {
        val add = tagPickerAdd == true
        TagPickerDialog(
            add = add,
            tags = tags,
            onDismiss = { tagPickerAdd = null },
            onPick = { tagId ->
                onBatchTag?.invoke(selection.selectedIds.toList(), tagId, add)
                selection.clear()
                tagPickerAdd = null
            },
        )
    }
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            count = selection.selectedIds.size,
            onConfirm = {
                onBatchDelete?.invoke(selection.selectedIds.toList())
                selection.clear()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    Column(Modifier.fillMaxSize()) {
        if (selection.active) {
            SelectionHeaderRow(selection.selectedIds.size) { selection.clear() }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
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
                                selectionMode = selection.active,
                                selected = selection.selectedIds.contains(expense.id),
                                onClick = {
                                    if (selection.active) selection.toggle(expense.id)
                                    else onExpenseClick(expense)
                                },
                                onLongClick = if (selectionEnabled) {
                                    { selection.enter(expense.id) }
                                } else null,
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

        if (selection.active) {
            SelectionActionBar {
                val hasSelection = selection.selectedIds.isNotEmpty()
                if (onBatchTag != null) {
                    Button(
                        onClick = { tagPickerAdd = true },
                        modifier = Modifier.weight(1f),
                        enabled = hasSelection,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.batch_assign_tag))
                    }
                    OutlinedButton(
                        onClick = { tagPickerAdd = false },
                        modifier = Modifier.weight(1f),
                        enabled = hasSelection,
                    ) {
                        Text(stringResource(R.string.batch_remove_tag))
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }, enabled = hasSelection) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
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
