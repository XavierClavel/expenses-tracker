package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.tags.TagsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_filter_expenses)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        type = ""
                        categoryId = null
                        subcategoryId = null
                        minAmount = ""
                        maxAmount = ""
                        from = null
                        to = null
                        tagId = null
                    }) { Text(stringResource(R.string.filter_reset)) }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(stringResource(R.string.filter_apply), fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Type
            SectionLabel(stringResource(R.string.filter_type))
            com.xavierclavel.bankable.ui.SlidingToggle(
                options = listOf(
                    "" to stringResource(R.string.filter_type_all),
                    "EXPENSE" to stringResource(R.string.label_expense),
                    "INCOME" to stringResource(R.string.label_income),
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

            // Category / subcategory (collapsed by default)
            SectionLabel(stringResource(R.string.filter_category))
            var categoryExpanded by remember { mutableStateOf(false) }
            val selectedSub = subcategoryId?.let { id ->
                categories.flatMap { it.subcategories }.find { it.id == id }
            }
            val selectedCat = categoryId?.let { id -> categories.find { it.id == id } }
            val hasSelection = selectedSub != null || selectedCat != null
            val selectionLabel = selectedSub?.name ?: selectedCat?.name
                ?: stringResource(R.string.filter_all_categories)
            val selectionIcon = selectedSub?.icon ?: selectedCat?.icon
            val selectionColor = selectedSub?.color ?: selectedCat?.color

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { categoryExpanded = !categoryExpanded },
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasSelection) {
                        Icon(
                            imageVector = iconByName(selectionIcon),
                            contentDescription = null,
                            tint = colorHexByName(selectionColor),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                    }
                    Text(
                        text = selectionLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (hasSelection) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (categoryExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            }
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

            // Tag (collapsed by default)
            if (tags.isNotEmpty()) {
                SectionLabel(stringResource(R.string.filter_tag))
                var tagExpanded by remember { mutableStateOf(false) }
                val selectedTag = tagId?.let { id -> tags.find { it.id == id } }
                val tagSelectionLabel = selectedTag?.label
                    ?: stringResource(R.string.filter_all_tags)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tagExpanded = !tagExpanded },
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tagSelectionLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedTag != null) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (tagExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                        )
                    }
                }
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
            SectionLabel(stringResource(R.string.filter_amount))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = { minAmount = it },
                    label = { Text(stringResource(R.string.filter_amount_min)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = !minValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = { maxAmount = it },
                    label = { Text(stringResource(R.string.filter_amount_max)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = !maxValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            if (!minValid || !maxValid) {
                Text(
                    text = stringResource(R.string.filter_amount_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Date range
            SectionLabel(stringResource(R.string.filter_date_range))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DateField(
                    label = stringResource(R.string.filter_date_from),
                    value = from,
                    onValueChange = { from = it },
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    label = stringResource(R.string.filter_date_to),
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
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

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        SelectableRow(
            label = stringResource(R.string.filter_all_categories),
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
                        IconButton(onClick = {
                            expandedIds = if (isExpanded) expandedIds - category.id
                            else expandedIds + category.id
                        }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                            )
                        }
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
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        SelectableRow(
            label = stringResource(R.string.filter_all_tags),
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 28.dp else 0.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (selected) 0.dp else if (indent) 1.dp else 2.dp,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (indent) 44.dp else 48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = iconByName(icon),
                    contentDescription = null,
                    tint = colorHexByName(color),
                    modifier = Modifier.size(if (indent) 20.dp else 24.dp),
                )
                Spacer(Modifier.size(12.dp))
            }
            Text(
                text = label,
                style = if (indent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            trailing?.invoke()
        }
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
    val locale = LocalConfiguration.current.locales[0]
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier.height(56.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
    ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            text = value?.let { formatDate(it, locale) } ?: label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            IconButton(
                onClick = { onValueChange(null) },
                modifier = Modifier.size(20.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_clear_date),
                    modifier = Modifier.size(16.dp),
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.action_cancel)) }
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

private fun dateStringToMillis(dateStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

private fun millisToDateString(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(millis))
}
