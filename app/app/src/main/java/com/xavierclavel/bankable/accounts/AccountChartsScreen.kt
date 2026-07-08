package com.xavierclavel.bankable.accounts

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.model.AccountTrendDto
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val COLOR_POSITIVE = Color(0xFF4CAF50)
private val COLOR_NEGATIVE = Color(0xFFE53935)
private val COLOR_CONTRIB  = Color(0xFF42A5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountChartsScreen(viewModel: AccountsViewModel, accountId: Int?) {
    val trends by viewModel.trends.collectAsState()
    val timescale = viewModel.timescale
    val display = viewModel.chartDisplay
    val source = viewModel.chartSource

    LaunchedEffect(accountId) { viewModel.loadTrends(accountId) }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("month" to stringResource(R.string.label_month), "year" to stringResource(R.string.label_year)).forEach { (v, l) ->
                FilterChip(selected = timescale == v, onClick = { viewModel.setTimescale(v, accountId) }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("value" to stringResource(R.string.label_balance), "diff" to stringResource(R.string.label_change), "diff_percent" to stringResource(R.string.label_change_percent)).forEach { (v, l) ->
                FilterChip(selected = display == v, onClick = { viewModel.setChartDisplay(v) }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("both" to stringResource(R.string.source_both), "transfers" to stringResource(R.string.source_transfers), "interests" to stringResource(R.string.source_interests)).forEach { (v, l) ->
                FilterChip(selected = source == v, onClick = { viewModel.setChartSource(v) }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(8.dp))

        // Legend for the stacked view (contributions + interest).
        if (source == "both" && display == "value") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                LegendDot(COLOR_CONTRIB, stringResource(R.string.source_transfers))
                Spacer(Modifier.width(16.dp))
                LegendDot(COLOR_POSITIVE, stringResource(R.string.source_interests))
            }
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            val chartH = (maxHeight - 80.dp).coerceAtLeast(120.dp)

            if (trends.isEmpty()) {
                Text(stringResource(R.string.label_no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val isPercent = display == "diff_percent"
                // Cumulative value of the selected source at each period.
                val cumulative = trends.map { dto ->
                    val balance = dto.balance.toFloatOrNull() ?: 0f
                    val contrib = dto.contributions?.toFloatOrNull() ?: 0f
                    when (source) {
                        "transfers" -> contrib
                        "interests" -> balance - contrib
                        else         -> balance
                    }
                }
                val labels = trends.map { formatTrendLabel(it, timescale, locale) }

                val bars = if (display == "value" && source == "both") {
                    // Stacked bars: contributions (blue) + interest (green/red), summing to balance.
                    trends.mapIndexed { i, dto ->
                        val balance = dto.balance.toFloatOrNull() ?: 0f
                        val contrib = dto.contributions?.toFloatOrNull() ?: 0f
                        val interest = balance - contrib
                        BarEntry(
                            value = balance,
                            label = labels[i],
                            segments = listOf(
                                BarSegment(contrib, COLOR_CONTRIB),
                                BarSegment(interest, if (interest >= 0f) COLOR_POSITIVE else COLOR_NEGATIVE),
                            ),
                        )
                    }
                } else {
                    trends.mapIndexed { i, _ ->
                        val v = when (display) {
                            "diff"         -> if (i == 0) 0f else cumulative[i] - cumulative[i - 1]
                            "diff_percent" -> {
                                if (source == "interests" || source == "both") {
                                    // The % variation of the account is its return: the period's interest
                                    // over its time-weighted average capital (Modified Dietz, mid-period
                                    // transfers weighted by how long they were invested), NOT the raw
                                    // balance change — otherwise money you add would inflate it. Same
                                    // figure whether viewing the whole balance ("both") or interest only.
                                    (trends[i].returnRate?.toFloatOrNull() ?: 0f) * 100f
                                } else {
                                    // "transfers": how much your contributions grew this period.
                                    val denom = if (i == 0) 0f else cumulative[i - 1]
                                    if (i == 0 || denom == 0f) 0f else (cumulative[i] - cumulative[i - 1]) / kotlin.math.abs(denom) * 100f
                                }
                            }
                            else           -> cumulative[i]
                        }
                        BarEntry(value = v, label = labels[i])
                    }
                }

                // Single-source "value" bars for transfers read as contributions (blue);
                // interest keeps its sign-based green/red coloring.
                val accent = if (display == "value" && source == "transfers") COLOR_CONTRIB else null
                BarChart(bars = bars, isPercent = isPercent, chartHeight = chartH, accentColor = accent)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawRoundRect(color = color, cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()))
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatTrendLabel(dto: AccountTrendDto, timescale: String, locale: java.util.Locale): String {
    if (timescale == "year" || dto.month == null) return dto.year.toString()
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, dto.year)
        set(Calendar.MONTH, dto.month - 1)
    }
    val name = java.text.SimpleDateFormat("MMM", locale).format(cal.time)
        .replaceFirstChar { it.titlecase(locale) }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    return if (dto.year == currentYear) name else "$name '${dto.year % 100}"
}

// A single stacked segment of a bar, drawn as a waterfall from the running baseline.
data class BarSegment(val value: Float, val color: Color)

// `value` is the net/total shown in the highlight readout. When `segments` is
// non-empty the bar is drawn stacked (segments summing to `value`); otherwise a
// single bar of `value` is drawn.
data class BarEntry(val value: Float, val label: String, val segments: List<BarSegment> = emptyList())

@Composable
fun BarChart(
    bars: List<BarEntry>,
    barWidth: Dp = 44.dp,
    chartHeight: Dp = 180.dp,
    isPercent: Boolean = false,
    accentColor: Color? = null,
) {
    if (bars.isEmpty()) return

    // Vertical extent of a bar: the highest and lowest points its (possibly stacked)
    // segments reach relative to zero. Single bars just reach their own value.
    fun extent(bar: BarEntry): Pair<Float, Float> {
        if (bar.segments.isEmpty()) return bar.value.coerceAtLeast(0f) to bar.value.coerceAtMost(0f)
        var base = 0f; var hi = 0f; var lo = 0f
        bar.segments.forEach { base += it.value; hi = kotlin.math.max(hi, base); lo = kotlin.math.min(lo, base) }
        return hi to lo
    }

    val maxPositive = bars.maxOf { extent(it).first }
    val maxNegative = bars.maxOf { abs(extent(it).second) }
    val totalRange  = (maxPositive + maxNegative).coerceAtLeast(0.001f)
    val ticks       = remember(maxPositive, maxNegative) { computeTicks(maxPositive, maxNegative) }

    val listState   = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val suffix      = if (isPercent) " %" else " €"
    val labelAreaH  = 36.dp
    val yAxisWidth  = 22.dp

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
                val displayValue = if (accentColor != null) abs(highlighted.value) else highlighted.value
                val sign = if (accentColor == null && highlighted.value > 0f) "+" else ""
                Text(
                    text = "$sign${formatBarValue(displayValue, isPercent)}$suffix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        highlighted.value == 0f -> Color.Gray
                        accentColor != null     -> accentColor
                        highlighted.value > 0f  -> COLOR_POSITIVE
                        else                    -> COLOR_NEGATIVE
                    },
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
                        val baseColor = accentColor ?: if (bar.value >= 0f) COLOR_POSITIVE else COLOR_NEGATIVE

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(barWidth),
                        ) {
                            Canvas(modifier = Modifier.width(barWidth).height(chartHeight)) {
                                val w     = size.width
                                val h     = size.height
                                val scale = h / totalRange
                                val zeroY = maxPositive * scale
                                val x      = w * 0.15f
                                val bw     = w * 0.7f
                                val radius = 4.dp.toPx()
                                val alpha  = if (hi) 1f else 0.35f
                                if (bar.segments.isNotEmpty()) {
                                    var base = 0f
                                    bar.segments.forEach { seg ->
                                        if (seg.value != 0f) {
                                            val yTop = zeroY - (base + seg.value) * scale
                                            val yBase = zeroY - base * scale
                                            drawRoundRect(
                                                color        = seg.color.copy(alpha = alpha),
                                                topLeft      = Offset(x, kotlin.math.min(yTop, yBase)),
                                                size         = Size(bw, abs(yBase - yTop)),
                                                cornerRadius = CornerRadius(radius, radius),
                                            )
                                        }
                                        base += seg.value
                                    }
                                } else if (bar.value != 0f) {
                                    val barH = abs(bar.value) * scale
                                    val top  = if (bar.value > 0f) zeroY - barH else zeroY
                                    drawRoundRect(
                                        color        = baseColor.copy(alpha = alpha),
                                        topLeft      = Offset(x, top),
                                        size         = Size(bw, barH),
                                        cornerRadius = CornerRadius(radius, radius),
                                    )
                                }
                            }
                            Text(
                                text       = bar.label,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = if (hi) baseColor else Color.Gray.copy(alpha = 0.6f),
                                fontWeight = if (hi) FontWeight.Bold else FontWeight.Normal,
                                textAlign  = TextAlign.Center,
                                maxLines   = 2,
                                modifier   = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            // Mirror the y-axis width on the right so the scrollable area is
            // symmetric on the full screen and bars snap to true visual centre.
            Spacer(Modifier.width(yAxisWidth))
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

private fun formatBarValue(v: Float, isPercent: Boolean = false): String = when {
    !isPercent          -> formatRoundedAmount(v.toDouble())
    abs(v) >= 1_000_000 -> "${"%.2f".format(v / 1_000_000)}M"
    abs(v) >= 1_000     -> "${"%.1f".format(v / 1_000)}k"
    else                -> "%.2f".format(v)
}
