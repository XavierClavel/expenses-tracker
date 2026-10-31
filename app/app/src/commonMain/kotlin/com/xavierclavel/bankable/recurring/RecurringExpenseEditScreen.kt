package com.xavierclavel.bankable.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.iconByName
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
import com.xavierclavel.bankable.resources.dialog_confirm_delete_title
import com.xavierclavel.bankable.resources.dialog_delete_recurring_expense_message
import com.xavierclavel.bankable.resources.expense_tags_label
import com.xavierclavel.bankable.resources.label_category
import com.xavierclavel.bankable.resources.label_expense
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.label_title
import com.xavierclavel.bankable.resources.no_category_selected
import com.xavierclavel.bankable.resources.recurring_add_tag
import com.xavierclavel.bankable.resources.recurring_amount
import com.xavierclavel.bankable.resources.recurring_cd_next_day
import com.xavierclavel.bankable.resources.recurring_cd_previous_day
import com.xavierclavel.bankable.resources.recurring_day_label
import com.xavierclavel.bankable.resources.recurring_due_today_hint
import com.xavierclavel.bankable.resources.recurring_edit_hint
import com.xavierclavel.bankable.resources.recurring_expense_added
import com.xavierclavel.bankable.resources.recurring_short_months_hint
import com.xavierclavel.bankable.resources.screen_edit_recurring_expense
import com.xavierclavel.bankable.resources.screen_new_recurring_expense
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.AddPill
import com.xavierclavel.bankable.ui.DangerButton
import com.xavierclavel.bankable.ui.FormRow
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.TagPill
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightCardRaised
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.ui.theme.OnMint
import com.xavierclavel.bankable.util.ExpressionEvaluator
import com.xavierclavel.bankable.util.todayIsoDate
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
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

    val computedAmount = remember(amount) { ExpressionEvaluator.evaluate(amount) }
    val isAmountValid = computedAmount != null && computedAmount > 0
    val currencySym = currencySymbol("EUR")

    Scaffold(containerColor = NightBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            ScreenHeader(
                title = if (isEditing) stringResource(Res.string.screen_edit_recurring_expense)
                else stringResource(Res.string.screen_new_recurring_expense),
                navigationIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenGutter),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SlidingToggle(
                    options = listOf("EXPENSE" to stringResource(Res.string.label_expense), "INCOME" to stringResource(Res.string.label_income)),
                    selected = viewModel.selectedType,
                    onSelect = { viewModel.setSelectedType(it) },
                    modifier = Modifier.fillMaxWidth(),
                )

                AmountInput(
                    amount = amount,
                    onAmountChange = { amount = it },
                    currencySymbol = currencySym,
                    isError = amount.isNotBlank() && !isAmountValid,
                    supportingText = when {
                        amount.isBlank() -> null
                        computedAmount == null ->
                            stringResource(Res.string.amount_calc_invalid) to false
                        computedAmount <= 0 ->
                            stringResource(
                                Res.string.amount_calc_negative,
                                ExpressionEvaluator.formatAmount(computedAmount),
                                currencySym,
                            ) to false
                        ExpressionEvaluator.isExpression(amount) ->
                            stringResource(
                                Res.string.amount_calc_preview,
                                ExpressionEvaluator.formatAmount(computedAmount),
                                currencySym,
                            ) to true
                        else -> null
                    },
                )

                NightTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(Res.string.label_title),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )

                // On create, picking today's day adds this month's expense right away.
                val dayHints = listOfNotNull(
                    if (!isEditing && dayOfMonth == today) stringResource(Res.string.recurring_due_today_hint) else null,
                    if (dayOfMonth > 28) stringResource(Res.string.recurring_short_months_hint) else null,
                )
                DayOfMonthCard(
                    dayOfMonth = dayOfMonth,
                    hints = dayHints,
                    onClick = { showDayPicker = true },
                    onChange = { dayOfMonth = it },
                )

                FormRow(
                    label = stringResource(Res.string.label_category),
                    onClick = { navController.navigate("recurring/subcategory-picker") },
                ) {
                    if (subcategory != null) {
                        IconTile(
                            icon = iconByName(subcategory.icon),
                            color = colorHexByName(subcategory.color),
                            size = 40.dp,
                            iconSize = 20.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            subcategory.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = NightTextPrimary,
                        )
                    } else {
                        Text(
                            stringResource(Res.string.no_category_selected),
                            style = MaterialTheme.typography.bodyLarge,
                            color = NightTextTertiary,
                        )
                    }
                }

                if (tags.isNotEmpty()) {
                    val assignedTags = tags.filter { selectedTagIds.contains(it.id) }
                    val hasUnassigned = tags.any { !selectedTagIds.contains(it.id) }
                    FormRow(label = stringResource(Res.string.expense_tags_label), showChevron = false) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            assignedTags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { viewModel.toggleTag(tag.id) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TagPill(text = tag.label)
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(Res.string.batch_remove_tag),
                                        tint = NightTextSecondary,
                                        modifier = Modifier.padding(start = 2.dp).size(16.dp),
                                    )
                                }
                            }
                            if (hasUnassigned) {
                                AddPill(
                                    text = stringResource(Res.string.recurring_add_tag),
                                    onClick = { navController.navigate("recurring/tag-picker") },
                                )
                            }
                        }
                    }
                }

                if (isEditing) {
                    Text(
                        text = stringResource(Res.string.recurring_edit_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NightTextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Danger,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter)
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.action_save),
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
                    enabled = title.isNotBlank() && isAmountValid,
                )
                if (isEditing) {
                    DangerButton(
                        text = stringResource(Res.string.action_delete),
                        onClick = { showDeleteDialog = true },
                    )
                }
            }
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
                ) { Text(stringResource(Res.string.action_delete), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

/**
 * Centered "AMOUNT" label over a huge editable amount and a muted currency symbol.
 * [supportingText] is the message plus whether it's the positive (preview) kind.
 */
@Composable
private fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    isError: Boolean,
    supportingText: Pair<String, Boolean>?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.recurring_amount).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isError) Danger else NightTextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            val style = MaterialTheme.typography.displayLarge
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = style.merge(TextStyle(color = NightTextPrimary, textAlign = TextAlign.Center)),
                cursorBrush = SolidColor(Mint),
                modifier = Modifier.widthIn(min = 40.dp).width(IntrinsicSize.Min),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (amount.isEmpty()) Text("0", style = style, color = NightTextTertiary)
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(8.dp))
            Text(
                currencySymbol,
                style = MaterialTheme.typography.headlineMedium,
                color = NightTextTertiary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (supportingText != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                supportingText.first,
                style = MaterialTheme.typography.bodyMedium,
                color = if (supportingText.second) Mint else Danger,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "Day of the month" card: tap to open the day grid, or nudge with the −/+ buttons. */
@Composable
private fun DayOfMonthCard(
    dayOfMonth: Int,
    hints: List<String>,
    onClick: () -> Unit,
    onChange: (Int) -> Unit,
) {
    NightCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.recurring_day_label),
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextSecondary,
                modifier = Modifier.width(110.dp),
            )
            Text(
                dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NightTextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SquareIconButton(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(Res.string.recurring_cd_previous_day),
                enabled = dayOfMonth > 1,
                onClick = { onChange(dayOfMonth - 1) },
            )
            Spacer(Modifier.width(8.dp))
            SquareIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(Res.string.recurring_cd_next_day),
                enabled = dayOfMonth < 31,
                onClick = { onChange(dayOfMonth + 1) },
            )
        }
        hints.forEach { hint ->
            Spacer(Modifier.height(8.dp))
            Text(hint, style = MaterialTheme.typography.bodySmall, color = NightTextSecondary)
        }
    }
}

@Composable
private fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .background(NightCardRaised)
            .border(1.dp, NightBorder, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) NightTextPrimary else NightTextTertiary,
            modifier = Modifier.size(20.dp),
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
                                    .clip(CircleShape)
                                    .clickable { onPick(day) },
                                shape = CircleShape,
                                color = if (isSelected) Mint else NightCardRaised,
                                contentColor = if (isSelected) OnMint else NightTextPrimary,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
