package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.action_ok
import com.xavierclavel.bankable.resources.action_save
import com.xavierclavel.bankable.resources.amount_calc_invalid
import com.xavierclavel.bankable.resources.amount_calc_negative
import com.xavierclavel.bankable.resources.amount_calc_preview
import com.xavierclavel.bankable.resources.batch_remove_tag
import com.xavierclavel.bankable.resources.cd_add_tag
import com.xavierclavel.bankable.resources.cd_operators
import com.xavierclavel.bankable.resources.dialog_delete_expense_message
import com.xavierclavel.bankable.resources.dialog_delete_expense_title
import com.xavierclavel.bankable.resources.expense_tags_label
import com.xavierclavel.bankable.resources.label_amount
import com.xavierclavel.bankable.resources.label_date
import com.xavierclavel.bankable.resources.label_expense
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.label_title
import com.xavierclavel.bankable.resources.no_category_selected
import com.xavierclavel.bankable.resources.screen_edit_expense
import com.xavierclavel.bankable.resources.screen_new_expense
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.util.ExpressionEvaluator
import com.xavierclavel.bankable.util.formatIsoDateLong
import com.xavierclavel.bankable.util.isoDateToUtcMillis
import com.xavierclavel.bankable.util.todayIsoDate
import com.xavierclavel.bankable.util.utcMillisToIsoDate
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditScreen(
    viewModel: ExpensesViewModel,
    tagsViewModel: TagsViewModel,
    navController: NavController,
) {
    val expense = viewModel.selectedExpense
    val isEditing = expense != null
    val subcategory = viewModel.selectedSubcategory
    val tags by tagsViewModel.tags.collectAsState()
    val selectedTagIds = viewModel.selectedTagIds

    var title by rememberSaveable { mutableStateOf(expense?.title ?: "") }
    var amount by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(expense?.amount ?: ""))
    }
    var date by rememberSaveable { mutableStateOf(expense?.date ?: todayString()) }
    var showOperators by remember { mutableStateOf(false) }
    var amountFocused by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateStringToMillis(date)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) stringResource(Res.string.screen_edit_expense) else stringResource(Res.string.screen_new_expense)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SlidingToggle(
                options  = listOf("EXPENSE" to stringResource(Res.string.label_expense), "INCOME" to stringResource(Res.string.label_income)),
                selected = viewModel.selectedType,
                onSelect = { viewModel.setSelectedType(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(Res.string.label_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            val computedAmount = remember(amount.text) { ExpressionEvaluator.evaluate(amount.text) }
            val isAmountValid = computedAmount != null && computedAmount > 0
            val currencySym = currencySymbol("EUR")

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(Res.string.label_amount)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { amountFocused = it.isFocused },
                singleLine = true,
                isError = amount.text.isNotBlank() && !isAmountValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    when {
                        amount.text.isBlank() -> {}
                        computedAmount == null ->
                            Text(stringResource(Res.string.amount_calc_invalid))
                        computedAmount <= 0 ->
                            Text(
                                stringResource(
                                    Res.string.amount_calc_negative,
                                    ExpressionEvaluator.formatAmount(computedAmount),
                                    currencySym,
                                )
                            )
                        ExpressionEvaluator.isExpression(amount.text) ->
                            Text(
                                stringResource(
                                    Res.string.amount_calc_preview,
                                    ExpressionEvaluator.formatAmount(computedAmount),
                                    currencySym,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                    }
                },
                trailingIcon = if (amountFocused || showOperators) {
                    {
                        Box {
                            IconButton(onClick = { showOperators = true }) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = stringResource(Res.string.cd_operators),
                                )
                            }
                            DropdownMenu(
                                expanded = showOperators,
                                onDismissRequest = { showOperators = false },
                            ) {
                                listOf("+" to "+", "−" to "-", "×" to "*", "÷" to "/").forEach { (label, op) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            amount = amount.insertAtCursor(op)
                                            showOperators = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else null,
            )

            val locale = LocalAppLocale.current
            OutlinedTextField(
                value = formatDateDisplay(date, locale),
                onValueChange = {},
                label = { Text(stringResource(Res.string.label_date)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor          = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor        = MaterialTheme.colorScheme.outline,
                    disabledLabelColor         = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            SubcategorySelector(
                subcategoryName = subcategory?.name ?: stringResource(Res.string.no_category_selected),
                subcategoryIcon = subcategory?.icon,
                subcategoryColor = subcategory?.color,
                onClick = { navController.navigate("expense/subcategory-picker") },
            )

            if (tags.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.expense_tags_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val assignedTags = tags.filter { selectedTagIds.contains(it.id) }
                val hasUnassigned = tags.any { !selectedTagIds.contains(it.id) }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    assignedTags.forEach { tag ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.toggleTag(tag.id) },
                            label = { Text(tag.label) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.batch_remove_tag),
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                    if (hasUnassigned) {
                        AssistChip(
                            onClick = { navController.navigate("expense/tag-picker") },
                            label = { Text(stringResource(Res.string.cd_add_tag)) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    viewModel.saveExpense(
                        title = title,
                        amount = ExpressionEvaluator.formatAmount(computedAmount!!),
                        date = date,
                        onSuccess = {
                            tagsViewModel.refreshTagExpenses()
                            navController.popBackStack()
                        },
                        onError = { errorMessage = it },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && isAmountValid,
            ) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = millisToDateString(it) }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.dialog_delete_expense_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_expense_message, expense?.title ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteExpense(
                            onSuccess = {
                                tagsViewModel.refreshTagExpenses()
                                navController.popBackStack()
                            },
                            onError = { errorMessage = it },
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(Res.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SubcategorySelector(
    subcategoryName: String,
    subcategoryIcon: String?,
    subcategoryColor: String?,
    onClick: () -> Unit,
) {
    val iconColor = colorHexByName(subcategoryColor)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconByName(subcategoryIcon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = subcategoryName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            )
        }
    }
}

/** Replaces the current selection (or inserts at the caret) with [insert] and places the caret after it. */
private fun TextFieldValue.insertAtCursor(insert: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val newText = text.substring(0, start) + insert + text.substring(end)
    val caret = start + insert.length
    return TextFieldValue(text = newText, selection = TextRange(caret))
}

private fun todayString(): String = todayIsoDate()

private fun formatDateDisplay(dateStr: String, locale: String): String =
    formatIsoDateLong(dateStr, locale)

private fun dateStringToMillis(dateStr: String): Long = isoDateToUtcMillis(dateStr)

private fun millisToDateString(millis: Long): String = utcMillisToIsoDate(millis)
