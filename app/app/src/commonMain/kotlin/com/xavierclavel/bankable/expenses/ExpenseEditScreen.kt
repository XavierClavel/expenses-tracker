package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.xavierclavel.bankable.resources.amount_calc_invalid
import com.xavierclavel.bankable.resources.amount_calc_negative
import com.xavierclavel.bankable.resources.amount_calc_preview
import com.xavierclavel.bankable.resources.dialog_delete_expense_message
import com.xavierclavel.bankable.resources.dialog_delete_expense_title
import com.xavierclavel.bankable.resources.expense_tags_label
import com.xavierclavel.bankable.resources.expenses_add_tag_pill
import com.xavierclavel.bankable.resources.expenses_save_expense
import com.xavierclavel.bankable.resources.expenses_save_income
import com.xavierclavel.bankable.resources.filter_amount
import com.xavierclavel.bankable.resources.label_category
import com.xavierclavel.bankable.resources.label_date
import com.xavierclavel.bankable.resources.label_expense
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.label_title
import com.xavierclavel.bankable.resources.no_category_selected
import com.xavierclavel.bankable.resources.screen_edit_expense
import com.xavierclavel.bankable.resources.screen_new_expense
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.AddPill
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.FormRow
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightTextField
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.TagPill
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.DangerBorder
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
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
        val initial = expense?.amount ?: ""
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    var date by rememberSaveable { mutableStateOf(expense?.date ?: todayString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateStringToMillis(date)
    )

    val computedAmount = remember(amount.text) { ExpressionEvaluator.evaluate(amount.text) }
    val isAmountValid = computedAmount != null && computedAmount > 0
    val currencySym = currencySymbol("EUR")
    val canSave = title.isNotBlank() && isAmountValid

    Scaffold(
        containerColor = NightBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(NightBackground)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = ScreenGutter, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(
                        if (viewModel.selectedType == "INCOME") Res.string.expenses_save_income
                        else Res.string.expenses_save_expense
                    ),
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
                    enabled = canSave,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Close · centered title · delete (when editing)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.action_back),
                    onClick = { navController.popBackStack() },
                )
                Text(
                    text = if (isEditing) stringResource(Res.string.screen_edit_expense)
                    else stringResource(Res.string.screen_new_expense),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NightTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                if (isEditing) {
                    CircleIconButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = stringResource(Res.string.action_delete),
                        onClick = { showDeleteDialog = true },
                        tint = Danger,
                        borderColor = DangerBorder,
                    )
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }

            SlidingToggle(
                options = listOf("EXPENSE" to stringResource(Res.string.label_expense), "INCOME" to stringResource(Res.string.label_income)),
                selected = viewModel.selectedType,
                onSelect = { viewModel.setSelectedType(it) },
                modifier = Modifier.fillMaxWidth(),
            )

            AmountEditor(
                amount = amount,
                onAmountChange = { amount = it },
                computedAmount = computedAmount,
                currencySymbol = currencySym,
            )

            NightTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(Res.string.label_title),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            val locale = LocalAppLocale.current
            FormRow(
                label = stringResource(Res.string.label_date),
                onClick = { showDatePicker = true },
                showChevron = false,
                trailing = {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = NightTextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                },
            ) {
                Text(
                    text = formatDateDisplay(date, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = NightTextPrimary,
                )
            }

            SubcategorySelector(
                subcategoryName = subcategory?.name ?: stringResource(Res.string.no_category_selected),
                subcategoryIcon = subcategory?.icon,
                subcategoryColor = subcategory?.color,
                onClick = { navController.navigate("expense/subcategory-picker") },
            )

            if (tags.isNotEmpty()) {
                val assignedTags = tags.filter { selectedTagIds.contains(it.id) }
                val hasUnassigned = tags.any { !selectedTagIds.contains(it.id) }
                FormRow(
                    label = stringResource(Res.string.expense_tags_label),
                    showChevron = false,
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        assignedTags.forEach { tag ->
                            // Tapping an assigned tag removes it.
                            TagPill(
                                text = tag.label,
                                onClick = { viewModel.toggleTag(tag.id) },
                            )
                        }
                        if (hasUnassigned) {
                            AddPill(
                                text = stringResource(Res.string.expenses_add_tag_pill),
                                onClick = { navController.navigate("expense/tag-picker") },
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Danger,
                    style = MaterialTheme.typography.bodySmall,
                )
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
                ) { Text(stringResource(Res.string.action_delete), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

/**
 * The big centered amount: an editable expression with a muted currency sign, the
 * evaluated result (or why it can't be used) underneath, and operator keys.
 */
@Composable
private fun AmountEditor(
    amount: TextFieldValue,
    onAmountChange: (TextFieldValue) -> Unit,
    computedAmount: Double?,
    currencySymbol: String,
) {
    var focused by remember { mutableStateOf(false) }
    val amountStyle = MaterialTheme.typography.displayLarge

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.filter_amount).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NightTextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        val currencyStyle = MaterialTheme.typography.headlineMedium
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { amountStyle.lineHeight.toDp() }),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Shrink the amount until the whole expression fits beside the currency
            // sign, so nothing scrolls out of view; only past the minimum size does
            // the field scroll.
            val currencyWidth = with(density) { textMeasurer.measure(currencySymbol, currencyStyle).size.width.toDp() }
            val available = maxWidth - currencyWidth - 8.dp - CaretAllowance
            val shown = amount.text.ifEmpty { "0" }
            val fullWidth = with(density) { textMeasurer.measure(shown, amountStyle).size.width.toDp() }
            val scale = if (fullWidth > available) (available / fullWidth).coerceAtLeast(MinAmountScale) else 1f
            val fittedStyle = amountStyle.copy(fontSize = amountStyle.fontSize * scale)
            val textWidth = with(density) { textMeasurer.measure(shown, fittedStyle).size.width.toDp() }

            Row(verticalAlignment = Alignment.Bottom) {
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = fittedStyle.merge(TextStyle(color = NightTextPrimary, textAlign = TextAlign.Center)),
                    cursorBrush = SolidColor(Mint),
                    modifier = Modifier
                        .width((textWidth + CaretAllowance).coerceIn(32.dp, available + CaretAllowance))
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (amount.text.isEmpty()) {
                                Text("0", style = fittedStyle, color = NightTextTertiary)
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currencySymbol,
                    style = currencyStyle,
                    color = NightTextTertiary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }

        val helper: Pair<String, Color>? = when {
            amount.text.isBlank() -> null
            computedAmount == null ->
                stringResource(Res.string.amount_calc_invalid) to Danger
            computedAmount <= 0 ->
                stringResource(
                    Res.string.amount_calc_negative,
                    ExpressionEvaluator.formatAmount(computedAmount),
                    currencySymbol,
                ) to Danger
            ExpressionEvaluator.isExpression(amount.text) ->
                stringResource(
                    Res.string.amount_calc_preview,
                    ExpressionEvaluator.formatAmount(computedAmount),
                    currencySymbol,
                ) to Mint
            else -> null
        }
        Text(
            text = helper?.first ?: "",
            style = MaterialTheme.typography.titleSmall,
            color = helper?.second ?: Mint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp).height(22.dp),
        )

        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("+" to "+", "−" to "-", "×" to "*", "÷" to "/", "%" to "%", "( )" to "()").forEach { (label, op) ->
                OperatorKey(label) {
                    // Without focus the caret position is stale: append instead.
                    val base = if (focused) amount
                    else amount.copy(selection = TextRange(amount.text.length))
                    val inserted = base.insertAtCursor(op)
                    // Leave the caret between the parentheses.
                    onAmountChange(
                        if (op == "()") inserted.copy(selection = TextRange(inserted.selection.start - 1))
                        else inserted
                    )
                }
            }
        }
    }
}

// Room for the caret past the last character, so a field sized to its text never scrolls.
private val CaretAllowance = 4.dp

// The amount shrinks to at most 40 % of its size before the field starts scrolling.
private const val MinAmountScale = 0.4f

@Composable
private fun RowScope.OperatorKey(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(shape)
            .background(NightCard)
            .border(1.dp, NightBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NightTextPrimary)
    }
}

@Composable
internal fun SubcategorySelector(
    subcategoryName: String,
    subcategoryIcon: String?,
    subcategoryColor: String?,
    onClick: () -> Unit,
) {
    FormRow(
        label = stringResource(Res.string.label_category),
        onClick = onClick,
    ) {
        IconTile(
            icon = iconByName(subcategoryIcon),
            color = colorHexByName(subcategoryColor),
            size = 40.dp,
            iconSize = 20.dp,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = subcategoryName,
            style = MaterialTheme.typography.titleSmall,
            color = NightTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
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
