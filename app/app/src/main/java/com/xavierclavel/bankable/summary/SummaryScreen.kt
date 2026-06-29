package com.xavierclavel.bankable.summary

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
import androidx.compose.material.icons.filled.TrendingUp
import kotlin.math.abs
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
import androidx.compose.ui.res.stringResource
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.ui.SlidingToggle
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
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.xavierclavel.bankable.api.apiListExpenses
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.shadePalette
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.expenses.ExpensesViewModel
import com.xavierclavel.bankable.model.ExpenseOut
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private fun localizedMonthYear(year: Int, month: Int, locale: Locale): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
    }
    // Capitalize the first letter for title styling — French renders months
    // lowercase (e.g. "juin 2026"), which looks off as a standalone heading.
    return SimpleDateFormat("MMMM yyyy", locale).format(cal.time)
        .replaceFirstChar { it.titlecase(locale) }
}

// Neutral color for the "unspent income" remainder, so it doesn't read as a
// (possibly green) spending category.
private val ResidualColor = Color(0xFF9E9E9E)

data class SubcategoryEntry(
    val subcategoryId: Int,
    val value: Float,
    val label: String,
    val color: Color,
    val icon: String?,
)

data class PieEntry(
    val categoryId: Int,
    val value: Float,
    val label: String,
    val color: Color,
    val icon: String?,
    val subcategoryEntries: List<SubcategoryEntry> = emptyList(),
    // The "unspent income" remainder — drawn distinctly so it doesn't read as a category.
    val isResidual: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    categoriesViewModel: CategoriesViewModel,
    expensesViewModel: ExpensesViewModel,
    navController: NavController,
) {
    // Refresh summary every time this screen is resumed (e.g. returning from expense edit)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refresh()
        }
    }

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
                    val rawSubs = catSubs.mapNotNull { sub ->
                        val subOut = subcategoryMap[sub.categoryId] ?: return@mapNotNull null
                        Triple(sub.categoryId, sub.total.toFloatOrNull() ?: 0f, subOut)
                    }.filter { it.second > 0f }.sortedByDescending { it.second }
                    // Color-code subcategories as shades of the parent category's color,
                    // spread light→dark by their rank within the category.
                    val shades = shadePalette(colorHexByName(cat.color), rawSubs.size)
                    val subEntries = rawSubs.mapIndexed { i, (id, value, subOut) ->
                        SubcategoryEntry(
                            subcategoryId = id,
                            value         = value,
                            label         = subOut.name,
                            color         = shades[i],
                            icon          = subOut.icon,
                        )
                    }
                    PieEntry(
                        categoryId         = cat.id,
                        value              = total.toFloat(),
                        label              = cat.name,
                        color              = colorHexByName(cat.color),
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
        val locale = LocalConfiguration.current.locales[0]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { viewModel.previousPeriod() }, enabled = viewModel.canGoBack()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_previous))
            }
            Text(
                text = if (viewModel.timescale == "month")
                    localizedMonthYear(viewModel.selectedYear, viewModel.selectedMonth, locale)
                else viewModel.selectedYear.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { viewModel.nextPeriod() }, enabled = viewModel.canGoForward()) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.action_next))
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Timescale + type toggles ───────────────────────────────────────
        SlidingToggle(
            options  = listOf("month" to stringResource(R.string.label_month), "year" to stringResource(R.string.label_year)),
            selected = viewModel.timescale,
            onSelect = { viewModel.setTimescale(it) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        SlidingToggle(
            options  = listOf("EXPENSE" to stringResource(R.string.label_expenses), "INCOME" to stringResource(R.string.label_income)),
            selected = viewModel.selectedType,
            onSelect = { viewModel.setSelectedType(it) },
            modifier = Modifier.fillMaxWidth(),
        )

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

            val totalExpenses = summary?.totalExpenses?.toDoubleOrNull() ?: 0.0
            val totalIncome   = summary?.totalIncome?.toDoubleOrNull()   ?: 0.0
            val balance       = totalIncome - totalExpenses

            Column(modifier = Modifier.fillMaxWidth()) {
                // Balance summary card (or skeleton)
                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(64.dp).clip(MaterialTheme.shapes.medium).background(shimmerBrush()))
                } else {
                    BalanceSummaryCard(
                        totalExpenses = totalExpenses,
                        totalIncome   = totalIncome,
                    )
                }

                Spacer(Modifier.height(16.dp))

                when {
                    isLoading         -> SummarySkeleton()
                    pieEntries.isEmpty() -> Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.summary_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    else -> {
                        val pieTotal = pieEntries.sumOf { it.value.toDouble() }.toFloat()

                        // In EXPENSE view with a surplus, the donut represents *income*:
                        // expense categories plus an "unspent income" slice fill the ring,
                        // so every percentage is read against income. Otherwise (income
                        // view, or a deficit) percentages are read against the pie total.
                        val showSavings = viewModel.selectedType == "EXPENSE" && balance > 0
                        val savingsValue = if (showSavings) balance.toFloat() else 0f
                        val denominator  = pieTotal + savingsValue
                        val savingsIndex = pieEntries.size

                        val donutEntries = if (showSavings) {
                            pieEntries + PieEntry(
                                categoryId = -1,
                                value      = savingsValue,
                                label      = stringResource(R.string.summary_unspent_income),
                                color      = ResidualColor,
                                icon       = null,
                                isResidual = true,
                            )
                        } else pieEntries

                        DonutChart(
                            entries       = donutEntries,
                            total         = denominator,
                            selectedIndex = selectedSlice,
                            onSliceClick  = { i -> selectedSlice = if (selectedSlice == i) -1 else i },
                            balance       = balance,
                            selectedType  = viewModel.selectedType,
                            modifier      = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
                        )

                        // Caption stating which basis the percentages use, so the
                        // income-vs-spending denominator is never ambiguous.
                        if (viewModel.selectedType == "EXPENSE") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    if (showSavings) R.string.summary_pct_of_income
                                    else R.string.summary_pct_of_spending
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Savings row at the top: only in EXPENSE view when income >
                        // expenses. Selectable in sync with its donut slice.
                        if (showSavings) {
                            SavingsLegendRow(
                                savings    = balance,
                                total      = denominator,
                                isSelected = selectedSlice == savingsIndex,
                                onSelect   = { selectedSlice = if (selectedSlice == savingsIndex) -1 else savingsIndex },
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        pieEntries.forEachIndexed { index, entry ->
                            CategoryLegendRow(
                                entry       = entry,
                                total       = denominator,
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
                onExpenseClick = { expense ->
                    val sub = subcategoryMap[expense.categoryId]
                    expensesViewModel.prepareEditExpense(expense, sub)
                    bottomSheetEntry = null
                    navController.navigate("expense/edit")
                },
            )
        }
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun BalanceSummaryCard(totalExpenses: Double, totalIncome: Double) {
    val balance     = totalIncome - totalExpenses
    val isSaved     = balance >= 0
    val balColor    = if (isSaved) Color(0xFF4CAF50) else Color(0xFFE53935)

    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Expenses (left)
                Column {
                    Text("-${formatSummaryAmount(totalExpenses)}", style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                    Text(stringResource(R.string.label_expenses), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Balance (centre) — prominent
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val sign = if (isSaved) "+" else "-"
                    Text("$sign${formatSummaryAmount(abs(balance))}", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold, color = balColor)
                    Text(if (isSaved) stringResource(R.string.label_saved) else stringResource(R.string.label_deficit), style = MaterialTheme.typography.labelSmall,
                        color = balColor.copy(alpha = 0.8f))
                }
                // Income (right)
                Column(horizontalAlignment = Alignment.End) {
                    Text("+${formatSummaryAmount(totalIncome)}", style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text(stringResource(R.string.label_income), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SavingsLegendRow(
    savings: Double,
    total: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val fraction    = if (total > 0f) (savings / total).toFloat().coerceIn(0f, 1f) else 0f
    val color       = ResidualColor
    val trackColor  = MaterialTheme.colorScheme.outlineVariant
    val stripeBrush = rememberDiagonalStripeBrush(color)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape    = MaterialTheme.shapes.medium,
        color    = color.copy(alpha = if (isSelected) 0.18f else 0.08f),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.TrendingUp, null, tint = color, modifier = Modifier.size(22.dp))
                Text(
                    text       = stringResource(R.string.summary_unspent_income),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                    color      = color,
                )
                Text("%.1f%%".format(fraction * 100), style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.7f), modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                Text(formatSummaryAmount(savings), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.width(76.dp), textAlign = TextAlign.End)
                Spacer(Modifier.width(32.dp))
            }
            Canvas(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp).height(4.dp)) {
                val r = CornerRadius(2.dp.toPx())
                drawRoundRect(color = trackColor, cornerRadius = r)
                if (fraction > 0f) drawRoundRect(brush = stripeBrush, size = Size(size.width * fraction, size.height), cornerRadius = r)
            }
        }
    }
}

// A repeating 45° diagonal-stripe brush in [color]. Lines are drawn longer than the
// tile (endpoints off-bitmap) so no stroke caps fall inside it — that's what keeps the
// stripes continuous across tile boundaries instead of showing regular notches.
@Composable
private fun rememberDiagonalStripeBrush(color: Color): ShaderBrush = remember(color) {
    val tile = 14
    val w = tile.toFloat()
    val bitmap = android.graphics.Bitmap.createBitmap(tile, tile, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        strokeWidth = 5f
        style = android.graphics.Paint.Style.STROKE
    }
    // One stripe per tile period (spacing == tile), each drawn across the whole tile
    // and beyond so it gets clipped at the edges rather than capped inside.
    var b = -2f * w
    while (b <= 2f * w) {
        canvas.drawLine(-w, -w + b, 2f * w, 2f * w + b, paint)
        b += w
    }
    ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

@Composable
private fun DonutChart(
    entries: List<PieEntry>,
    total: Float,
    selectedIndex: Int,
    onSliceClick: (Int) -> Unit,
    balance: Double = 0.0,
    selectedType: String = "EXPENSE",
    modifier: Modifier = Modifier,
) {
    val anySelected  = selectedIndex != -1
    val selectedEntry = entries.getOrNull(selectedIndex)
    val stripeBrush  = rememberDiagonalStripeBrush(ResidualColor)

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
                // Residual (unspent income) is full width like the others but filled
                // with a diagonal-line hatch, so it reads as "leftover", not a category.
                val stroke  = if (isSelected) strokeBase * 1.15f else strokeBase
                val inset   = stroke / 2f
                val topLeft = Offset((size.width - diameter) / 2f + inset, (size.height - diameter) / 2f + inset)
                val arcSize = Size(diameter - stroke, diameter - stroke)
                val start   = startAngle + gapDeg / 2f
                val sweep   = sweepAngle.coerceAtLeast(0.1f)
                if (entry.isResidual) {
                    drawArc(
                        brush      = stripeBrush,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        alpha      = alpha,
                        style      = Stroke(width = stroke),
                    )
                } else {
                    drawArc(
                        color      = entry.color.copy(alpha = alpha),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(width = stroke),
                    )
                }
                startAngle += 360f * fraction
            }
        }
        // Centre label: selected slice → slice value/name
        //               expense mode, no selection → balance (saved/deficit)
        //               income mode, no selection → total income
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
            when {
                selectedEntry != null -> {
                    Text(formatSummaryAmount(selectedEntry.value.toDouble()),
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, color = selectedEntry.color)
                    Text(selectedEntry.label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                selectedType == "EXPENSE" && balance != 0.0 -> {
                    val balColor = if (balance > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                    val sign     = if (balance > 0) "+" else "-"
                    Text("$sign${formatSummaryAmount(abs(balance))}",
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, color = balColor)
                    Text(if (balance > 0) stringResource(R.string.label_saved) else stringResource(R.string.label_deficit),
                        style = MaterialTheme.typography.labelSmall,
                        color = balColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                }
                else -> {
                    Text(formatSummaryAmount(total.toDouble()),
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.label_total), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
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
    val entryColor = entry.color
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
                    subEntry = sub,
                    total    = total,
                    onClick  = { onSubcategoryClick(sub) },
                )
            }
        }
    }
}

@Composable
private fun SubcategoryLegendRow(
    subEntry: SubcategoryEntry,
    total: Float,
    onClick: () -> Unit,
) {
    val pct        = if (total > 0f) subEntry.value / total * 100f else 0f
    val fraction   = (subEntry.value / total).coerceIn(0f, 1f)
    val color      = subEntry.color
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
    onExpenseClick: (ExpenseOut) -> Unit,
) {
    var expenses  by remember { mutableStateOf<List<ExpenseOut>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val color = entry.color

    LaunchedEffect(entry.subcategoryId) {
        isLoading = true
        try {
            expenses = apiListExpenses(0, 100, subcategoryId = entry.subcategoryId, from = dateFrom, to = dateTo)
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
                Text(stringResource(R.string.summary_no_expenses), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> expenses.forEach { expense ->
                ExpenseSheetRow(expense, onClick = { onExpenseClick(expense) })
            }
        }
    }
}

@Composable
private fun ExpenseSheetRow(expense: ExpenseOut, onClick: () -> Unit) {
    val locale      = LocalConfiguration.current.locales[0]
    val amountColor = if (expense.type == "INCOME") Color(0xFF4CAF50) else Color(0xFFE53935)
    val sign        = if (expense.type == "INCOME") "+" else "-"
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text     = formatDateShort(expense.date, locale),
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
            text       = "$sign${formatAmountDisplay(expense.amount, locale)} ${currencySymbol(expense.currency)}",
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
    return "${formatRoundedAmount(value)} €"
}

private fun formatDateShort(dateStr: String, locale: Locale): String {
    return try {
        val out = SimpleDateFormat("d MMM", locale)
        out.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)!!)
    } catch (_: Exception) { dateStr }
}
