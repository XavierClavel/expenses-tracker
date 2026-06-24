package com.xavierclavel.bankable.trends

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.accounts.BarChart
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.constants.colorHexByName
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.constants.iconByName
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.SubcategoryOut
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val COLOR_INCOME  = Color(0xFF4CAF50)
private val COLOR_EXPENSE = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel,
    categoriesViewModel: CategoriesViewModel,
) {
    val categories by categoriesViewModel.categories.collectAsState()
    val locale = LocalConfiguration.current.locales[0]
    LaunchedEffect(locale)     { viewModel.updateLocale(locale) }
    LaunchedEffect(categories) { viewModel.updateCategories(categories) }

    val bars   by viewModel.bars.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isLoading = viewModel.isLoading

    var showCategoryPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Chart — fills all available space dynamically ───────────────────
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            // BarChart / GroupedBarChart total height = 36dp (header) + chartH + 28dp (labels)
            // Reserve those 64dp + a small buffer so the chart fills the box.
            val chartH = (maxHeight - 80.dp).coerceAtLeast(120.dp)

            when {
                isLoading -> CircularProgressIndicator()
                viewModel.dataMode == "income_expense" ->
                    if (groups.isEmpty()) EmptyHint()
                    else GroupedBarChart(groups = groups, chartHeight = chartH)
                viewModel.dataMode == "category" && viewModel.selectedCategory == null && viewModel.selectedSubcategory == null ->
                    Text(stringResource(R.string.trends_select_category_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                bars.isEmpty() -> EmptyHint()
                else -> {
                    val accentColor = if (viewModel.dataMode == "category")
                        colorHexByName(viewModel.selectedSubcategory?.color ?: viewModel.selectedCategory?.color)
                    else null
                    BarChart(bars = bars, chartHeight = chartH, accentColor = accentColor)
                }
            }
        }

        // ── Controls — no elevation background, just a divider ─────────────
        HorizontalDivider()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Timescale chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("month" to stringResource(R.string.label_month), "year" to stringResource(R.string.label_year)).forEach { (v, l) ->
                    FilterChip(selected = viewModel.timescale == v, onClick = { viewModel.setTimescale(v) }, label = { Text(l) })
                }
            }
            // Data mode chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("income_expense" to stringResource(R.string.label_in_out), "flow" to stringResource(R.string.label_flow), "category" to stringResource(R.string.label_category)).forEach { (v, l) ->
                    FilterChip(selected = viewModel.dataMode == v, onClick = { viewModel.setDataMode(v) }, label = { Text(l) })
                }
            }

            // Aggregation chips (year mode only)
            if (viewModel.timescale == "year") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("total" to stringResource(R.string.label_total), "average" to stringResource(R.string.label_mean), "median" to stringResource(R.string.label_median)).forEach { (v, l) ->
                        FilterChip(selected = viewModel.aggregation == v, onClick = { viewModel.setAggregation(v) }, label = { Text(l) })
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
        }
    }

    // ── Category picker modal ──────────────────────────────────────────────────
    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            CategoryPickerSheet(
                categories = categories,
                onSelectCategory    = { cat -> viewModel.selectCategory(cat);    showCategoryPicker = false },
                onSelectSubcategory = { sub -> viewModel.selectSubcategory(sub); showCategoryPicker = false },
            )
        }
    }
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

    Surface(
        modifier       = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape          = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (name != null) {
                androidx.compose.material3.Icon(
                    imageVector = iconByName(icon),
                    contentDescription = null,
                    tint = colorHexByName(colorHex),
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text     = name,
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            } else {
                Text(
                    text     = stringResource(R.string.trends_select_category_hint),
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
        // Sliding type toggle
        TypeToggle(
            selectedType = selectedType,
            onSelect     = { selectedType = it; expandedCategories = emptySet() },
            modifier     = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )

        HorizontalDivider()

        LazyColumn(
            modifier       = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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

// ── Sliding type toggle ────────────────────────────────────────────────────────

@Composable
private fun TypeToggle(
    selectedType: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("EXPENSE" to stringResource(R.string.label_expenses), "INCOME" to stringResource(R.string.label_income))
    val selectedIndex = options.indexOfFirst { it.first == selectedType }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val pillWidth = maxWidth / 2
        val pillOffset by animateDpAsState(
            targetValue   = pillWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label         = "toggle_pill",
        )

        // Sliding pill
        Box(
            modifier = Modifier
                .width(pillWidth)
                .fillMaxSize()
                .offset(x = pillOffset)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary),
        )

        // Labels
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { i, (value, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()

                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedIndex == i) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (selectedIndex == i) MaterialTheme.colorScheme.onPrimary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Body: tapping selects the category
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelect)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(iconByName(category.icon), null, tint = colorHexByName(category.color), modifier = Modifier.size(22.dp))
                Text(category.name, style = MaterialTheme.typography.bodyLarge)
            }
            // Expand arrow: only toggles subcategory visibility
            if (hasSubcategories) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }
    }
}

@Composable
private fun SubcategoryPickerRow(
    subcategory: SubcategoryOut,
    onSelect: () -> Unit,
) {
    Surface(
        modifier       = Modifier.fillMaxWidth().padding(start = 20.dp).clickable(onClick = onSelect),
        shape          = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(iconByName(subcategory.icon), null, tint = colorHexByName(subcategory.color), modifier = Modifier.size(18.dp))
            Text(subcategory.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

// ── Misc ───────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyHint() {
    Text(stringResource(R.string.label_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ── Grouped bar chart (income_expense mode) ────────────────────────────────────

@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    groupWidth: Dp = 52.dp,
    chartHeight: Dp = 180.dp,
) {
    if (groups.isEmpty()) return

    val maxVal       = groups.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(0.001f)
    val ticks        = remember(maxVal) { simpleTicks(maxVal) }
    val labelAreaH   = 36.dp
    val yAxisWidth   = 22.dp

    val listState    = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val groupWidthPx = with(LocalDensity.current) { groupWidth.roundToPx() }

    val centeredIndex by remember(groupWidthPx, groups.size) {
        derivedStateOf {
            val firstIdx    = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            (firstIdx + firstOffset.toFloat() / groupWidthPx)
                .roundToInt().coerceIn(0, groups.lastIndex)
        }
    }
    val highlighted = groups.getOrNull(centeredIndex)

    LaunchedEffect(groups.size) {
        if (groups.isNotEmpty()) listState.scrollToItem(groups.lastIndex)
    }

    Column {
        // Highlighted value header
        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            if (highlighted != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("+${fmtVal(highlighted.income)} €",  color = if (highlighted.income  == 0f) Color.Gray else COLOR_INCOME,  fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("-${fmtVal(highlighted.expense)} €", color = if (highlighted.expense == 0f) Color.Gray else COLOR_EXPENSE, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        Row {
            // Y-axis
            BoxWithConstraints(modifier = Modifier.width(yAxisWidth).height(chartHeight)) {
                val h = maxHeight
                ticks.forEach { (value, label) ->
                    val fraction = 1f - (value / maxVal)
                    if (fraction !in 0f..1f) return@forEach
                    val yOff = (h * fraction - 6.dp).coerceAtLeast(0.dp)
                    Text(
                        text     = label,
                        modifier = Modifier.offset(y = yOff).width(yAxisWidth - 4.dp),
                        fontSize = 9.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        maxLines  = 1,
                    )
                }
            }

            // Scrollable area
            BoxWithConstraints(modifier = Modifier.weight(1f).height(chartHeight + labelAreaH)) {
                val hPad = ((maxWidth - groupWidth) / 2).coerceAtLeast(0.dp)

                // Background graduation lines
                Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                    val h = size.height
                    ticks.forEach { (value, _) ->
                        val y = h * (1f - value / maxVal)
                        if (y < 0f || y > h) return@forEach
                        drawLine(
                            color = if (value == 0f) Color.Gray.copy(0.45f) else Color.Gray.copy(0.13f),
                            start = Offset(0f, y), end = Offset(size.width, y),
                            strokeWidth = if (value == 0f) 1.dp.toPx() else (0.5f).dp.toPx(),
                        )
                    }
                }

                LazyRow(
                    state          = listState,
                    flingBehavior  = snapBehavior,
                    modifier       = Modifier.height(chartHeight + labelAreaH),
                    contentPadding = PaddingValues(horizontal = hPad),
                ) {
                    itemsIndexed(groups) { index, group ->
                        val hi    = index == centeredIndex
                        val alpha = if (hi) 1f else 0.35f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(groupWidth),
                        ) {
                            Canvas(modifier = Modifier.width(groupWidth).height(chartHeight)) {
                                val r        = CornerRadius(3.dp.toPx())
                                val w        = size.width
                                val h        = size.height
                                val scale    = h / maxVal
                                val subBarW  = w * 0.35f
                                val sideGap  = w * 0.05f
                                val innerGap = w * 0.1f

                                val incomeH  = (group.income  * scale).coerceAtLeast(0f)
                                val expenseH = (group.expense * scale).coerceAtLeast(0f)

                                drawRoundRect(color = COLOR_INCOME.copy(alpha = alpha),
                                    topLeft = Offset(sideGap, h - incomeH), size = Size(subBarW, incomeH), cornerRadius = r)
                                drawRoundRect(color = COLOR_EXPENSE.copy(alpha = alpha),
                                    topLeft = Offset(sideGap + subBarW + innerGap, h - expenseH), size = Size(subBarW, expenseH), cornerRadius = r)
                            }
                            Text(
                                text       = group.label,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = if (hi) MaterialTheme.colorScheme.onBackground else Color.Gray.copy(0.6f),
                                fontWeight = if (hi) FontWeight.Bold else FontWeight.Normal,
                                textAlign  = TextAlign.Center,
                                maxLines   = 2,
                                modifier   = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(yAxisWidth))
        }
    }
}

// ── Tick helpers ───────────────────────────────────────────────────────────────

private fun simpleTicks(max: Float): List<Pair<Float, String>> {
    if (max <= 0f) return listOf(0f to "0")
    val step = niceStep(max / 4f)
    val result = mutableListOf<Pair<Float, String>>()
    var tick = 0f; var guard = 0
    while (tick <= max + step * 0.01f && guard++ < 8) { result += tick to fmtTick(tick); tick += step }
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
    abs(v) >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
    v % 1f == 0f        -> v.toInt().toString()
    else                -> "%.1f".format(v)
}

private fun fmtVal(v: Float): String = formatRoundedAmount(v.toDouble())
