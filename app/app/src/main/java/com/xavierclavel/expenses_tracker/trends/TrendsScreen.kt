package com.xavierclavel.expenses_tracker.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavierclavel.expenses_tracker.accounts.BarChart
import com.xavierclavel.expenses_tracker.categories.CategoriesViewModel
import com.xavierclavel.expenses_tracker.constants.colorHexByName
import com.xavierclavel.expenses_tracker.constants.iconByName
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
    LaunchedEffect(categories) { viewModel.updateCategories(categories) }

    val bars   by viewModel.bars.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isLoading = viewModel.isLoading

    val needsCategory    = viewModel.dataType in listOf("category_in", "category_out")
    val needsSubcategory = viewModel.dataType in listOf("subcategory_in", "subcategory_out")

    val requiredCatType = when (viewModel.dataType) {
        "category_in", "subcategory_in" -> "INCOME"
        else -> "EXPENSE"
    }
    val filteredCategories  = categories.filter { it.type == requiredCatType }
    val filteredSubcategories = filteredCategories.flatMap { it.subcategories }.filter { !it.isDefault }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Chart ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                viewModel.dataType == "income_expense" -> {
                    if (groups.isEmpty()) EmptyChartHint()
                    else GroupedBarChart(groups = groups)
                }
                needsCategory && viewModel.selectedCategory == null ->
                    Text("Select a category", color = MaterialTheme.colorScheme.onSurfaceVariant)
                needsSubcategory && viewModel.selectedSubcategory == null ->
                    Text("Select a subcategory", color = MaterialTheme.colorScheme.onSurfaceVariant)
                bars.isEmpty() -> EmptyChartHint()
                else -> BarChart(bars = bars)
            }
        }

        // ── Controls ───────────────────────────────────────────────────────
        Surface(tonalElevation = 4.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleDropdown(
                        label = "Timescale",
                        selected = viewModel.timescale,
                        options = listOf("month" to "Month", "year" to "Year"),
                        onSelect = { viewModel.setTimescale(it) },
                        modifier = Modifier.weight(1f),
                    )
                    SimpleDropdown(
                        label = "Data",
                        selected = viewModel.dataType,
                        options = listOf(
                            "income_expense" to "In / Out",
                            "flow"           to "Flow",
                            "category_in"    to "Category In",
                            "category_out"   to "Category Out",
                            "subcategory_in" to "Subcategory In",
                            "subcategory_out" to "Subcategory Out",
                        ),
                        onSelect = { viewModel.setDataType(it) },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (viewModel.timescale == "year") {
                    SimpleDropdown(
                        label = "Aggregation",
                        selected = viewModel.aggregation,
                        options = listOf("total" to "Total", "average" to "Mean", "median" to "Median"),
                        onSelect = { viewModel.setAggregation(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (needsCategory) {
                    SimpleDropdown(
                        label = "Category",
                        selected = viewModel.selectedCategory?.name ?: "",
                        options = filteredCategories.map { it.name to it.name },
                        onSelect = { name ->
                            viewModel.setSelectedCategory(filteredCategories.find { it.name == name })
                        },
                        leadingIcon = viewModel.selectedCategory?.let { cat ->
                            { androidx.compose.material3.Icon(
                                imageVector = iconByName(cat.icon),
                                contentDescription = null,
                                tint = colorHexByName(cat.color),
                                modifier = Modifier.padding(end = 4.dp),
                            ) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (needsSubcategory) {
                    SimpleDropdown(
                        label = "Subcategory",
                        selected = viewModel.selectedSubcategory?.name ?: "",
                        options = filteredSubcategories.map { it.name to it.name },
                        onSelect = { name ->
                            viewModel.setSelectedSubcategory(filteredSubcategories.find { it.name == name })
                        },
                        leadingIcon = viewModel.selectedSubcategory?.let { sub ->
                            { androidx.compose.material3.Icon(
                                imageVector = iconByName(sub.icon),
                                contentDescription = null,
                                tint = colorHexByName(sub.color),
                                modifier = Modifier.padding(end = 4.dp),
                            ) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChartHint() {
    Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ── Grouped bar chart (income_expense mode) ────────────────────────────────────

@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    groupWidth: Dp = 52.dp,
    chartHeight: Dp = 180.dp,
) {
    if (groups.isEmpty()) return

    val maxVal      = groups.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(0.001f)
    val ticks       = remember(maxVal) { simpleTicks(maxVal) }
    val labelAreaH  = 28.dp
    val yAxisWidth  = 44.dp

    val listState   = rememberLazyListState()
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
        // Value display
        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            if (highlighted != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("+${fmtVal(highlighted.income)} €",  color = COLOR_INCOME,  fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("-${fmtVal(highlighted.expense)} €", color = COLOR_EXPENSE, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
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
                        text = label,
                        modifier = Modifier.offset(y = yOff).width(yAxisWidth - 4.dp),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        maxLines = 1,
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
                            start = Offset(0f, y),
                            end   = Offset(size.width, y),
                            strokeWidth = if (value == 0f) 1.dp.toPx() else (0.5f).dp.toPx(),
                        )
                    }
                }

                LazyRow(
                    state = listState,
                    flingBehavior = snapBehavior,
                    modifier = Modifier.height(chartHeight + labelAreaH),
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

                                drawRoundRect(
                                    color        = COLOR_INCOME.copy(alpha = alpha),
                                    topLeft      = Offset(sideGap, h - incomeH),
                                    size         = Size(subBarW, incomeH),
                                    cornerRadius = r,
                                )
                                drawRoundRect(
                                    color        = COLOR_EXPENSE.copy(alpha = alpha),
                                    topLeft      = Offset(sideGap + subBarW + innerGap, h - expenseH),
                                    size         = Size(subBarW, expenseH),
                                    cornerRadius = r,
                                )
                            }
                            Text(
                                text       = group.label,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = if (hi) MaterialTheme.colorScheme.onBackground else Color.Gray.copy(0.6f),
                                fontWeight = if (hi) FontWeight.Bold else FontWeight.Normal,
                                textAlign  = TextAlign.Center,
                                maxLines   = 1,
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

// ── Dropdown helper ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second ?: selected

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            leadingIcon = leadingIcon,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, displayLabel) ->
                DropdownMenuItem(
                    text = { Text(displayLabel) },
                    onClick = { onSelect(value); expanded = false },
                )
            }
        }
    }
}

// ── Tick helpers (positive-only for grouped chart) ────────────────────────────

private fun simpleTicks(max: Float): List<Pair<Float, String>> {
    if (max <= 0f) return listOf(0f to "0")
    val step  = niceStep(max / 4f)
    val result = mutableListOf<Pair<Float, String>>()
    var tick = 0f
    var guard = 0
    while (tick <= max + step * 0.01f && guard++ < 8) {
        result += tick to fmtTick(tick)
        tick += step
    }
    return result
}

private fun niceStep(raw: Float): Float {
    if (raw <= 0f) return 1f
    val exp = floor(log10(raw.toDouble())).toInt()
    val mag = 10.0.pow(exp).toFloat()
    return mag * when (raw / mag) {
        in 0f..1.5f  -> 1f
        in 1.5f..3.5f -> 2f
        in 3.5f..7.5f -> 5f
        else          -> 10f
    }
}

private fun fmtTick(v: Float): String = when {
    abs(v) >= 1_000_000 -> "${(v / 1_000_000).toInt()}M"
    abs(v) >= 10_000    -> "${(v / 1_000).toInt()}k"
    abs(v) >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
    v % 1f == 0f        -> v.toInt().toString()
    else                -> "%.1f".format(v)
}

private fun fmtVal(v: Float): String = when {
    abs(v) >= 1_000_000 -> "${"%.2f".format(v / 1_000_000)}M"
    abs(v) >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
    else                -> "%.2f".format(v)
}
