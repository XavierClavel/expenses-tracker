package com.xavierclavel.expenses_tracker.summary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xavierclavel.expenses_tracker.api.apiListExpenses
import com.xavierclavel.expenses_tracker.categories.CategoriesViewModel
import com.xavierclavel.expenses_tracker.constants.colorHexByName
import com.xavierclavel.expenses_tracker.constants.iconByName
import com.xavierclavel.expenses_tracker.model.ExpenseOut
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private val MONTH_NAMES = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December",
)

data class SubcategoryEntry(
    val subcategoryId: Int,
    val value: Float,
    val label: String,
    val colorHex: String?,
    val icon: String?,
)

data class PieEntry(
    val categoryId: Int,
    val value: Float,
    val label: String,
    val colorHex: String?,
    val icon: String?,
    val subcategoryEntries: List<SubcategoryEntry> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    categoriesViewModel: CategoriesViewModel,
) {
    val categories by categoriesViewModel.categories.collectAsState()
    val summary    = viewModel.summary
    val isLoading  = viewModel.isLoading

    val subcategoryParentMap = remember(categories) {
        categories.flatMap { cat -> cat.subcategories.map { sub -> sub.id to cat } }.toMap()
    }
    val subcategoryMap = remember(categories) {
        categories.flatMap { cat -> cat.subcategories.map { sub -> sub.id to sub } }.toMap()
    }

    val subcatSummaries = if (viewModel.selectedType == "EXPENSE")
        summary?.expensesByCategory ?: emptyList()
    else
        summary?.incomeByCategory ?: emptyList()

    val pieEntries = remember(categories, subcatSummaries, viewModel.selectedType) {
        categories
            .filter { it.type == viewModel.selectedType }
            .mapNotNull { cat ->
                val catSubs = subcatSummaries.filter { subcategoryParentMap[it.categoryId]?.id == cat.id }
                val total   = catSubs.sumOf { it.total.toDoubleOrNull() ?: 0.0 }
                if (total <= 0.0) null
                else {
                    val subEntries = catSubs.mapNotNull { sub ->
                        val subOut = subcategoryMap[sub.categoryId] ?: return@mapNotNull null
                        SubcategoryEntry(
                            subcategoryId = sub.categoryId,
                            value         = sub.total.toFloatOrNull() ?: 0f,
                            label         = subOut.name,
                            colorHex      = subOut.color,
                            icon          = subOut.icon,
                        )
                    }.filter { it.value > 0f }.sortedByDescending { it.value }
                    PieEntry(
                        categoryId         = cat.id,
                        value              = total.toFloat(),
                        label              = cat.name,
                        colorHex           = cat.color,
                        icon               = cat.icon,
                        subcategoryEntries = subEntries,
                    )
                }
            }
            .sortedByDescending { it.value }
    }

    val periodKey = when (viewModel.timescale) {
        "year" -> viewModel.selectedYear * 12
        else   -> viewModel.selectedYear * 12 + viewModel.selectedMonth
    }

    // Date range for expense list bottom sheet
    val (dateFrom, dateTo) = remember(viewModel.selectedYear, viewModel.selectedMonth, viewModel.timescale) {
        computeDateRange(viewModel.selectedYear, viewModel.selectedMonth, viewModel.timescale)
    }

    // Bottom sheet state — outside AnimatedContent so it survives period transitions
    var bottomSheetEntry by remember { mutableStateOf<SubcategoryEntry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(viewModel.selectedYear, viewModel.selectedMonth, viewModel.timescale) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        when {
                            totalDragX >  120f -> viewModel.previousPeriod()
                            totalDragX < -120f -> viewModel.nextPeriod()
                        }
                    }
                ) { _, dragAmount -> totalDragX += dragAmount }
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Date navigation ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { viewModel.previousPeriod() }, enabled = viewModel.canGoBack()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
            }
            Text(
                text = if (viewModel.timescale == "month")
                    "${MONTH_NAMES[viewModel.selectedMonth - 1]} ${viewModel.selectedYear}"
                else viewModel.selectedYear.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { viewModel.nextPeriod() }, enabled = viewModel.canGoForward()) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Timescale + type toggles ───────────────────────────────────────
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("month" to "Month", "year" to "Year").forEachIndexed { i, (v, l) ->
                SegmentedButton(
                    selected = viewModel.timescale == v,
                    onClick  = { viewModel.setTimescale(v) },
                    shape    = SegmentedButtonDefaults.itemShape(i, 2),
                    label    = { Text(l) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("EXPENSE" to "Expenses", "INCOME" to "Income").forEachIndexed { i, (v, l) ->
                SegmentedButton(
                    selected = viewModel.selectedType == v,
                    onClick  = { viewModel.setSelectedType(v) },
                    shape    = SegmentedButtonDefaults.itemShape(i, 2),
                    label    = { Text(l) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Animated data section ──────────────────────────────────────────
        AnimatedContent(
            targetState = periodKey,
            transitionSpec = {
                val forward = targetState > initialState
                slideInHorizontally(
                    initialOffsetX = { if (forward) it else -it },
                    animationSpec  = tween(320, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(200)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { if (forward) -it else it },
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(150))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { _ ->
            // Both selectedSlice and expandedCategories reset per period
            var selectedSlice        by remember { mutableIntStateOf(-1) }
            var expandedCategories   by remember { mutableStateOf(setOf<Int>()) }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Total cards
                if (isLoading) {
                    val brush = shimmerBrush()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f).height(64.dp).clip(MaterialTheme.shapes.medium).background(brush))
                        Box(Modifier.weight(1f).height(64.dp).clip(MaterialTheme.shapes.medium).background(brush))
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TotalCard("Expenses", summary?.totalExpenses?.toDoubleOrNull() ?: 0.0, Color(0xFFE53935), "-", Modifier.weight(1f))
                        TotalCard("Income",   summary?.totalIncome?.toDoubleOrNull()   ?: 0.0, Color(0xFF4CAF50), "+", Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(16.dp))

                when {
                    isLoading         -> SummarySkeleton()
                    pieEntries.isEmpty() -> Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("No data for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        val pieTotal = pieEntries.sumOf { it.value.toDouble() }.toFloat()

                        DonutChart(
                            entries      = pieEntries,
                            total        = pieTotal,
                            selectedIndex = selectedSlice,
                            onSliceClick = { i -> selectedSlice = if (selectedSlice == i) -1 else i },
                            modifier     = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
                        )

                        Spacer(Modifier.height(16.dp))

                        pieEntries.forEachIndexed { index, entry ->
                            CategoryLegendRow(
                                entry       = entry,
                                total       = pieTotal,
                                isSelected  = index == selectedSlice,
                                isExpanded  = expandedCategories.contains(entry.categoryId),
                                onSelect    = { selectedSlice = if (selectedSlice == index) -1 else index },
                                onToggleExpand = {
                                    expandedCategories =
                                        if (expandedCategories.contains(entry.categoryId))
                                            expandedCategories - entry.categoryId
                                        else
                                            expandedCategories + entry.categoryId
                                },
                                onSubcategoryClick = { sub -> bottomSheetEntry = sub },
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Expense list bottom sheet ──────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (bottomSheetEntry != null) {
        ModalBottomSheet(
            onDismissRequest = { bottomSheetEntry = null },
            sheetState       = sheetState,
        ) {
            ExpenseListSheetContent(
                entry    = bottomSheetEntry!!,
                dateFrom = dateFrom,
                dateTo   = dateTo,
            )
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun TotalCard(label: String, amount: Double, color: Color, sign: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$sign${formatSummaryAmount(amount)}", style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DonutChart(
    entries: List<PieEntry>,
    total: Float,
    selectedIndex: Int,
    onSliceClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anySelected  = selectedIndex != -1
    val selectedEntry = entries.getOrNull(selectedIndex)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(entries, total) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val dx = tap.x - cx;      val dy = tap.y - cy
                    val dist   = sqrt(dx * dx + dy * dy)
                    val outerR = minOf(size.width, size.height) / 2f
                    val strokeW = outerR * 0.28f
                    if (dist < outerR - strokeW || dist > outerR) {
                        onSliceClick(-1); return@detectTapGestures
                    }
                    val rawDeg   = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
                    val tapAngle = ((rawDeg + 90f) % 360f + 360f) % 360f
                    var cum = 0f
                    entries.forEachIndexed { i, e ->
                        val sweep = 360f * e.value / total
                        if (tapAngle >= cum && tapAngle < cum + sweep) { onSliceClick(i); return@detectTapGestures }
                        cum += sweep
                    }
                    onSliceClick(-1)
                }
            }
        ) {
            val diameter   = minOf(size.width, size.height)
            val strokeBase = diameter / 2f * 0.28f
            val gapDeg     = 1.5f
            var startAngle = -90f
            entries.forEachIndexed { i, entry ->
                val fraction   = entry.value / total
                val sweepAngle = 360f * fraction - gapDeg
                val isSelected = i == selectedIndex
                val alpha  = when { !anySelected -> 1f; isSelected -> 1f; else -> 0.25f }
                val stroke = if (isSelected) strokeBase * 1.15f else strokeBase
                val inset  = stroke / 2f
                drawArc(
                    color      = colorHexByName(entry.colorHex).copy(alpha = alpha),
                    startAngle = startAngle + gapDeg / 2f,
                    sweepAngle = sweepAngle.coerceAtLeast(0.1f),
                    useCenter  = false,
                    topLeft    = Offset((size.width - diameter) / 2f + inset, (size.height - diameter) / 2f + inset),
                    size       = Size(diameter - stroke, diameter - stroke),
                    style      = Stroke(width = stroke),
                )
                startAngle += 360f * fraction
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
            Text(
                text       = if (selectedEntry != null) formatSummaryAmount(selectedEntry.value.toDouble())
                             else formatSummaryAmount(total.toDouble()),
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = if (selectedEntry != null) colorHexByName(selectedEntry.colorHex)
                             else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text      = selectedEntry?.label ?: "Total",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategoryLegendRow(
    entry: PieEntry,
    total: Float,
    isSelected: Boolean,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onSubcategoryClick: (SubcategoryEntry) -> Unit,
) {
    val pct        = if (total > 0f) entry.value / total * 100f else 0f
    val fraction   = (entry.value / total).coerceIn(0f, 1f)
    val entryColor = colorHexByName(entry.colorHex)
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val hasSubs    = entry.subcategoryEntries.isNotEmpty()

    Column {
        Surface(
            modifier      = Modifier.fillMaxWidth().clickable(onClick = onSelect),
            shape         = MaterialTheme.shapes.medium,
            color         = if (isSelected) entryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
            tonalElevation = if (isSelected) 0.dp else 2.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(iconByName(entry.icon), null, tint = entryColor, modifier = Modifier.size(22.dp))
                    Text(
                        text       = entry.label,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier   = Modifier.weight(1f),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Text("%.1f%%".format(pct), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                    Text(formatSummaryAmount(entry.value.toDouble()), style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = entryColor, modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
                    if (hasSubs) {
                        IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Spacer(Modifier.width(32.dp))
                    }
                }
                Canvas(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp).height(4.dp)) {
                    val r = CornerRadius(2.dp.toPx())
                    drawRoundRect(color = trackColor, cornerRadius = r)
                    if (fraction > 0f) drawRoundRect(color = entryColor, size = Size(size.width * fraction, size.height), cornerRadius = r)
                }
            }
        }

        if (isExpanded) {
            entry.subcategoryEntries.forEach { sub ->
                Spacer(Modifier.height(3.dp))
                SubcategoryLegendRow(
                    subEntry      = sub,
                    categoryTotal = entry.value,
                    onClick       = { onSubcategoryClick(sub) },
                )
            }
        }
    }
}

@Composable
private fun SubcategoryLegendRow(
    subEntry: SubcategoryEntry,
    categoryTotal: Float,
    onClick: () -> Unit,
) {
    val pct        = if (categoryTotal > 0f) subEntry.value / categoryTotal * 100f else 0f
    val fraction   = (subEntry.value / categoryTotal).coerceIn(0f, 1f)
    val color      = colorHexByName(subEntry.colorHex)
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier       = Modifier.fillMaxWidth().padding(start = 20.dp).clickable(onClick = onClick),
        shape          = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(iconByName(subEntry.icon), null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    text     = subEntry.label,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("%.1f%%".format(pct), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                Text(formatSummaryAmount(subEntry.value.toDouble()), style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Canvas(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp).height(3.dp)) {
                val r = CornerRadius(1.5.dp.toPx())
                drawRoundRect(color = trackColor, cornerRadius = r)
                if (fraction > 0f) drawRoundRect(color = color, size = Size(size.width * fraction, size.height), cornerRadius = r)
            }
        }
    }
}

// ── Expense list bottom sheet ──────────────────────────────────────────────────

@Composable
private fun ExpenseListSheetContent(
    entry: SubcategoryEntry,
    dateFrom: String,
    dateTo: String,
) {
    var expenses  by remember { mutableStateOf<List<ExpenseOut>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val color = colorHexByName(entry.colorHex)

    LaunchedEffect(entry.subcategoryId) {
        isLoading = true
        try {
            expenses = apiListExpenses(0, 100, entry.subcategoryId, dateFrom, dateTo)
        } catch (_: Exception) {
            expenses = emptyList()
        }
        isLoading = false
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(iconByName(entry.icon), null, tint = color, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(formatSummaryAmount(entry.value.toDouble()),
                    style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            expenses.isEmpty() -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("No expenses for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> expenses.forEach { expense ->
                ExpenseSheetRow(expense)
            }
        }
    }
}

@Composable
private fun ExpenseSheetRow(expense: ExpenseOut) {
    val amountColor = if (expense.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFE53935)
    val sign        = if (expense.type == "INCOME") "+" else "-"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text     = formatDateShort(expense.date),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text     = expense.title,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text       = "$sign${expense.amount} ${expense.currency}",
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = amountColor,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

// ── Skeleton loader ────────────────────────────────────────────────────────────

@Composable
private fun shimmerBrush(): Brush {
    val base       = MaterialTheme.colorScheme.surfaceVariant
    val highlight  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1800f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label         = "shimmer_x",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start  = Offset(offset - 600f, 0f),
        end    = Offset(offset + 600f, 0f),
    )
}

@Composable
private fun SummarySkeleton() {
    val brush = shimmerBrush()
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter    = minOf(size.width, size.height)
                val strokeWidth = diameter / 2f * 0.28f
                val inset       = strokeWidth / 2f
                drawArc(
                    brush = brush, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size    = Size(diameter - strokeWidth, diameter - strokeWidth),
                    style   = Stroke(width = strokeWidth),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        repeat(4) {
            Box(Modifier.fillMaxWidth().height(72.dp).clip(MaterialTheme.shapes.medium).background(brush))
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun computeDateRange(year: Int, month: Int, timescale: String): Pair<String, String> {
    return if (timescale == "month") {
        val from = "%04d-%02d-01".format(year, month)
        val lastDay = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
        from to "%04d-%02d-%02d".format(year, month, lastDay)
    } else {
        "%04d-01-01".format(year) to "%04d-12-31".format(year)
    }
}

private fun formatSummaryAmount(value: Double): String {
    val number = when {
        value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)}M"
        value >= 10_000    -> "${(value / 1_000).toInt()}k"
        value >= 1_000     -> "${"%.1f".format(value / 1_000)}k"
        else               -> "${"%.0f".format(value)}"
    }
    return "$number €"
}

private fun formatDateShort(dateStr: String): String {
    return try {
        val out = SimpleDateFormat("d MMM", Locale.getDefault())
        out.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)!!)
    } catch (_: Exception) { dateStr }
}
