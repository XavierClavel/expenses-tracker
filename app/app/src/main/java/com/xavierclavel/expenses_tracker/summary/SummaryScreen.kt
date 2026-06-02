package com.xavierclavel.expenses_tracker.summary

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xavierclavel.expenses_tracker.categories.CategoriesViewModel
import com.xavierclavel.expenses_tracker.constants.colorHexByName
import com.xavierclavel.expenses_tracker.constants.iconByName
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

private val MONTH_NAMES = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December",
)

data class PieEntry(
    val value: Float,
    val label: String,
    val colorHex: String?,
    val icon: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel,
    categoriesViewModel: CategoriesViewModel,
) {
    val categories by categoriesViewModel.categories.collectAsState()
    val summary = viewModel.summary
    val isLoading = viewModel.isLoading

    val subcategoryParentMap = remember(categories) {
        categories.flatMap { cat -> cat.subcategories.map { sub -> sub.id to cat } }.toMap()
    }

    val subcatSummaries = if (viewModel.selectedType == "EXPENSE")
        summary?.expensesByCategory ?: emptyList()
    else
        summary?.incomeByCategory ?: emptyList()

    val pieEntries = remember(categories, subcatSummaries, viewModel.selectedType) {
        categories
            .filter { it.type == viewModel.selectedType }
            .mapNotNull { cat ->
                val total = subcatSummaries
                    .filter { subcategoryParentMap[it.categoryId]?.id == cat.id }
                    .sumOf { it.total.toDoubleOrNull() ?: 0.0 }
                if (total <= 0.0) null
                else PieEntry(
                    value = total.toFloat(),
                    label = cat.name,
                    colorHex = cat.color,
                    icon = cat.icon,
                )
            }
            .sortedByDescending { it.value }
    }

    // Resets to -1 (nothing selected) whenever the loaded data changes
    var selectedSlice by remember(summary, viewModel.selectedType) { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Horizontal swipe must come before verticalScroll so it can intercept
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
                else
                    viewModel.selectedYear.toString(),
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
                    onClick = { viewModel.setTimescale(v) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(l) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("EXPENSE" to "Expenses", "INCOME" to "Income").forEachIndexed { i, (v, l) ->
                SegmentedButton(
                    selected = viewModel.selectedType == v,
                    onClick = { viewModel.setSelectedType(v) },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                    label = { Text(l) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Totals cards ───────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalCard("Expenses", summary?.totalExpenses?.toDoubleOrNull() ?: 0.0, Color(0xFFE53935), "-", Modifier.weight(1f))
            TotalCard("Income",   summary?.totalIncome?.toDoubleOrNull()   ?: 0.0, Color(0xFF4CAF50), "+", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // ── Pie chart + legend ─────────────────────────────────────────────
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pieEntries.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No data for this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val pieTotal = pieEntries.sumOf { it.value.toDouble() }.toFloat()

            DonutChart(
                entries = pieEntries,
                total = pieTotal,
                selectedIndex = selectedSlice,
                onSliceClick = { i -> selectedSlice = if (selectedSlice == i) -1 else i },
                modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(16.dp))

            pieEntries.forEachIndexed { index, entry ->
                LegendRow(
                    entry = entry,
                    total = pieTotal,
                    isSelected = index == selectedSlice,
                    onClick = { selectedSlice = if (selectedSlice == index) -1 else index },
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
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
    val anySelected = selectedIndex != -1
    val selectedEntry = entries.getOrNull(selectedIndex)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(entries, total) {
                    detectTapGestures { tap ->
                        val cx = size.width  / 2f
                        val cy = size.height / 2f
                        val dx = tap.x - cx
                        val dy = tap.y - cy
                        val dist = sqrt(dx * dx + dy * dy)

                        val outerR = minOf(size.width, size.height) / 2f
                        val strokeW = outerR * 0.28f
                        val innerR = outerR - strokeW

                        if (dist < innerR || dist > outerR) {
                            onSliceClick(-1)
                            return@detectTapGestures
                        }

                        // Convert to angle: 0° = top, clockwise
                        val rawDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
                        val tapAngle = ((rawDeg + 90f) % 360f + 360f) % 360f

                        var cumAngle = 0f
                        entries.forEachIndexed { i, e ->
                            val sweep = 360f * e.value / total
                            if (tapAngle >= cumAngle && tapAngle < cumAngle + sweep) {
                                onSliceClick(i)
                                return@detectTapGestures
                            }
                            cumAngle += sweep
                        }
                        onSliceClick(-1)
                    }
                }
        ) {
            val diameter   = minOf(size.width, size.height)
            val radius     = diameter / 2f
            val strokeBase = radius * 0.28f
            val gapDeg     = 1.5f

            var startAngle = -90f
            entries.forEachIndexed { i, entry ->
                val fraction   = entry.value / total
                val sweepAngle = 360f * fraction - gapDeg
                val isSelected = i == selectedIndex
                val alpha      = when { !anySelected -> 1f; isSelected -> 1f; else -> 0.25f }
                val stroke     = if (isSelected) strokeBase * 1.15f else strokeBase

                val inset   = stroke / 2f
                val topLeft = Offset((size.width - diameter) / 2f + inset, (size.height - diameter) / 2f + inset)
                val arcSize = Size(diameter - stroke, diameter - stroke)

                drawArc(
                    color       = colorHexByName(entry.colorHex).copy(alpha = alpha),
                    startAngle  = startAngle + gapDeg / 2f,
                    sweepAngle  = sweepAngle.coerceAtLeast(0.1f),
                    useCenter   = false,
                    topLeft     = topLeft,
                    size        = arcSize,
                    style       = Stroke(width = stroke),
                )
                startAngle += 360f * fraction
            }
        }

        // Center label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(90.dp),
        ) {
            Text(
                text = if (selectedEntry != null) formatSummaryAmount(selectedEntry.value.toDouble())
                       else formatSummaryAmount(total.toDouble()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (selectedEntry != null) colorHexByName(selectedEntry.colorHex)
                        else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = selectedEntry?.label ?: "€",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LegendRow(
    entry: PieEntry,
    total: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val pct        = if (total > 0f) entry.value / total * 100f else 0f
    val fraction   = (entry.value / total).coerceIn(0f, 1f)
    val entryColor = colorHexByName(entry.colorHex)
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) entryColor.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 0.dp else 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = iconByName(entry.icon),
                    contentDescription = null,
                    tint = entryColor,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "%.1f%%".format(pct),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.End,
                )
                Text(
                    text = formatSummaryAmount(entry.value.toDouble()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = entryColor,
                    modifier = Modifier.width(76.dp),
                    textAlign = TextAlign.End,
                )
            }

            // Progress bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .height(4.dp)
            ) {
                val r = CornerRadius(2.dp.toPx())
                drawRoundRect(color = trackColor, cornerRadius = r)
                if (fraction > 0f) {
                    drawRoundRect(
                        color = entryColor,
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = r,
                    )
                }
            }
        }
    }
}

private fun formatSummaryAmount(value: Double): String = when {
    value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)}M"
    value >= 10_000    -> "${(value / 1_000).toInt()}k"
    value >= 1_000     -> "${"%.1f".format(value / 1_000)}k"
    else               -> "${"%.0f".format(value)}"
}
