package com.xavierclavel.bankable.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.expenses.ExpenseItem
import com.xavierclavel.bankable.expenses.ExpensesViewModel
import com.xavierclavel.bankable.expenses.formatDate
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.batch_assign_tag
import com.xavierclavel.bankable.resources.batch_remove_tag
import com.xavierclavel.bankable.resources.cat_default_badge
import com.xavierclavel.bankable.resources.cat_subcategory_count
import com.xavierclavel.bankable.resources.cd_add_subcategory
import com.xavierclavel.bankable.resources.cd_edit_category
import com.xavierclavel.bankable.resources.label_expenses
import com.xavierclavel.bankable.resources.label_subcategories
import com.xavierclavel.bankable.resources.no_expenses_in_category
import com.xavierclavel.bankable.resources.no_subcategories_yet
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.ConfirmDeleteDialog
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightFab
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SecondaryButton
import com.xavierclavel.bankable.ui.SectionLabel
import com.xavierclavel.bankable.ui.SelectionActionBar
import com.xavierclavel.bankable.ui.SelectionHeaderRow
import com.xavierclavel.bankable.ui.TagPickerDialog
import com.xavierclavel.bankable.ui.UnderlineTabs
import com.xavierclavel.bankable.ui.rememberSelectionController
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.DangerBorder
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

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
    val color = colorHexByName(category.color)

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = category.name,
                subtitle = stringResource(
                    Res.string.cat_subcategory_count,
                    category.subcategories.count { !it.isDefault },
                ),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
                leading = { IconTile(icon = iconByName(category.icon), color = color, size = 52.dp, iconSize = 26.dp) },
                actions = {
                    CircleIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(Res.string.cd_edit_category),
                        onClick = {
                            viewModel.prepareEditCategory(category)
                            navController.navigate("category/edit")
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                NightFab(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.cd_add_subcategory),
                    onClick = {
                        viewModel.prepareNewSubcategory(category)
                        navController.navigate("subcategory/edit")
                    },
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            UnderlineTabs(
                tabs = listOf(
                    stringResource(Res.string.label_expenses),
                    stringResource(Res.string.label_subcategories),
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = ScreenGutter),
            )

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
        emptyText = stringResource(Res.string.no_expenses_in_category),
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
    val locale = LocalAppLocale.current
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
                    CircularProgressIndicator(color = Mint)
                }
            } else if (expenses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(emptyText, style = MaterialTheme.typography.bodyLarge, color = NightTextSecondary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = ScreenGutter, top = 8.dp, end = ScreenGutter, bottom = 96.dp),
                ) {
                    grouped.forEach { (dateStr, dayExpenses) ->
                        stickyHeader(key = "header_$dateStr") {
                            SectionLabel(
                                text = formatDate(dateStr, locale),
                                modifier = Modifier
                                    .background(NightBackground)
                                    .padding(top = 10.dp, bottom = 8.dp, start = 4.dp),
                            )
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
                                CircularProgressIndicator(color = Mint)
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
                    PrimaryButton(
                        text = stringResource(Res.string.batch_assign_tag),
                        onClick = { tagPickerAdd = true },
                        modifier = Modifier.weight(1f),
                        enabled = hasSelection,
                        icon = Icons.Outlined.Sell,
                    )
                    SecondaryButton(
                        text = stringResource(Res.string.batch_remove_tag),
                        onClick = { tagPickerAdd = false },
                        modifier = Modifier.weight(1f),
                        enabled = hasSelection,
                    )
                }
                CircleIconButton(
                    icon = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.action_delete),
                    onClick = { showDeleteConfirm = true },
                    enabled = hasSelection,
                    tint = Danger,
                    borderColor = DangerBorder,
                )
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
    // The default subcategory leads the list, flagged with a badge.
    val subcategories = remember(category) {
        category.subcategories.sortedByDescending { it.isDefault }
    }

    if (subcategories.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.no_subcategories_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = NightTextSecondary,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(subcategories, key = { it.id }) { subcategory ->
                SubcategoryCard(
                    name = if (subcategory.isDefault) category.name else subcategory.name,
                    subcategory = subcategory,
                    onClick = {
                        viewModel.prepareViewSubcategory(subcategory)
                        navController.navigate("subcategory/view")
                    },
                )
            }
        }
    }
}

@Composable
private fun SubcategoryCard(
    name: String,
    subcategory: SubcategoryOut,
    onClick: () -> Unit,
) {
    NightCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = iconByName(subcategory.icon), color = colorHexByName(subcategory.color))
            Spacer(Modifier.width(14.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NightTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (subcategory.isDefault) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.cat_default_badge).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NightTextSecondary,
                )
            }
        }
    }
}
