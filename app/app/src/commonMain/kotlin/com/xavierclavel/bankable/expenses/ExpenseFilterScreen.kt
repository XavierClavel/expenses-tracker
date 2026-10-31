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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_ok
import com.xavierclavel.bankable.resources.cd_clear_date
import com.xavierclavel.bankable.resources.expenses_apply_many
import com.xavierclavel.bankable.resources.expenses_apply_one
import com.xavierclavel.bankable.resources.expenses_category_and_tag
import com.xavierclavel.bankable.resources.expenses_date_any
import com.xavierclavel.bankable.resources.filter_all_categories
import com.xavierclavel.bankable.resources.filter_all_tags
import com.xavierclavel.bankable.resources.filter_amount
import com.xavierclavel.bankable.resources.filter_amount_invalid
import com.xavierclavel.bankable.resources.filter_amount_max
import com.xavierclavel.bankable.resources.filter_amount_min
import com.xavierclavel.bankable.resources.filter_apply
import com.xavierclavel.bankable.resources.filter_category
import com.xavierclavel.bankable.resources.filter_date_from
import com.xavierclavel.bankable.resources.filter_date_range
import com.xavierclavel.bankable.resources.filter_date_to
import com.xavierclavel.bankable.resources.filter_reset
import com.xavierclavel.bankable.resources.filter_tag
import com.xavierclavel.bankable.resources.filter_type
import com.xavierclavel.bankable.resources.filter_type_all
import com.xavierclavel.bankable.resources.label_expense
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.screen_filter_expenses
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.CardShape
import com.xavierclavel.bankable.ui.FormRow
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.PillChip
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SectionLabel
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.DangerBorder
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.util.isoDateToUtcMillis
import com.xavierclavel.bankable.util.utcMillisToIsoDate
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpenseFilterScreen(
    expensesViewModel: ExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    tagsViewModel: TagsViewModel,
    navController: NavController,
) {
    val categories by categoriesViewModel.categories.collectAsState()
    val tags by tagsViewModel.tags.collectAsState()
    val initial = expensesViewModel.filter

    // "" = all types, otherwise "EXPENSE" / "INCOME"
    var type by remember { mutableStateOf(initial.type ?: "") }
    var categoryId by remember { mutableStateOf(initial.categoryId) }
    var subcategoryId by remember { mutableStateOf(initial.subcategoryId) }
    var minAmount by remember { mutableStateOf(initial.minAmount ?: "") }
    var maxAmount by remember { mutableStateOf(initial.maxAmount ?: "") }
    var from by remember { mutableStateOf(initial.from) }
    var to by remember { mutableStateOf(initial.to) }
    var tagId by remember { mutableStateOf(initial.tagId) }

    val minValid = minAmount.isBlank() || normalizeAmount(minAmount) != null
    val maxValid = maxAmount.isBlank() || normalizeAmount(maxAmount) != null

    val visibleCategories = remember(categories, type) {
        if (type.isBlank()) categories else categories.filter { it.type == type }
    }

    val activeCount = listOf(
        type.isNotBlank(),
        categoryId != null || subcategoryId != null,
        tagId != null,
        normalizeAmount(minAmount) != null,
        normalizeAmount(maxAmount) != null,
        from != null,
        to != null,
    ).count { it }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.screen_filter_expenses),
                navigationIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            ) {
                PillChip(
                    text = stringResource(Res.string.filter_reset),
                    selected = false,
                    onClick = {
                        type = ""
                        categoryId = null
                        subcategoryId = null
                        minAmount = ""
                        maxAmount = ""
                        from = null
                        to = null
                        tagId = null
                    },
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(NightBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = ScreenGutter, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = when (activeCount) {
                        0 -> stringResource(Res.string.filter_apply)
                        1 -> stringResource(Res.string.expenses_apply_one)
                        else -> stringResource(Res.string.expenses_apply_many, activeCount)
                    },
                    onClick = {
                        expensesViewModel.applyFilter(
                            ExpenseFilter(
                                categoryId = categoryId,
                                subcategoryId = subcategoryId,
                                type = type.ifBlank { null },
                                from = from,
                                to = to,
                                minAmount = normalizeAmount(minAmount),
                                maxAmount = normalizeAmount(maxAmount),
                                tagId = tagId,
                            )
                        )
                        navController.popBackStack()
                    },
                    enabled = minValid && maxValid,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Type
            FilterSection(stringResource(Res.string.filter_type))
            SlidingToggle(
                options = listOf(
                    "" to stringResource(Res.string.filter_type_all),
                    "EXPENSE" to stringResource(Res.string.label_expense),
                    "INCOME" to stringResource(Res.string.label_income),
                ),
                selected = type,
                onSelect = { newType ->
                    type = newType
                    // Drop a category selection that no longer matches the chosen type.
                    if (newType.isNotBlank()) {
                        val stillValid = categories.any {
                            it.type == newType && (it.id == categoryId ||
                                it.subcategories.any { s -> s.id == subcategoryId })
                        }
                        if (!stillValid) {
                            categoryId = null
                            subcategoryId = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Category / subcategory and tag (collapsed by default)
            FilterSection(
                stringResource(if (tags.isNotEmpty()) Res.string.expenses_category_and_tag else Res.string.filter_category)
            )
            var categoryExpanded by remember { mutableStateOf(false) }
            val selectedSub = subcategoryId?.let { id ->
                categories.flatMap { it.subcategories }.find { it.id == id }
            }
            val selectedCat = categoryId?.let { id -> categories.find { it.id == id } }
            val hasSelection = selectedSub != null || selectedCat != null
            val selectionLabel = selectedSub?.name ?: selectedCat?.name
                ?: stringResource(Res.string.filter_all_categories)
            val selectionIcon = selectedSub?.icon ?: selectedCat?.icon
            val selectionColor = selectedSub?.color ?: selectedCat?.color

            DropdownRow(
                label = stringResource(Res.string.filter_category),
                value = selectionLabel,
                hasValue = hasSelection,
                expanded = categoryExpanded,
                onClick = { categoryExpanded = !categoryExpanded },
                icon = if (hasSelection) selectionIcon else null,
                iconColor = selectionColor,
            )
            if (categoryExpanded) {
                CategoryFilterList(
                    categories = visibleCategories,
                    selectedCategoryId = categoryId,
                    selectedSubcategoryId = subcategoryId,
                    onSelectAll = { categoryId = null; subcategoryId = null; categoryExpanded = false },
                    onSelectCategory = { id -> categoryId = id; subcategoryId = null; categoryExpanded = false },
                    onSelectSubcategory = { id -> subcategoryId = id; categoryId = null; categoryExpanded = false },
                )
            }

            if (tags.isNotEmpty()) {
                var tagExpanded by remember { mutableStateOf(false) }
                val selectedTag = tagId?.let { id -> tags.find { it.id == id } }
                DropdownRow(
                    label = stringResource(Res.string.filter_tag),
                    value = selectedTag?.label ?: stringResource(Res.string.filter_all_tags),
                    hasValue = selectedTag != null,
                    expanded = tagExpanded,
                    onClick = { tagExpanded = !tagExpanded },
                )
                if (tagExpanded) {
                    TagFilterList(
                        tags = tags,
                        selectedTagId = tagId,
                        onSelectAll = { tagId = null; tagExpanded = false },
                        onSelectTag = { id -> tagId = id; tagExpanded = false },
                    )
                }
            }

            // Amount range
            FilterSection(stringResource(Res.string.filter_amount))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AmountBox(
                    label = stringResource(Res.string.filter_amount_min),
                    value = minAmount,
                    onValueChange = { minAmount = it },
                    isError = !minValid,
                    modifier = Modifier.weight(1f),
                )
                AmountBox(
                    label = stringResource(Res.string.filter_amount_max),
                    value = maxAmount,
                    onValueChange = { maxAmount = it },
                    isError = !maxValid,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!minValid || !maxValid) {
                Text(
                    text = stringResource(Res.string.filter_amount_invalid),
                    color = Danger,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Date range
            FilterSection(stringResource(Res.string.filter_date_range))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DateField(
                    label = stringResource(Res.string.filter_date_from),
                    value = from,
                    onValueChange = { from = it },
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    label = stringResource(Res.string.filter_date_to),
                    value = to,
                    onValueChange = { to = it },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilterSection(text: String) {
    SectionLabel(text = text, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
}

/** Collapsed "Category: Groceries ⌄" row that toggles its option list. */
@Composable
private fun DropdownRow(
    label: String,
    value: String,
    hasValue: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    icon: String? = null,
    iconColor: String? = null,
) {
    FormRow(
        label = label,
        onClick = onClick,
        showChevron = false,
        trailing = {
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = NightTextSecondary,
                modifier = Modifier.size(22.dp),
            )
        },
    ) {
        if (icon != null) {
            Icon(
                imageVector = iconByName(icon),
                contentDescription = null,
                tint = colorHexByName(iconColor),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
            color = if (hasValue) NightTextPrimary else NightTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CategoryFilterList(
    categories: List<CategoryOut>,
    selectedCategoryId: Int?,
    selectedSubcategoryId: Int?,
    onSelectAll: () -> Unit,
    onSelectCategory: (Int) -> Unit,
    onSelectSubcategory: (Int) -> Unit,
) {
    var expandedIds by remember { mutableStateOf(setOf<Int>()) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SelectableRow(
            label = stringResource(Res.string.filter_all_categories),
            selected = selectedCategoryId == null && selectedSubcategoryId == null,
            onClick = onSelectAll,
        )
        categories.forEach { category ->
            val isExpanded = expandedIds.contains(category.id)
            val children = category.subcategories.filter { !it.isDefault }

            SelectableRow(
                label = category.name,
                icon = category.icon,
                color = category.color,
                selected = selectedCategoryId == category.id,
                onClick = { onSelectCategory(category.id) },
                trailing = if (children.isNotEmpty()) {
                    {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = NightTextSecondary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    expandedIds = if (isExpanded) expandedIds - category.id
                                    else expandedIds + category.id
                                }
                                .padding(8.dp)
                                .size(22.dp),
                        )
                    }
                } else null,
            )

            if (isExpanded) {
                children.forEach { child ->
                    SelectableRow(
                        label = child.name,
                        icon = child.icon,
                        color = child.color,
                        selected = selectedSubcategoryId == child.id,
                        onClick = { onSelectSubcategory(child.id) },
                        indent = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagFilterList(
    tags: List<TagOut>,
    selectedTagId: Int?,
    onSelectAll: () -> Unit,
    onSelectTag: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SelectableRow(
            label = stringResource(Res.string.filter_all_tags),
            selected = selectedTagId == null,
            onClick = onSelectAll,
        )
        tags.forEach { tag ->
            SelectableRow(
                label = tag.label,
                selected = selectedTagId == tag.id,
                onClick = { onSelectTag(tag.id) },
            )
        }
    }
}

@Composable
private fun SelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: String? = null,
    color: String? = null,
    indent: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    NightCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 28.dp else 0.dp),
        onClick = onClick,
        selected = selected,
        contentPadding = PaddingValues(start = 16.dp, end = if (trailing != null) 6.dp else 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (indent) 46.dp else 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = iconByName(icon),
                    contentDescription = null,
                    tint = colorHexByName(color),
                    modifier = Modifier.size(if (indent) 20.dp else 22.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = if (indent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = NightTextPrimary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Mint,
                    modifier = Modifier.size(20.dp),
                )
            }
            trailing?.invoke()
        }
    }
}

/** "Min   20 €" card: label on the left, a right-aligned numeric field and the currency. */
@Composable
private fun AmountBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = CardShape
    val textStyle = MaterialTheme.typography.titleMedium
    Row(
        modifier = modifier
            .clip(shape)
            .background(NightCard)
            .border(1.dp, if (isError) DangerBorder else NightBorder, shape)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = NightTextSecondary)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = textStyle.merge(TextStyle(color = NightTextPrimary, textAlign = TextAlign.End)),
            cursorBrush = SolidColor(Mint),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text("—", style = textStyle, color = NightTextTertiary)
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Text(currencySymbol("EUR"), style = MaterialTheme.typography.bodyMedium, color = NightTextTertiary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalAppLocale.current
    var showPicker by remember { mutableStateOf(false) }

    NightCard(
        modifier = modifier,
        onClick = { showPicker = true },
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = NightTextSecondary)
                Text(
                    text = value?.let { formatDate(it, locale) } ?: stringResource(Res.string.expenses_date_any),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value != null) NightTextPrimary else NightTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (value != null) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.cd_clear_date),
                    tint = NightTextSecondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onValueChange(null) }
                        .padding(8.dp)
                        .size(18.dp),
                )
            } else {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = NightTextSecondary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                )
            }
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value?.let { dateStringToMillis(it) }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onValueChange(millisToDateString(it)) }
                    showPicker = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Returns a canonical decimal string (dot separator) if [text] is a valid positive number, else null. */
private fun normalizeAmount(text: String): String? {
    val cleaned = text.trim().replace(',', '.')
    if (cleaned.isBlank()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return cleaned
}

private fun dateStringToMillis(dateStr: String): Long = isoDateToUtcMillis(dateStr)

private fun millisToDateString(millis: Long): String = utcMillisToIsoDate(millis)
