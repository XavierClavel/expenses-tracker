package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import android.widget.Toast
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.ConfirmDeleteDialog
import com.xavierclavel.bankable.ui.TagPickerDialog
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import androidx.compose.ui.platform.LocalConfiguration
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    tagsViewModel: TagsViewModel,
    navController: NavController,
    onLogout: () -> Unit,
) {
    val expenses by viewModel.expenses.collectAsState()
    val isLoading = viewModel.isLoading
    val categories by categoriesViewModel.categories.collectAsState()
    val tags by tagsViewModel.tags.collectAsState()
    val context = LocalContext.current

    val subcategoryMap = remember(categories) {
        categories.flatMap { it.subcategories }.associateBy { it.id }
    }
    val locale = LocalConfiguration.current.locales[0]

    val selectionMode = viewModel.selectionMode
    val selectedIds = viewModel.selectedExpenseIds
    // null = closed, true = pick a tag to add, false = pick a tag to remove
    var tagPickerAdd by remember { mutableStateOf<Boolean?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Hardware back exits selection mode instead of leaving the screen.
    BackHandler(enabled = selectionMode) { viewModel.clearSelection() }

    if (tagPickerAdd != null) {
        val add = tagPickerAdd == true
        TagPickerDialog(
            add = add,
            tags = tags,
            onDismiss = { tagPickerAdd = null },
            onPick = { tagId ->
                viewModel.batchTagSelection(
                    tagId = tagId,
                    add = add,
                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                )
                tagPickerAdd = null
            },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            count = selectedIds.size,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.batchDeleteSelection(
                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                )
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    val grouped = remember(expenses) {
        expenses.groupBy { it.date }
    }

    // Start scrolled just past the search header (item 0) so expenses fill from the
    // top on open; the search bar sits above and is revealed by scrolling up.
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
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.batch_selected_count, selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_exit_selection))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (selectionMode) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { tagPickerAdd = true },
                            modifier = Modifier.weight(1f),
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.batch_assign_tag))
                        }
                        OutlinedButton(
                            onClick = { tagPickerAdd = false },
                            modifier = Modifier.weight(1f),
                            enabled = selectedIds.isNotEmpty(),
                        ) {
                            Text(stringResource(R.string.batch_remove_tag))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = {
                    viewModel.prepareNewExpense()
                    navController.navigate("expense/edit")
                }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_expense))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (expenses.isEmpty() && isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 80.dp),
                ) {
                    item(key = "search_header") {
                        SearchHeader(
                            query = viewModel.searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            filtersActive = viewModel.filter.isActive,
                            onFilterClick = { navController.navigate("expense/filter") },
                            onSettings = { navController.navigate("settings") },
                            onLogout = onLogout,
                        )
                    }
                    if (expenses.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (viewModel.hasActiveFilters)
                                        stringResource(R.string.no_matching_expenses)
                                    else
                                        stringResource(R.string.no_expenses_yet),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
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
                                    selectionMode = selectionMode,
                                    selected = selectedIds.contains(expense.id),
                                    onClick = {
                                        if (selectionMode) {
                                            viewModel.toggleSelection(expense.id)
                                        } else {
                                            viewModel.prepareEditExpense(expense, subcategoryMap[expense.categoryId])
                                            navController.navigate("expense/edit")
                                        }
                                    },
                                    onLongClick = { viewModel.enterSelectionMode(expense.id) },
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
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    filtersActive: Boolean,
    onFilterClick: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.cd_logout))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.search_expenses_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear_search))
                        }
                    }
                } else null,
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
            IconButton(onClick = onFilterClick) {
                BadgedBox(
                    badge = { if (filtersActive) Badge() },
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.cd_filter),
                        tint = if (filtersActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpenseItem(
    expense: ExpenseOut,
    subcategory: SubcategoryOut?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    val iconColor = colorHexByName(subcategory?.color)
    val locale = LocalConfiguration.current.locales[0]

    val amountColor = if (expense.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFE53935)
    val sign = if (expense.type == "INCOME") "+" else "-"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Icon(
                        imageVector = if (selected) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        imageVector = iconByName(subcategory?.icon),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
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
                    text = "$sign${formatAmountDisplay(expense.amount, locale)} ${currencySymbol(expense.currency)}",
                    color = amountColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }

        // Bookmark ribbon pinned to the top edge, at a fixed x so it lines up across cards.
        if (expense.tagIds.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = stringResource(R.string.cd_has_tags),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp)
                    .offset(y = (-6).dp)
                    .size(width = 15.dp, height = 22.dp),
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
