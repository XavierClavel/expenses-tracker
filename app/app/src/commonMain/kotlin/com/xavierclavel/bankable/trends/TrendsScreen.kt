package com.xavierclavel.bankable.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.platformFormatMonthYear
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.label_category
import com.xavierclavel.bankable.resources.label_expenses
import com.xavierclavel.bankable.resources.label_flow
import com.xavierclavel.bankable.resources.label_in_out
import com.xavierclavel.bankable.resources.label_income
import com.xavierclavel.bankable.resources.label_mean
import com.xavierclavel.bankable.resources.label_median
import com.xavierclavel.bankable.resources.label_month
import com.xavierclavel.bankable.resources.label_no_data
import com.xavierclavel.bankable.resources.label_total
import com.xavierclavel.bankable.resources.label_year
import com.xavierclavel.bankable.resources.nav_trends
import com.xavierclavel.bankable.resources.trends_above_mean
import com.xavierclavel.bankable.resources.trends_below_mean
import com.xavierclavel.bankable.resources.trends_select_category_hint
import com.xavierclavel.bankable.resources.trends_vs_mean
import com.xavierclavel.bankable.ui.ChartAxisLabel
import com.xavierclavel.bankable.ui.ChartAxisLabelHeight
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.PillChip
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.SlidingToggle
import com.xavierclavel.bankable.ui.StatTile
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.ChartNeutral
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightCardRaised
import com.xavierclavel.bankable.ui.theme.NightDivider
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.accounts.initialBarIndex
import com.xavierclavel.bankable.accounts.isCurrentPeriod
import com.xavierclavel.bankable.util.formatFixed
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import com.xavierclavel.bankable.resources.trends_all_time
import com.xavierclavel.bankable.resources.trends_vs_mean_short
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.RowScope
import org.jetbrains.compose.resources.stringResource

private val COLOR_INCOME  = Mint
private val COLOR_EXPENSE = ChartNeutral

// One column of the trends chart: one bar per series, plus the value the stats
// and the detail card are computed from.
private data class TrendColumn(
    val axisLabel: String,
    val title: String,
    val bars: List<Pair<Float, Color>>,
    val value: Float,
    // Calendar year of a monthly column; null for yearly columns.
    val year: Int? = null,
    val isCurrent: Boolean = false,
)

// A series the stats and the detail card report on. In / out has two (income and
// expenses); the other modes have one.
private data class TrendSeries(
    val name: String,
    val color: Color,
    val values: List<Float>,
    // For spending, being under the mean is the good outcome; for income and net
    // flow it's the other way round.
    val higherIsGood: Boolean,
    // The values the mean and median are taken over: the selected column's year
    // for monthly series, everything for yearly ones.
    val statValues: List<Float> = values,
) {
    val total = values.sum()
    val mean = if (statValues.isEmpty()) 0f else statValues.sum() / statValues.size
    val median = statValues.sorted().let { s ->
        when {
            s.isEmpty()     -> 0f
            s.size % 2 == 1 -> s[s.size / 2]
            else            -> (s[s.size / 2 - 1] + s[s.size / 2]) / 2f
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel,
    categoriesViewModel: CategoriesViewModel,
) {
    val categories by categoriesViewModel.categories.collectAsState()
    val locale = LocalAppLocale.current
    LaunchedEffect(locale)     { viewModel.updateLocale(locale) }
    LaunchedEffect(categories) { viewModel.updateCategories(categories) }

    val bars   by viewModel.bars.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isLoading = viewModel.isLoading
    val isMonth   = viewModel.timescale == "month"

    var showCategoryPicker by remember { mutableStateOf(false) }

    val accentColor = colorHexByName(viewModel.selectedSubcategory?.color ?: viewModel.selectedCategory?.color)
    val columns = remember(groups, bars, viewModel.dataMode, isMonth, locale, accentColor) {
        when (viewModel.dataMode) {
            "income_expense" -> groups.map { g ->
                TrendColumn(
                    axisLabel = g.label,
                    title     = if (g.year != null && g.month != null)
                        platformFormatMonthYear(g.year, g.month, locale).replaceFirstChar { it.titlecase() }
                    else g.label,
                    bars      = listOf(g.income to COLOR_INCOME, g.expense to COLOR_EXPENSE),
                    value     = g.expense,
                    year      = g.year.takeIf { g.month != null },
                    isCurrent = g.year != null && isCurrentPeriod(g.year, g.month),
                )
            }
            "flow" -> bars.map { b ->
                TrendColumn(b.label, b.label, listOf(b.value to (if (b.value >= 0f) Mint else Danger)), b.value, b.year, b.isCurrent)
            }
            else -> bars.map { b -> TrendColumn(b.label, b.label, listOf(b.value to accentColor), b.value, b.year, b.isCurrent) }
        }
    }
    val initialIndex = remember(columns) { initialBarIndex(columns.size) { columns[it].isCurrent } }
    var selectedIndex by remember(columns) { mutableIntStateOf(initialIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = stringResource(Res.string.nav_trends)) {
            SlidingToggle(
                options  = listOf("month" to stringResource(Res.string.label_month), "year" to stringResource(Res.string.label_year)),
                selected = viewModel.timescale,
                onSelect = { viewModel.setTimescale(it) },
                height   = 46.dp,
                modifier = Modifier.width(160.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Data mode chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf("income_expense" to stringResource(Res.string.label_in_out), "flow" to stringResource(Res.string.label_flow), "category" to stringResource(Res.string.label_category)).forEach { (v, l) ->
                    PillChip(text = l, selected = viewModel.dataMode == v, onClick = { viewModel.setDataMode(v) })
                }
            }

            // Aggregation chips (year mode only)
            if (!isMonth) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf("total" to stringResource(Res.string.label_total), "average" to stringResource(Res.string.label_mean), "median" to stringResource(Res.string.label_median)).forEach { (v, l) ->
                        PillChip(text = l, selected = viewModel.aggregation == v, onClick = { viewModel.setAggregation(v) })
                    }
                }
            }

            // Category selector (category mode only)
            if (viewModel.dataMode == "category") {
                CategorySelectorRow(
                    selectedCategory    = viewModel.selectedCategory,
                    selectedSubcategory = viewModel.selectedSubcategory,
                    onClick             = { showCategoryPicker = true },
                )
            }

            // ── Chart card ─────────────────────────────────────────────────
            NightCard(
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp),
            ) {
                if (viewModel.dataMode == "income_expense") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LegendDot(COLOR_INCOME, stringResource(Res.string.label_income))
                        Spacer(Modifier.width(14.dp))
                        LegendDot(COLOR_EXPENSE, stringResource(Res.string.label_expenses))
                    }
                    Spacer(Modifier.height(16.dp))
                }

                val chartHeight = 210.dp
                val placeholder: (@Composable () -> Unit)? = when {
                    isLoading -> { { CircularProgressIndicator(color = Mint) } }
                    viewModel.dataMode == "category" && viewModel.selectedCategory == null && viewModel.selectedSubcategory == null ->
                        { { Text(stringResource(Res.string.trends_select_category_hint), color = NightTextSecondary) } }
                    columns.isEmpty() -> { { EmptyHint() } }
                    else -> null
                }
                if (placeholder != null) {
                    Box(Modifier.fillMaxWidth().height(chartHeight + 28.dp), contentAlignment = Alignment.Center) {
                        placeholder()
                    }
                } else {
                    TrendColumnsChart(
                        columns       = columns,
                        selectedIndex = selectedIndex,
                        initialIndex  = initialIndex,
                        onSelect      = { selectedIndex = it },
                        chartHeight   = chartHeight,
                    )
                }
            }

            val selectedType = viewModel.selectedCategory?.type
                ?: categories.firstOrNull { c -> c.subcategories.any { it.id == viewModel.selectedSubcategory?.id } }?.type
            val statYear = columns.getOrNull(selectedIndex)?.year
            val statIndices = if (statYear == null) columns.indices.toList()
                              else columns.indices.filter { columns[it].year == statYear }
            val series = when (viewModel.dataMode) {
                "income_expense" -> listOf(
                    TrendSeries(stringResource(Res.string.label_income), COLOR_INCOME, groups.map { it.income }, higherIsGood = true),
                    TrendSeries(stringResource(Res.string.label_expenses), COLOR_EXPENSE, groups.map { it.expense }, higherIsGood = false),
                )
                "flow" -> listOf(
                    TrendSeries(stringResource(Res.string.label_flow), Mint, columns.map { it.value }, higherIsGood = true),
                )
                else -> listOf(
                    TrendSeries(
                        name         = viewModel.selectedSubcategory?.name ?: viewModel.selectedCategory?.name ?: "",
                        color        = accentColor,
                        values       = columns.map { it.value },
                        higherIsGood = selectedType == "INCOME",
                    ),
                )
            }.map { s -> s.copy(statValues = statIndices.mapNotNull { s.values.getOrNull(it) }) }
            if (!isLoading && columns.isNotEmpty()) {
                // Summing yearly means/medians isn't meaningful, so the tiles are
                // only shown for the monthly series.
                if (isMonth) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val statPeriod = statYear?.toString() ?: stringResource(Res.string.trends_all_time)
                        SeriesStatTile(stringResource(Res.string.label_total), stringResource(Res.string.trends_all_time), series) { it.total }
                        SeriesStatTile(stringResource(Res.string.label_mean), statPeriod, series) { it.mean }
                        SeriesStatTile(stringResource(Res.string.label_median), statPeriod, series) { it.median }
                    }
                }

                columns.getOrNull(selectedIndex)?.let { column ->
                    PeriodDetailCard(
                        title         = column.title,
                        series        = series,
                        selectedIndex = selectedIndex,
                        showMean      = statIndices.size > 1,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Category picker modal ──────────────────────────────────────────────────
    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = NightCard,
        ) {
            CategoryPickerSheet(
                categories = categories,
                onSelectCategory    = { cat -> viewModel.selectCategory(cat);    showCategoryPicker = false },
                onSelectSubcategory = { sub -> viewModel.selectSubcategory(sub); showCategoryPicker = false },
            )
        }
    }
}

// ── Chart legend / detail card ─────────────────────────────────────────────────

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = NightTextSecondary)
    }
}

// A stat tile with one value per series, coloured like the chart when there are
// several. [period] says what the stat covers ("2025", "all time").
@Composable
private fun RowScope.SeriesStatTile(label: String, period: String, series: List<TrendSeries>, stat: (TrendSeries) -> Float) {
    NightCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = NightTextSecondary, maxLines = 1)
        Text(period, style = MaterialTheme.typography.bodySmall, color = NightTextTertiary, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        series.forEach { s ->
            Text(
                "${fmtVal(stat(s))} €",
                style      = if (series.size == 1) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = if (series.size == 1) NightTextPrimary else s.color,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PeriodDetailCard(
    title: String,
    series: List<TrendSeries>,
    selectedIndex: Int,
    showMean: Boolean,
) {
    NightCard(
        modifier       = Modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        val single = series.singleOrNull()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style    = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color    = NightTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showMean) {
                Text(
                    if (single != null) stringResource(Res.string.trends_vs_mean, single.name.lowercase())
                    else stringResource(Res.string.trends_vs_mean_short),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NightTextSecondary,
                    maxLines = 1,
                )
            }
        }
        if (single != null) {
            val value = single.values.getOrNull(selectedIndex) ?: 0f
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${fmtVal(value)} €",
                    style    = MaterialTheme.typography.displaySmall,
                    color    = NightTextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (showMean) MeanDelta(value, single.mean, single.higherIsGood)
            }
        } else {
            series.forEach { s ->
                val value = s.values.getOrNull(selectedIndex) ?: 0f
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(s.color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        s.name,
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = NightTextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${fmtVal(value)} €",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color      = NightTextPrimary,
                        maxLines   = 1,
                    )
                }
                if (showMean) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        MeanDelta(value, s.mean, s.higherIsGood, MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// "−12 % below mean", in mint when the move is the good direction for the series.
@Composable
private fun MeanDelta(
    value: Float,
    mean: Float,
    higherIsGood: Boolean,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    if (mean == 0f) return
    val pct = ((value - mean) / abs(mean) * 100f).roundToInt()
    val pctText = if (pct < 0) "−${-pct} %" else "+$pct %"
    val color = when {
        pct == 0 -> NightTextSecondary
        (pct > 0) == higherIsGood -> Mint
        else -> Danger
    }
    Text(
        stringResource(if (pct < 0) Res.string.trends_below_mean else Res.string.trends_above_mean, pctText),
        style      = style,
        fontWeight = FontWeight.Bold,
        color      = color,
        maxLines   = 1,
    )
}

// ── Category selector row ──────────────────────────────────────────────────────

@Composable
private fun CategorySelectorRow(
    selectedCategory: CategoryOut?,
    selectedSubcategory: SubcategoryOut?,
    onClick: () -> Unit,
) {
    val icon     = selectedSubcategory?.icon  ?: selectedCategory?.icon
    val name     = selectedSubcategory?.name  ?: selectedCategory?.name
    val colorHex = selectedSubcategory?.color ?: selectedCategory?.color

    NightCard(
        modifier       = Modifier.fillMaxWidth(),
        onClick        = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (name != null) {
                Icon(
                    imageVector = iconByName(icon),
                    contentDescription = null,
                    tint = colorHexByName(colorHex),
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text     = name,
                    style    = MaterialTheme.typography.bodyLarge,
                    color    = NightTextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            } else {
                Text(
                    text     = stringResource(Res.string.trends_select_category_hint),
                    style    = MaterialTheme.typography.bodyLarge,
                    color    = NightTextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NightTextSecondary,
            )
        }
    }
}

// ── Category picker sheet ──────────────────────────────────────────────────────

@Composable
private fun CategoryPickerSheet(
    categories: List<CategoryOut>,
    onSelectCategory: (CategoryOut) -> Unit,
    onSelectSubcategory: (SubcategoryOut) -> Unit,
) {
    var selectedType       by remember { mutableStateOf("EXPENSE") }
    var expandedCategories by remember { mutableStateOf(setOf<Int>()) }

    val filtered = categories.filter { it.type == selectedType }

    Column(modifier = Modifier.fillMaxWidth()) {
        SlidingToggle(
            options  = listOf("EXPENSE" to stringResource(Res.string.label_expenses), "INCOME" to stringResource(Res.string.label_income)),
            selected = selectedType,
            onSelect = { selectedType = it; expandedCategories = emptySet() },
            height   = 48.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 12.dp),
        )

        LazyColumn(
            modifier       = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ScreenGutter, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            filtered.forEach { cat ->
                val isExpanded = expandedCategories.contains(cat.id)
                val nonDefault = cat.subcategories.filter { !it.isDefault }

                item(key = "cat_${cat.id}") {
                    CategoryPickerRow(
                        category         = cat,
                        isExpanded       = isExpanded,
                        hasSubcategories = nonDefault.isNotEmpty(),
                        onSelect         = { onSelectCategory(cat) },
                        onToggleExpand   = {
                            expandedCategories =
                                if (isExpanded) expandedCategories - cat.id
                                else            expandedCategories + cat.id
                        },
                    )
                }

                if (isExpanded) {
                    nonDefault.forEach { sub ->
                        item(key = "sub_${sub.id}") {
                            SubcategoryPickerRow(
                                subcategory = sub,
                                onSelect    = { onSelectSubcategory(sub) },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Category / subcategory picker rows ────────────────────────────────────────

@Composable
private fun CategoryPickerRow(
    category: CategoryOut,
    isExpanded: Boolean,
    hasSubcategories: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
) {
    NightCard(
        modifier       = Modifier.fillMaxWidth(),
        onClick        = onSelect,
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(iconByName(category.icon), null, tint = colorHexByName(category.color), modifier = Modifier.size(24.dp))
            Text(
                category.name,
                style    = MaterialTheme.typography.bodyLarge,
                color    = NightTextPrimary,
                modifier = Modifier.weight(1f),
            )
            // Expand arrow: only toggles subcategory visibility
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(if (hasSubcategories) Modifier.clickable(onClick = onToggleExpand) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (hasSubcategories) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NightTextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubcategoryPickerRow(
    subcategory: SubcategoryOut,
    onSelect: () -> Unit,
) {
    NightCard(
        modifier       = Modifier.fillMaxWidth().padding(start = 20.dp),
        onClick        = onSelect,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(iconByName(subcategory.icon), null, tint = colorHexByName(subcategory.color), modifier = Modifier.size(20.dp))
            Text(
                subcategory.name,
                style    = MaterialTheme.typography.bodyMedium,
                color    = NightTextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Misc ───────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyHint() {
    Text(stringResource(Res.string.label_no_data), color = NightTextSecondary)
}

// ── Column chart ───────────────────────────────────────────────────────────────

// Scrolls horizontally and snaps a column to the centre, like the account charts.
// The centred column is the selected one; tapping a column scrolls it there.
@Composable
private fun TrendColumnsChart(
    columns: List<TrendColumn>,
    selectedIndex: Int,
    initialIndex: Int,
    onSelect: (Int) -> Unit,
    chartHeight: Dp,
) {
    if (columns.isEmpty()) return

    val allValues = columns.flatMap { c -> c.bars.map { it.first } }
    val rawMax    = allValues.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    val rawMin    = allValues.minOrNull()?.coerceAtMost(0f) ?: 0f
    val ticks     = remember(rawMin, rawMax) { rangeTicks(rawMin, if (rawMax == rawMin) rawMin + 1f else rawMax) }
    val top       = maxOf(rawMax, ticks.last().first).coerceAtLeast(rawMin + 0.001f)
    val bottom    = minOf(rawMin, ticks.first().first)
    val labelAreaH = ChartAxisLabelHeight
    val axisWidth  = 26.dp

    val colWidth   = 44.dp
    val colWidthPx = with(LocalDensity.current) { colWidth.roundToPx() }

    val listState    = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val scope        = rememberCoroutineScope()
    // With the centring content padding, the first visible item sits in the centre.
    val centeredIndex by remember(colWidthPx, columns.size) {
        derivedStateOf {
            (listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset.toFloat() / colWidthPx)
                .roundToInt()
                .coerceIn(0, columns.lastIndex)
        }
    }
    LaunchedEffect(columns.size) {
        listState.scrollToItem(initialIndex)
    }
    LaunchedEffect(centeredIndex) { onSelect(centeredIndex) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(chartHeight + labelAreaH)) {
        val plotWidth = maxWidth - axisWidth
        val hPad      = ((plotWidth - colWidth) / 2).coerceAtLeast(0.dp)

        // Grid lines across the whole card, tick labels sitting on them at the right
        Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            val h = size.height
            ticks.forEach { (value, _) ->
                val y = h * (top - value) / (top - bottom)
                if (y < 0f || y > h) return@forEach
                drawLine(
                    color = if (value == 0f) NightTextTertiary.copy(alpha = 0.5f) else NightDivider,
                    start = Offset(0f, y), end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).width(axisWidth).height(chartHeight)) {
            ticks.forEach { (value, label) ->
                if (value == 0f) return@forEach
                val fraction = (top - value) / (top - bottom)
                if (fraction !in 0f..1f) return@forEach
                Text(
                    text      = label,
                    modifier  = Modifier.offset(y = chartHeight * fraction - 16.dp).width(axisWidth),
                    fontSize  = 11.sp,
                    color     = NightTextSecondary,
                    textAlign = TextAlign.End,
                    maxLines  = 1,
                )
            }
        }

        LazyRow(
            state          = listState,
            flingBehavior  = snapBehavior,
            contentPadding = PaddingValues(horizontal = hPad),
            modifier       = Modifier.width(plotWidth).height(chartHeight + labelAreaH),
        ) {
            itemsIndexed(columns) { index, column ->
                val selected = index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(colWidth)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                        ) { scope.launch { listState.animateScrollToItem(index) } },
                ) {
                    Canvas(modifier = Modifier.width(colWidth).height(chartHeight)) {
                        val w = size.width
                        val h = size.height
                        if (selected) {
                            drawRoundRect(
                                color        = NightCardRaised.copy(alpha = 0.55f),
                                topLeft      = Offset(w * 0.04f, 0f),
                                size         = Size(w * 0.92f, h),
                                cornerRadius = CornerRadius(8.dp.toPx()),
                            )
                        }
                        val n       = column.bars.size
                        val barW    = if (n == 1) w * 0.46f else w * 0.3f
                        val gap     = w * 0.07f
                        val groupW  = n * barW + (n - 1) * gap
                        val zeroY   = h * top / (top - bottom)
                        column.bars.forEachIndexed { i, (value, color) ->
                            val x = (w - groupW) / 2f + i * (barW + gap)
                            val y = h * (top - value) / (top - bottom)
                            drawColumnBar(x, barW, zeroY, y, color)
                        }
                    }
                    ChartAxisLabel(text = column.axisLabel, highlighted = selected)
                }
            }
        }
    }
}

// A bar from the zero line to [valueY], rounded on the end away from zero.
private fun DrawScope.drawColumnBar(x: Float, width: Float, zeroY: Float, valueY: Float, color: Color) {
    val topY    = minOf(zeroY, valueY)
    val bottomY = maxOf(zeroY, valueY)
    if (bottomY - topY < 0.5f) return
    val r = CornerRadius(minOf(width / 2f, 4.dp.toPx(), bottomY - topY))
    val positive = valueY <= zeroY
    val rect = RoundRect(
        left              = x,
        top               = topY,
        right             = x + width,
        bottom            = bottomY,
        topLeftCornerRadius     = if (positive) r else CornerRadius.Zero,
        topRightCornerRadius    = if (positive) r else CornerRadius.Zero,
        bottomRightCornerRadius = if (positive) CornerRadius.Zero else r,
        bottomLeftCornerRadius  = if (positive) CornerRadius.Zero else r,
    )
    drawPath(Path().apply { addRoundRect(rect) }, color)
}

// ── Tick helpers ───────────────────────────────────────────────────────────────

private fun rangeTicks(min: Float, max: Float): List<Pair<Float, String>> {
    val step  = niceStep((max - min) / 4f)
    val start = floor(min / step) * step
    val end   = ceil(max / step) * step
    val result = mutableListOf<Pair<Float, String>>()
    var tick = start; var guard = 0
    while (tick <= end + step * 0.01f && guard++ < 12) {
        // Snap float drift so the zero line is exactly 0
        val v = if (abs(tick) < step * 0.001f) 0f else tick
        result += v to fmtTick(v)
        tick += step
    }
    return result
}

private fun niceStep(raw: Float): Float {
    if (raw <= 0f) return 1f
    val exp = floor(log10(raw.toDouble())).toInt()
    val mag = 10.0.pow(exp).toFloat()
    return mag * when (raw / mag) { in 0f..1.5f -> 1f; in 1.5f..3.5f -> 2f; in 3.5f..7.5f -> 5f; else -> 10f }
}

private fun fmtTick(v: Float): String = when {
    abs(v) >= 1_000_000 -> "${(v / 1_000_000).toInt()}M"
    abs(v) >= 10_000    -> "${(v / 1_000).toInt()}k"
    abs(v) >= 1_000     -> if (v % 1_000f == 0f) "${(v / 1_000).toInt()}k" else "${formatFixed((v / 1_000).toDouble(), 1)}k"
    v % 1f == 0f        -> v.toInt().toString()
    else                -> formatFixed(v.toDouble(), 1)
}

private fun fmtVal(v: Float): String = formatRoundedAmount(v.toDouble())
