package com.xavierclavel.expenses_tracker.accounts

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavierclavel.expenses_tracker.model.AccountTrendDto
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val COLOR_POSITIVE = Color(0xFF4CAF50)
private val COLOR_NEGATIVE = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountChartsScreen(viewModel: AccountsViewModel, accountId: Int?) {
    val trends by viewModel.trends.collectAsState()
    val timescale = viewModel.timescale
    val display = viewModel.chartDisplay

    LaunchedEffect(accountId) { viewModel.loadTrends(accountId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("month" to "Month", "year" to "Year").forEach { (v, l) ->
                FilterChip(selected = timescale == v, onClick = { viewModel.setTimescale(v, accountId) }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("value" to "Balance", "diff" to "Change", "diff_percent" to "Change %").forEach { (v, l) ->
                FilterChip(selected = display == v, onClick = { viewModel.setChartDisplay(v) }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(12.dp))

        if (trends.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val isPercent = display == "diff_percent"
            val bars = trends.map { dto ->
                val v = when (display) {
                    "diff"         -> dto.change?.toFloatOrNull() ?: 0f
                    "diff_percent" -> (dto.proportionalChange?.toFloatOrNull() ?: 0f) * 100f
                    else           -> dto.balance.toFloatOrNull() ?: 0f
                }
                BarEntry(value = v, label = formatTrendLabel(dto, timescale))
            }
            BarChart(bars = bars, isPercent = isPercent)
        }
    }
}

private fun formatTrendLabel(dto: AccountTrendDto, timescale: String): String {
    if (timescale == "year" || dto.month == null) return dto.year.toString()
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val name = months.getOrElse(dto.month - 1) { dto.month.toString() }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return if (dto.year == currentYear) name else "$name '${dto.year % 100}"
}

data class BarEntry(val value: Float, val label: String)

@Composable
fun BarChart(
    bars: List<BarEntry>,
    barWidth: Dp = 44.dp,
    chartHeight: Dp = 180.dp,
    isPercent: Boolean = false,
) {
    if (bars.isEmpty()) return

    val maxPositive = bars.maxOf { it.value.coerceAtLeast(0f) }
    val maxNegative = bars.maxOf { abs(it.value.coerceAtMost(0f)) }
    val totalRange  = (maxPositive + maxNegative).coerceAtLeast(0.001f)
    val ticks       = remember(maxPositive, maxNegative) { computeTicks(maxPositive, maxNegative) }

    val listState   = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val suffix      = if (isPercent) " %" else " €"
    val labelAreaH  = 28.dp
    val yAxisWidth  = 44.dp

    val barWidthPx = with(LocalDensity.current) { barWidth.roundToPx() }

    // firstVisibleItemIndex and firstVisibleItemScrollOffset are mutableIntStateOf —
    // derivedStateOf tracks them correctly on every scroll frame.
    // centeredIndex = firstIdx + firstOffset / barWidthPx  (derived from scroll geometry)
    val centeredIndex by remember(barWidthPx, bars.size) {
        derivedStateOf {
            val firstIdx = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            (firstIdx + firstOffset.toFloat() / barWidthPx)
                .roundToInt()
                .coerceIn(0, bars.lastIndex)
        }
    }

    LaunchedEffect(bars.size) {
        if (bars.isNotEmpty()) listState.scrollToItem(bars.lastIndex)
    }

    val highlighted = bars.getOrNull(centeredIndex)

    Column {
        // ── Highlighted value ──────────────────────────────────────────────
        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            if (highlighted != null) {
                val sign = if (highlighted.value > 0f) "+" else ""
                Text(
                    text = "$sign${formatBarValue(highlighted.value)}$suffix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted.value >= 0f) COLOR_POSITIVE else COLOR_NEGATIVE,
                )
            }
        }

        Row {
            // ── Y-axis labels (fixed, Compose-native) ──────────────────────
            BoxWithConstraints(modifier = Modifier.width(yAxisWidth).height(chartHeight)) {
                val h = maxHeight
                ticks.forEach { (value, label) ->
                    val fraction = (maxPositive - value) / totalRange
                    if (fraction !in 0f..1f) return@forEach
                    val yOff = (h * fraction - 6.dp).coerceAtLeast(0.dp)
                    Text(
                        text = label,
                        modifier = Modifier
                            .offset(y = yOff)
                            .width(yAxisWidth - 4.dp),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
            }

            // ── Scrollable chart area ──────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(chartHeight + labelAreaH),
            ) {
                val hPad = ((maxWidth - barWidth) / 2).coerceAtLeast(0.dp)

                // Background: fixed graduation lines (drawn behind the bars)
                Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                    val h = size.height
                    val scale = h / totalRange
                    val zeroY = maxPositive * scale
                    ticks.forEach { (value, _) ->
                        val y = zeroY - value * scale
                        if (y < 0f || y > h) return@forEach
                        drawLine(
                            color = if (value == 0f) Color.Gray.copy(alpha = 0.45f)
                                    else            Color.Gray.copy(alpha = 0.13f),
                            start = Offset(0f, y),
                            end   = Offset(size.width, y),
                            strokeWidth = if (value == 0f) 1.dp.toPx() else (0.5f).dp.toPx(),
                        )
                    }
                }

                // Bars + labels (scrollable, on top of graduation lines)
                LazyRow(
                    state = listState,
                    flingBehavior = snapBehavior,
                    modifier = Modifier.height(chartHeight + labelAreaH),
                    contentPadding = PaddingValues(horizontal = hPad),
                ) {
                    itemsIndexed(bars) { index, bar ->
                        val hi        = index == centeredIndex
                        val baseColor = if (bar.value >= 0f) COLOR_POSITIVE else COLOR_NEGATIVE

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(barWidth),
                        ) {
                            Canvas(modifier = Modifier.width(barWidth).height(chartHeight)) {
                                val w     = size.width
                                val h     = size.height
                                val scale = h / totalRange
                                val zeroY = maxPositive * scale
                                if (bar.value == 0f) return@Canvas
                                val barH = abs(bar.value) * scale
                                val top  = if (bar.value > 0f) zeroY - barH else zeroY
                                val radius = 4.dp.toPx()
                                drawRoundRect(
                                    color        = baseColor.copy(alpha = if (hi) 1f else 0.35f),
                                    topLeft      = Offset(w * 0.15f, top),
                                    size         = Size(w * 0.7f, barH),
                                    cornerRadius = CornerRadius(radius, radius),
                                )
                            }
                            Text(
                                text       = bar.label,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = if (hi) baseColor else Color.Gray.copy(alpha = 0.6f),
                                fontWeight = if (hi) FontWeight.Bold else FontWeight.Normal,
                                textAlign  = TextAlign.Center,
                                maxLines   = 1,
                                modifier   = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Tick helpers ───────────────────────────────────────────────────────────────

private fun computeTicks(maxPositive: Float, maxNegative: Float): List<Pair<Float, String>> {
    val max = maxPositive
    val min = -maxNegative
    if (max == 0f && min == 0f) return listOf(0f to "0")
    val range = (max - min).coerceAtLeast(0.001f)
    val step  = niceStep(range / 4f)
    val first = ceil(min / step) * step
    val result = mutableListOf<Pair<Float, String>>()
    var tick = first
    var guard = 0
    while (tick <= max + step * 0.01f && guard++ < 12) {
        val v = if (abs(tick) < step * 0.01f) 0f else tick
        result += v to formatTickLabel(v)
        tick += step
    }
    return result
}

private fun niceStep(raw: Float): Float {
    if (raw <= 0f) return 1f
    val exp = floor(log10(raw.toDouble())).toInt()
    val mag = 10.0.pow(exp).toFloat()
    return mag * when (raw / mag) {
        in 0f..1.5f -> 1f
        in 1.5f..3.5f -> 2f
        in 3.5f..7.5f -> 5f
        else -> 10f
    }
}

private fun formatTickLabel(v: Float): String = when {
    abs(v) >= 1_000_000 -> "${(v / 1_000_000).toInt()}M"
    abs(v) >= 10_000    -> "${(v / 1_000).toInt()}k"
    abs(v) >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
    v % 1f == 0f        -> v.toInt().toString()
    else                -> "%.1f".format(v)
}

private fun formatBarValue(v: Float): String = when {
    abs(v) >= 1_000_000 -> "${"%.2f".format(v / 1_000_000)}M"
    abs(v) >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
    else                -> "%.2f".format(v)
}
