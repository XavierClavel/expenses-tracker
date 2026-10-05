package com.xavierclavel.bankable.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.expenses.SubcategorySelector
import com.xavierclavel.bankable.platform.showToast
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.action_cancel
import com.xavierclavel.bankable.resources.action_delete
import com.xavierclavel.bankable.resources.action_save
import com.xavierclavel.bankable.resources.amount_calc_invalid
import com.xavierclavel.bankable.resources.amount_calc_negative
import com.xavierclavel.bankable.resources.amount_calc_preview
import com.xavierclavel.bankable.resources.batch_remove_tag
import com.xavierclavel.bankable.resources.cd_add_tag
import com.xavierclavel.bankable.resources.dialog_confirm_delete_title
import com.xavierclavel.bankable.resources.dialog_delete_recurring_expense_message
import com.xavierclavel.bankable.resources.expense_tags_label
import com.xavierclavel.bankable.resources.label_amount
import com.xavierclavel.bankable.resources.label_expense
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.label_title
import com.xavierclavel.bankable.resources.no_category_selected
import com.xavierclavel.bankable.resources.recurring_day_label
import com.xavierclavel.bankable.resources.recurring_day_value
import com.xavierclavel.bankable.resources.recurring_due_today_hint
import com.xavierclavel.bankable.resources.recurring_edit_hint
import com.xavierclavel.bankable.resources.recurring_expense_added
import com.xavierclavel.bankable.resources.recurring_short_months_hint
import com.xavierclavel.bankable.resources.screen_edit_recurring_expense
import com.xavierclavel.bankable.resources.screen_new_recurring_expense
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.util.ExpressionEvaluator
import com.xavierclavel.bankable.util.todayIsoDate
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringExpenseEditScreen(
    viewModel: RecurringExpensesViewModel,
    tagsViewModel: TagsViewModel,
    navController: NavController,
    onExpenseCreated: () -> Unit,
) {
    val recurringExpense = viewModel.selectedRecurringExpense
    val isEditing = recurringExpense != null
    val subcategory = viewModel.selectedSubcategory
    val tags by tagsViewModel.tags.collectAsState()
    val selectedTagIds = viewModel.selectedTagIds
    // Same clock as the backend's stored dates (see util/Dates.kt).
    val today = remember { todayIsoDate().substring(8, 10).toInt() }

    var title by rememberSaveable { mutableStateOf(recurringExpense?.title ?: "") }
    var amount by rememberSaveable { mutableStateOf(recurringExpense?.amount ?: "") }
    var dayOfMonth by rememberSaveable { mutableStateOf(recurringExpense?.dayOfMonth ?: today) }
    var showDayPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val expenseAddedMessage = stringResource(Res.string.recurring_expense_added)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) stringResource(Res.string.screen_edit_recurring_expense)
                        else stringResource(Res.string.screen_new_recurring_expense)
                    )
                },
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
                },
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
                options = listOf("EXPENSE" to stringResource(Res.string.label_expense), "INCOME" to stringResource(Res.string.label_income)),
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

            val computedAmount = remember(amount) { ExpressionEvaluator.evaluate(amount) }
            val isAmountValid = computedAmount != null && computedAmount > 0
            val currencySym = currencySymbol("EUR")

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(Res.string.label_amount)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = amount.isNotBlank() && !isAmountValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    when {
                        amount.isBlank() -> {}
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
                        ExpressionEvaluator.isExpression(amount) ->
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
            )

            OutlinedTextField(
                value = stringResource(Res.string.recurring_day_value, dayOfMonth),
                onValueChange = {},
                label = { Text(stringResource(Res.string.recurring_day_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDayPicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(Icons.Default.EventRepeat, contentDescription = null)
                },
                supportingText = {
                    // On create, picking today's day adds this month's expense right away.
                    val hints = listOfNotNull(
                        if (!isEditing && dayOfMonth == today) stringResource(Res.string.recurring_due_today_hint) else null,
                        if (dayOfMonth > 28) stringResource(Res.string.recurring_short_months_hint) else null,
                    )
                    if (hints.isNotEmpty()) Text(hints.joinToString("\n"))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            SubcategorySelector(
                subcategoryName = subcategory?.name ?: stringResource(Res.string.no_category_selected),
                subcategoryIcon = subcategory?.icon,
                subcategoryColor = subcategory?.color,
                onClick = { navController.navigate("recurring/subcategory-picker") },
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
                            onClick = { navController.navigate("recurring/tag-picker") },
                            label = { Text(stringResource(Res.string.cd_add_tag)) },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                        )
                    }
                }
            }

            if (isEditing) {
                Text(
                    text = stringResource(Res.string.recurring_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    viewModel.saveRecurringExpense(
                        title = title,
                        amount = ExpressionEvaluator.formatAmount(computedAmount!!),
                        dayOfMonth = dayOfMonth,
                        onSuccess = { createdExpense ->
                            if (createdExpense) {
                                onExpenseCreated()
                                showToast(expenseAddedMessage)
                            }
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

    if (showDayPicker) {
        DayOfMonthPickerDialog(
            selected = dayOfMonth,
            onPick = {
                dayOfMonth = it
                showDayPicker = false
            },
            onDismiss = { showDayPicker = false },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.dialog_confirm_delete_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_recurring_expense_message, recurringExpense?.title ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteRecurringExpense(
                            onSuccess = { navController.popBackStack() },
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

/** A calendar-like grid of the days 1 to 31; tapping a day picks it. */
@Composable
private fun DayOfMonthPickerDialog(
    selected: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recurring_day_label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..31).chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day ->
                            val isSelected = day == selected
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { onPick(day) },
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                        // Pad the last, partial week so its cells keep the same width.
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}
