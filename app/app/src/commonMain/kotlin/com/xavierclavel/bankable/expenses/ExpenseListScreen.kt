package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.platformFormatDecimal
import com.xavierclavel.bankable.platform.platformFormatMonthYear
import com.xavierclavel.bankable.platform.showToast
import com.xavierclavel.bankable.resources.batch_make_recurring
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.app_name
import com.xavierclavel.bankable.resources.batch_assign_tag
import com.xavierclavel.bankable.resources.batch_duplicate
import com.xavierclavel.bankable.resources.batch_duplicate_done
import com.xavierclavel.bankable.resources.batch_make_recurring_done
import com.xavierclavel.bankable.resources.batch_remove_tag
import com.xavierclavel.bankable.resources.batch_selected_count
import com.xavierclavel.bankable.resources.cd_add_expense
import com.xavierclavel.bankable.resources.cd_clear_search
import com.xavierclavel.bankable.resources.cd_exit_selection
import com.xavierclavel.bankable.resources.cd_filter
import com.xavierclavel.bankable.resources.cd_has_tags
import com.xavierclavel.bankable.resources.cd_settings
import com.xavierclavel.bankable.resources.expenses_empty_cta
import com.xavierclavel.bankable.resources.expenses_empty_description
import com.xavierclavel.bankable.resources.expenses_empty_title
import com.xavierclavel.bankable.resources.expenses_balance_so_far
import com.xavierclavel.bankable.resources.expenses_today_prefix
import com.xavierclavel.bankable.resources.no_matching_expenses
import com.xavierclavel.bankable.resources.screen_recurring_expenses
import com.xavierclavel.bankable.resources.search_expenses_hint
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.ConfirmDeleteDialog
import com.xavierclavel.bankable.ui.EmptyState
import com.xavierclavel.bankable.ui.CheckDot
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightFab
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SectionLabel
import com.xavierclavel.bankable.ui.TagPickerDialog
import com.xavierclavel.bankable.ui.amountColor
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.DangerBorder
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.MintBorder
import com.xavierclavel.bankable.ui.theme.MintContainer
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.util.currentMonth
import com.xavierclavel.bankable.util.currentYear
import com.xavierclavel.bankable.util.formatIsoDateLong
import com.xavierclavel.bankable.util.plusOneMonthIsoDate
import com.xavierclavel.bankable.util.formatIsoDateShort
import com.xavierclavel.bankable.util.todayIsoDate
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseListScreen(
    viewModel: ExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    tagsViewModel: TagsViewModel,
    navController: NavController,
) {
    val expenses by viewModel.expenses.collectAsState()
    val isLoading = viewModel.isLoading
    val categories by categoriesViewModel.categories.collectAsState()
    val tags by tagsViewModel.tags.collectAsState()

    val subcategoryMap = remember(categories) {
        categories.flatMap { it.subcategories }.associateBy { it.id }
    }
    val locale = LocalAppLocale.current
    val currency = currencySymbol(expenses.firstOrNull()?.currency ?: "EUR")

    val selectionMode = viewModel.selectionMode
    val selectedIds = viewModel.selectedExpenseIds
    // null = closed, true = pick a tag to add, false = pick a tag to remove
    var tagPickerAdd by remember { mutableStateOf<Boolean?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDuplicatePicker by remember { mutableStateOf(false) }
    var showMakeRecurringConfirm by remember { mutableStateOf(false) }

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
                    onError = { msg -> showToast(msg) },
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
                    onError = { msg -> showToast(msg) },
                )
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showDuplicatePicker) {
        // Default to one month after the most recent selected expense: the usual case is
        // carrying recurring expenses over to the next month.
        val latestDate = expenses.filter { selectedIds.contains(it.id) }.maxOfOrNull { it.date }
        val count = selectedIds.size
        val doneMessage = stringResource(Res.string.batch_duplicate_done, count)
        DuplicateExpensesDialog(
            count = count,
            initialDate = latestDate?.let { plusOneMonthIsoDate(it) } ?: todayIsoDate(),
            onConfirm = { date ->
                showDuplicatePicker = false
                viewModel.batchDuplicateSelection(
                    date = date,
                    onSuccess = { showToast(doneMessage) },
                    onError = { msg -> showToast(msg) },
                )
            },
            onDismiss = { showDuplicatePicker = false },
        )
    }

    if (showMakeRecurringConfirm) {
        val count = selectedIds.size
        val doneMessage = stringResource(Res.string.batch_make_recurring_done, count)
        MakeRecurringDialog(
            count = count,
            onConfirm = {
                showMakeRecurringConfirm = false
                viewModel.batchMakeRecurringSelection(
                    onSuccess = { showToast(doneMessage) },
                    onError = { msg -> showToast(msg) },
                )
            },
            onDismiss = { showMakeRecurringConfirm = false },
        )
    }

    val grouped = remember(expenses) {
        expenses.groupBy { it.date }
    }

    // "Balance so far" (income minus expenses) for the current month, shown only when it is exact: no search or
    // filter narrows the list, and every expense of the month has been loaded (the list
    // is sorted newest first, so an older expense proves the month is complete).
    val year = currentYear()
    val month = currentMonth()
    val monthPrefix = "$year-${month.toString().padStart(2, '0')}"
    val monthBalance: Double? = remember(expenses, viewModel.hasActiveFilters, viewModel.allLoaded, monthPrefix) {
        val complete = viewModel.allLoaded || expenses.any { it.date < "$monthPrefix-01" }
        if (viewModel.hasActiveFilters || !complete) null
        else expenses
            .filter { it.date.startsWith(monthPrefix) }
            .sumOf { signedValue(it) }
    }

    val listState = rememberLazyListState()
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
        containerColor = NightBackground,
        topBar = {
            if (selectionMode) {
                val selectedTotal = expenses
                    .filter { selectedIds.contains(it.id) }
                    .sumOf { signedValue(it) }
                Column(Modifier.background(NightBackground)) {
                    ScreenHeader(
                        title = stringResource(Res.string.batch_selected_count, selectedIds.size),
                        subtitle = formatSignedTotal(selectedTotal, locale, currency),
                        navigationIcon = Icons.Default.Close,
                        navigationContentDescription = stringResource(Res.string.cd_exit_selection),
                        onNavigate = { viewModel.clearSelection() },
                        large = false,
                    ) {
                        CircleIconButton(
                            icon = Icons.Outlined.Repeat,
                            contentDescription = stringResource(Res.string.batch_make_recurring),
                            onClick = { showMakeRecurringConfirm = true },
                            enabled = selectedIds.isNotEmpty(),
                        )
                        CircleIconButton(
                            icon = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(Res.string.batch_duplicate),
                            onClick = { showDuplicatePicker = true },
                            enabled = selectedIds.isNotEmpty(),
                        )
                        CircleIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = stringResource(Res.string.action_delete),
                            onClick = { showDeleteConfirm = true },
                            enabled = selectedIds.isNotEmpty(),
                            tint = Danger,
                            borderColor = DangerBorder,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ScreenGutter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SelectionPillButton(
                            text = stringResource(Res.string.batch_assign_tag),
                            onClick = { tagPickerAdd = true },
                            enabled = selectedIds.isNotEmpty(),
                            primary = true,
                            modifier = Modifier.weight(1f),
                        )
                        SelectionPillButton(
                            text = stringResource(Res.string.batch_remove_tag),
                            onClick = { tagPickerAdd = false },
                            enabled = selectedIds.isNotEmpty(),
                            primary = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                ScreenHeader(
                    title = stringResource(Res.string.app_name),
                    modifier = Modifier.background(NightBackground),
                ) {
                    CircleIconButton(
                        icon = Icons.Outlined.Repeat,
                        contentDescription = stringResource(Res.string.screen_recurring_expenses),
                        onClick = { navController.navigate("recurring") },
                    )
                    CircleIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = stringResource(Res.string.cd_settings),
                        onClick = { navController.navigate("settings") },
                    )
                }
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                NightFab(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.cd_add_expense),
                    onClick = {
                        viewModel.prepareNewExpense()
                        navController.navigate("expense/edit")
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (expenses.isEmpty() && isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = Mint)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = ScreenGutter, top = 4.dp, end = ScreenGutter, bottom = 96.dp),
                ) {
                    item(key = "search_header") {
                        SearchBar(
                            query = viewModel.searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            filtersActive = viewModel.filter.isActive,
                            onFilterClick = { navController.navigate("expense/filter") },
                        )
                    }
                    item(key = "month_summary") {
                        if (monthBalance != null && !selectionMode) {
                            MonthSummary(
                                label = stringResource(
                                    Res.string.expenses_balance_so_far,
                                    platformFormatMonthYear(year, month, locale).replaceFirstChar { it.titlecase() },
                                ),
                                amount = formatSignedTotal(monthBalance, locale, currency),
                                amountColor = if (monthBalance > 0) Mint else NightTextPrimary,
                            )
                        } else {
                            Spacer(Modifier.height(1.dp))
                        }
                    }
                    if (expenses.isEmpty() && viewModel.hasActiveFilters) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.no_matching_expenses),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NightTextSecondary,
                                )
                            }
                        }
                    } else if (expenses.isEmpty()) {
                        item(key = "empty") {
                            EmptyState(
                                animationPath = "files/empty_expenses.json",
                                title = stringResource(Res.string.expenses_empty_title),
                                description = stringResource(Res.string.expenses_empty_description),
                                actionLabel = stringResource(Res.string.expenses_empty_cta),
                                actionIcon = Icons.Default.Add,
                                onAction = {
                                    viewModel.prepareNewExpense()
                                    navController.navigate("expense/edit")
                                },
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }
                    } else {
                        val today = todayIsoDate()
                        grouped.forEach { (dateStr, dayExpenses) ->
                            stickyHeader(key = "header_$dateStr") {
                                val dayTotal = dayExpenses.sumOf { signedValue(it) }
                                val dateLabel = if (dateStr.startsWith("$year-")) formatIsoDateShort(dateStr, locale)
                                else formatDate(dateStr, locale)
                                SectionLabel(
                                    text = if (dateStr == today) stringResource(Res.string.expenses_today_prefix, dateLabel)
                                    else dateLabel,
                                    trailing = formatSignedTotal(dayTotal, locale, currency),
                                    trailingColor = if (dayTotal > 0) Mint else NightTextSecondary,
                                    modifier = Modifier
                                        .background(NightBackground)
                                        .padding(top = 14.dp, bottom = 8.dp, start = 2.dp, end = 2.dp),
                                )
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
                                    CircularProgressIndicator(color = Mint)
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filtersActive: Boolean,
    onFilterClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 54.dp)
                .clip(shape)
                .background(NightCard)
                .border(1.dp, NightBorder, shape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = NightTextSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            val textStyle = MaterialTheme.typography.bodyLarge
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle.merge(TextStyle(color = NightTextPrimary)),
                cursorBrush = SolidColor(Mint),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(stringResource(Res.string.search_expenses_hint), style = textStyle, color = NightTextTertiary)
                        }
                        inner()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.cd_clear_search),
                    tint = NightTextSecondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                        .padding(4.dp)
                        .size(18.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(shape)
                .background(if (filtersActive) MintContainer else NightCard)
                .border(1.dp, if (filtersActive) MintBorder else NightBorder, shape)
                .clickable(onClick = onFilterClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = stringResource(Res.string.cd_filter),
                tint = if (filtersActive) Mint else NightTextPrimary,
                modifier = Modifier.size(22.dp),
            )
            if (filtersActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-10).dp, y = 10.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Mint),
                )
            }
        }
    }
}

@Composable
private fun MonthSummary(label: String, amount: String, amountColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = NightTextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.headlineMedium,
            color = amountColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun SelectionPillButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (primary) MintContainer else NightCard)
            .border(1.dp, if (primary) MintBorder else NightBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (primary) {
            Icon(Icons.Outlined.Sell, contentDescription = null, tint = Mint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
            color = if (primary) Mint else NightTextSecondary,
        )
    }
}

@Composable
internal fun ExpenseItem(
    expense: ExpenseOut,
    subcategory: SubcategoryOut?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    val locale = LocalAppLocale.current
    val isIncome = expense.type == "INCOME"
    val sign = if (isIncome) "+" else "−"

    NightCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CheckDot(checked = selected, size = 28.dp)
                }
            } else {
                IconTile(
                    icon = iconByName(subcategory?.icon),
                    color = colorHexByName(subcategory?.color),
                    size = 40.dp,
                    iconSize = 20.dp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subcategory != null) {
                        Text(
                            text = subcategory.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = NightTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (expense.tagIds.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Outlined.Sell,
                            contentDescription = stringResource(Res.string.cd_has_tags),
                            tint = Mint,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$sign${formatAmountDisplay(expense.amount, locale)} ${currencySymbol(expense.currency)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor(isIncome),
                maxLines = 1,
            )
        }
    }
}

/** Income counts positive, expenses negative. */
private fun signedValue(expense: ExpenseOut): Double {
    val value = expense.amount.toDoubleOrNull() ?: 0.0
    return if (expense.type == "INCOME") value else -value
}

/** "+2 850,00 €" / "−153,00 €" with the locale's separators. */
private fun formatSignedTotal(value: Double, locale: String, currency: String): String {
    val sign = if (value > 0) "+" else if (value < 0) "−" else ""
    return "$sign${platformFormatDecimal(kotlin.math.abs(value), locale, 2, 2)} $currency"
}

internal fun formatDate(dateStr: String, locale: String): String =
    formatIsoDateLong(dateStr, locale)
