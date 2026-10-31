package com.xavierclavel.bankable.summary

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.xavierclavel.bankable.api.apiListExpenses
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.constants.shadePalette
import com.xavierclavel.bankable.expenses.ExpensesViewModel
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.platformFormatMonthYear
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_next
import com.xavierclavel.bankable.resources.action_previous
import com.xavierclavel.bankable.resources.label_deficit
import com.xavierclavel.bankable.resources.label_expenses
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.label_month
import com.xavierclavel.bankable.resources.label_saved
import com.xavierclavel.bankable.resources.label_total
import com.xavierclavel.bankable.resources.label_year
import com.xavierclavel.bankable.resources.nav_summary
import com.xavierclavel.bankable.resources.summary_no_data
import com.xavierclavel.bankable.resources.summary_no_expenses
import com.xavierclavel.bankable.resources.summary_outlier_mean
import com.xavierclavel.bankable.resources.summary_outliers_caption
import com.xavierclavel.bankable.resources.summary_outliers_title
import com.xavierclavel.bankable.resources.summary_pct_of_income
import com.xavierclavel.bankable.resources.summary_pct_of_spending
import com.xavierclavel.bankable.resources.summary_saved_income
import com.xavierclavel.bankable.ui.CardDivider
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.ShareBar
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.amountColor
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightCardRaised
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.ui.theme.NightTrack
import com.xavierclavel.bankable.util.formatFixed
import com.xavierclavel.bankable.util.formatIsoDateShort
import com.xavierclavel.bankable.util.lastDayOfMonth
import com.xavierclavel.bankable.util.padZeros
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt
import org.jetbrains.compose.resources.stringResource

private fun localizedMonthYear(year: Int, month: Int, locale: String): String =
    // Capitalize the first letter for title styling — French renders months
    // lowercase (e.g. "juin 2026"), which looks off as a standalone heading.
    platformFormatMonthYear(year, month, locale).replaceFirstChar { it.titlecase() }

// Neutral color for the "unspent income" remainder, so it doesn't read as a
// (possibly green) spending category.
private val ResidualColor = NightTextTertiary

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

data class OutlierEntry(
    val subcategory: SubcategoryEntry,
    // What the subcategory totals over the same months at its usual pace.
    val mean: Float,
    val deviation: Double,
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

    val outlierEntries = remember(categories, viewModel.outliers, viewModel.selectedType) {
        viewModel.outliers
            .filter { it.type == viewModel.selectedType }
            .map { outlier ->
                val subOut = subcategoryMap[outlier.subcategoryId]
                OutlierEntry(
                    subcategory = SubcategoryEntry(
                        subcategoryId = outlier.subcategoryId,
                        value         = outlier.total.toFloatOrNull() ?: 0f,
                        label         = subOut?.name ?: outlier.subcategoryName,
                        color         = subcategoryParentMap[outlier.subcategoryId]?.let { colorHexByName(it.color) } ?: ResidualColor,
                        icon          = subOut?.icon,
                    ),
                    mean      = outlier.mean.toFloatOrNull() ?: 0f,
                    deviation = outlier.deviation,
                )
            }
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
            .background(NightBackground)
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
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Title + timescale toggle ───────────────────────────────────────
        ScreenHeader(title = stringResource(Res.string.nav_summary)) {
            SlidingToggle(
                options  = listOf("month" to stringResource(Res.string.label_month), "year" to stringResource(Res.string.label_year)),
                selected = viewModel.timescale,
                onSelect = { viewModel.setTimescale(it) },
                height   = 46.dp,
                modifier = Modifier.width(160.dp),
            )
        }

        Column(modifier = Modifier.padding(horizontal = ScreenGutter)) {
            // ── Date navigation ────────────────────────────────────────────
            val locale = LocalAppLocale.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon               = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(Res.string.action_previous),
                    onClick            = { viewModel.previousPeriod() },
                    enabled            = viewModel.canGoBack(),
                    size               = 52.dp,
                )
                Text(
                    text = if (viewModel.timescale == "month")
                        localizedMonthYear(viewModel.selectedYear, viewModel.selectedMonth, locale)
                    else viewModel.selectedYear.toString(),
                    style     = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color     = NightTextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                    modifier  = Modifier.weight(1f),
                )
                CircleIconButton(
                    icon               = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.action_next),
                    onClick            = { viewModel.nextPeriod() },
                    enabled            = viewModel.canGoForward(),
                    size               = 52.dp,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Type toggle ────────────────────────────────────────────────
            SlidingToggle(
                options  = listOf("EXPENSE" to stringResource(Res.string.label_expenses), "INCOME" to stringResource(Res.string.label_income)),
                selected = viewModel.selectedType,
                onSelect = { viewModel.setSelectedType(it) },
                height   = 56.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(28.dp))

            // ── Animated data section ──────────────────────────────────────
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
                    when {
                        isLoading         -> SummarySkeleton()
                        pieEntries.isEmpty() -> Box(
                            Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(Res.string.summary_no_data), color = NightTextSecondary)
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
                                    label      = stringResource(Res.string.summary_saved_income),
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
                                pieTotal      = pieTotal,
                                balance       = balance,
                                modifier      = Modifier.size(232.dp).align(Alignment.CenterHorizontally),
                            )

                            // Caption stating which basis the percentages use, so the
                            // income-vs-spending denominator is never ambiguous.
                            if (viewModel.selectedType == "EXPENSE") {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(
                                        if (showSavings) Res.string.summary_pct_of_income
                                        else Res.string.summary_pct_of_spending
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NightTextTertiary,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            Spacer(Modifier.height(24.dp))

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

                            if (viewModel.timescale == "year" && outlierEntries.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                OutliersPanel(
                                    entries      = outlierEntries,
                                    window       = viewModel.outlierWindow(),
                                    selectedType = viewModel.selectedType,
                                    onEntryClick = { sub -> bottomSheetEntry = sub },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // ── Expense list bottom sheet ──────────────────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (bottomSheetEntry != null) {
        ModalBottomSheet(
            onDismissRequest = { bottomSheetEntry = null },
            sheetState       = sheetState,
            containerColor   = NightCard,
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
private fun SavingsLegendRow(
    savings: Double,
    total: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val fraction    = if (total > 0f) (savings / total).toFloat().coerceIn(0f, 1f) else 0f
    val stripeBrush = rememberDiagonalStripeBrush(NightTextSecondary)

    NightCard(
        modifier       = Modifier.fillMaxWidth(),
        onClick        = onSelect,
        selected       = isSelected,
        accent         = NightTextSecondary,
        contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.TrendingUp, null, tint = NightTextSecondary, modifier = Modifier.size(24.dp))
            Text(
                text       = stringResource(Res.string.summary_saved_income),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = NightTextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f),
            )
            Text(formatPct(fraction * 100f), style = MaterialTheme.typography.bodyMedium, color = NightTextSecondary)
            Text(
                formatSummaryAmount(savings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = NightTextSecondary,
            )
            // Same footprint as the category rows' expand button.
            Spacer(Modifier.size(30.dp))
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().padding(end = 8.dp).height(6.dp)) {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(color = NightTrack, cornerRadius = r)
            if (fraction > 0f) drawRoundRect(brush = stripeBrush, size = Size(size.width * fraction, size.height), cornerRadius = r)
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
    val bitmap = ImageBitmap(tile, tile)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color
        strokeWidth = 5f
        style = PaintingStyle.Stroke
    }
    // One stripe per tile period (spacing == tile), each drawn across the whole tile
    // and beyond so it gets clipped at the edges rather than capped inside.
    var b = -2f * w
    while (b <= 2f * w) {
        canvas.drawLine(Offset(-w, -w + b), Offset(2f * w, 2f * w + b), paint)
        b += w
    }
    ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
}

// Corner radius of the donut slices: enough to soften them, not to round them off.
private val SliceCorner = 6.dp

/**
 * A ring segment from [startDeg] sweeping [sweepDeg] (clockwise, 0° = 3 o'clock) with
 * its four corners rounded by [corner], shrunk as needed so thin slices stay valid.
 */
private fun roundedRingSlice(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    startDeg: Float,
    sweepDeg: Float,
    corner: Float,
): Path {
    val toRad = PI.toFloat() / 180f
    val a0 = startDeg * toRad
    val a1 = (startDeg + sweepDeg) * toRad
    val r = minOf(corner, (outerRadius - innerRadius) / 2f, (a1 - a0) * innerRadius / 2f).coerceAtLeast(0f)
    val co = r / outerRadius
    val ci = r / innerRadius
    fun pt(radius: Float, angle: Float) = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
    return Path().apply {
        pt(outerRadius, a0 + co).let { moveTo(it.x, it.y) }
        arcTo(Rect(center, outerRadius), (a0 + co) / toRad, (a1 - a0 - 2 * co) / toRad, false)
        pt(outerRadius, a1).let { c -> pt(outerRadius - r, a1).let { quadraticTo(c.x, c.y, it.x, it.y) } }
        pt(innerRadius + r, a1).let { lineTo(it.x, it.y) }
        pt(innerRadius, a1).let { c -> pt(innerRadius, a1 - ci).let { quadraticTo(c.x, c.y, it.x, it.y) } }
        arcTo(Rect(center, innerRadius), (a1 - ci) / toRad, -(a1 - a0 - 2 * ci) / toRad, false)
        pt(innerRadius, a0).let { c -> pt(innerRadius + r, a0).let { quadraticTo(c.x, c.y, it.x, it.y) } }
        pt(outerRadius - r, a0).let { lineTo(it.x, it.y) }
        pt(outerRadius, a0).let { c -> pt(outerRadius, a0 + co).let { quadraticTo(c.x, c.y, it.x, it.y) } }
        close()
    }
}

@Composable
private fun DonutChart(
    entries: List<PieEntry>,
    total: Float,
    selectedIndex: Int,
    onSliceClick: (Int) -> Unit,
    pieTotal: Float,
    balance: Double,
    modifier: Modifier = Modifier,
) {
    val anySelected   = selectedIndex != -1
    val selectedEntry = entries.getOrNull(selectedIndex)
    val stripeBrush   = rememberDiagonalStripeBrush(ResidualColor)
    val ringFraction  = 0.24f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(entries, total) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val dx = tap.x - cx;      val dy = tap.y - cy
                    val dist   = sqrt(dx * dx + dy * dy)
                    val outerR = minOf(size.width, size.height) / 2f
                    val strokeW = outerR * ringFraction
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
            val strokeBase = diameter / 2f * ringFraction
            // Gaps only when there's more than one slice, so a lone category is a full ring.
            val gapDeg     = if (entries.size > 1) 2f else 0f
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
                if (entries.size > 1) {
                    // Slices are ring segments with slightly softened corners.
                    val slice = roundedRingSlice(
                        center      = Offset(size.width / 2f, size.height / 2f),
                        outerRadius = diameter / 2f,
                        innerRadius = diameter / 2f - stroke,
                        startDeg    = start,
                        sweepDeg    = sweep,
                        corner      = SliceCorner.toPx(),
                    )
                    if (entry.isResidual) drawPath(slice, brush = stripeBrush, alpha = alpha)
                    else drawPath(slice, color = entry.color.copy(alpha = alpha))
                } else if (entry.isResidual) {
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
        // Centre label: selected slice → its name, amount and share
        //               otherwise → TOTAL of the shown type + saved/deficit line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(horizontal = 50.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (selectedEntry != null) {
                Text(
                    selectedEntry.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NightTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatSummaryAmount(selectedEntry.value.toDouble()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (selectedEntry.isResidual) NightTextPrimary else selectedEntry.color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatPct(if (total > 0f) selectedEntry.value / total * 100f else 0f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NightTextSecondary,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    stringResource(Res.string.label_total).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = NightTextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatSummaryAmount(pieTotal.toDouble()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = NightTextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                if (balance != 0.0) {
                    Spacer(Modifier.height(2.dp))
                    val sign = if (balance > 0) "+" else "-"
                    val word = stringResource(if (balance > 0) Res.string.label_saved else Res.string.label_deficit)
                    Text(
                        "$sign${formatSummaryAmount(abs(balance))} $word",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balance > 0) Mint else Danger,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
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
    val hasSubs    = entry.subcategoryEntries.isNotEmpty()

    Column {
        NightCard(
            modifier       = Modifier.fillMaxWidth(),
            onClick        = onSelect,
            selected       = isSelected,
            accent         = entryColor,
            contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(iconByName(entry.icon), null, tint = entryColor, modifier = Modifier.size(24.dp))
                Text(
                    text       = entry.label,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                    color      = NightTextPrimary,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(formatPct(pct), style = MaterialTheme.typography.bodyMedium, color = NightTextSecondary)
                Text(
                    formatSummaryAmount(entry.value.toDouble()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = entryColor,
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .then(if (hasSubs) Modifier.clickable(onClick = onToggleExpand) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasSubs) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = NightTextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            ShareBar(
                fraction = fraction,
                color    = entryColor,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        if (isExpanded) {
            entry.subcategoryEntries.forEach { sub ->
                Spacer(Modifier.height(4.dp))
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
    val pct      = if (total > 0f) subEntry.value / total * 100f else 0f
    val fraction = (subEntry.value / total).coerceIn(0f, 1f)
    val color    = subEntry.color

    NightCard(
        modifier       = Modifier.fillMaxWidth().padding(start = 20.dp),
        onClick        = onClick,
        contentPadding = PaddingValues(start = 12.dp, end = 8.dp, top = 7.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(iconByName(subEntry.icon), null, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text     = subEntry.label,
                style    = MaterialTheme.typography.bodyLarge,
                color    = NightTextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatPct(pct), style = MaterialTheme.typography.bodyMedium, color = NightTextSecondary)
            Text(
                formatSummaryAmount(subEntry.value.toDouble()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
            Icon(Icons.Default.ChevronRight, null, tint = NightTextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(5.dp))
        ShareBar(
            fraction = fraction,
            color    = color,
            height   = 4.dp,
            modifier = Modifier.padding(end = 6.dp),
        )
    }
}

// ── Outliers panel ─────────────────────────────────────────────────────────────

// Subcategories whose year runs well above their usual pace. Above average is a
// warning for spending but good news for income, hence the type-dependent accent.
@Composable
private fun OutliersPanel(
    entries: List<OutlierEntry>,
    window: IntRange,
    selectedType: String,
    onEntryClick: (SubcategoryEntry) -> Unit,
) {
    val accent     = if (selectedType == "INCOME") Mint else Danger
    val windowText = if (window.first == window.last) "${window.first}" else "${window.first}–${window.last}"

    NightCard(
        modifier       = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = accent, modifier = Modifier.size(24.dp))
            Text(
                stringResource(Res.string.summary_outliers_title),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = NightTextPrimary,
            )
        }
        Text(
            text     = stringResource(
                Res.string.summary_outliers_caption,
                formatSummaryAmount(OUTLIER_MIN_TOTAL.toDouble()),
                "${formatFixed(OUTLIER_THRESHOLD * 100, 0)}%",
                windowText,
            ),
            style    = MaterialTheme.typography.bodyMedium,
            color    = NightTextSecondary,
            modifier = Modifier.padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
        )
        entries.forEach { entry ->
            OutlierRow(entry = entry, accent = accent, onClick = { onEntryClick(entry.subcategory) })
        }
    }
}

@Composable
private fun OutlierRow(entry: OutlierEntry, accent: Color, onClick: () -> Unit) {
    val sub = entry.subcategory
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(top = 8.dp, bottom = 8.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(iconByName(sub.icon), null, tint = sub.color, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                sub.label,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = NightTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.summary_outlier_mean, formatSummaryAmount(entry.mean.toDouble())),
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextSecondary,
            )
        }
        Text(
            formatSummaryAmount(sub.value.toDouble()),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color      = NightTextPrimary,
            textAlign  = TextAlign.End,
        )
        // Fixed minimum width so the amounts line up whatever the percentage's length.
        Box(
            modifier = Modifier
                .widthIn(min = 56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.14f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "+${formatFixed(entry.deviation * 100, 0)}%",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = accent,
            )
        }
        Icon(Icons.Default.ChevronRight, null, tint = NightTextSecondary, modifier = Modifier.size(20.dp))
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

    Column(modifier = Modifier.padding(horizontal = ScreenGutter).padding(bottom = 32.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(iconByName(entry.icon), null, tint = color, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.label, style = MaterialTheme.typography.titleLarge, color = NightTextPrimary)
                Text(
                    formatSummaryAmount(entry.value.toDouble()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
        CardDivider()
        Spacer(Modifier.height(4.dp))

        when {
            isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint)
            }
            expenses.isEmpty() -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.summary_no_expenses), color = NightTextSecondary)
            }
            else -> expenses.forEach { expense ->
                ExpenseSheetRow(expense, onClick = { onExpenseClick(expense) })
            }
        }
    }
}

@Composable
private fun ExpenseSheetRow(expense: ExpenseOut, onClick: () -> Unit) {
    val locale   = LocalAppLocale.current
    val isIncome = expense.type == "INCOME"
    val sign     = if (isIncome) "+" else "-"
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text     = formatDateShort(expense.date, locale),
            style    = MaterialTheme.typography.bodySmall,
            color    = NightTextSecondary,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text     = expense.title,
            style    = MaterialTheme.typography.bodyMedium,
            color    = NightTextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text       = "$sign${formatAmountDisplay(expense.amount, locale)} ${currencySymbol(expense.currency)}",
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = amountColor(isIncome),
        )
    }
    CardDivider()
}

// ── Skeleton loader ────────────────────────────────────────────────────────────

@Composable
private fun shimmerBrush(): Brush {
    val base       = NightCard
    val highlight  = NightCardRaised
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
        Box(modifier = Modifier.size(232.dp).align(Alignment.CenterHorizontally)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter    = minOf(size.width, size.height)
                val strokeWidth = diameter / 2f * 0.24f
                val inset       = strokeWidth / 2f
                drawArc(
                    brush = brush, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size    = Size(diameter - strokeWidth, diameter - strokeWidth),
                    style   = Stroke(width = strokeWidth),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        repeat(4) {
            Box(Modifier.fillMaxWidth().height(76.dp).clip(MaterialTheme.shapes.large).background(brush))
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(10.dp))
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private fun computeDateRange(year: Int, month: Int, timescale: String): Pair<String, String> {
    return if (timescale == "month") {
        val prefix = "${year.padZeros(4)}-${month.padZeros(2)}"
        "$prefix-01" to "$prefix-${lastDayOfMonth(year, month).padZeros(2)}"
    } else {
        "${year.padZeros(4)}-01-01" to "${year.padZeros(4)}-12-31"
    }
}

private fun formatSummaryAmount(value: Double): String {
    return "${formatRoundedAmount(value)} €"
}

private fun formatPct(pct: Float): String = "${formatFixed(pct.toDouble(), 1)} %"

private fun formatDateShort(dateStr: String, locale: String): String =
    formatIsoDateShort(dateStr, locale)
