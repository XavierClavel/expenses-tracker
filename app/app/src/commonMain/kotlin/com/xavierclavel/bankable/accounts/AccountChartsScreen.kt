package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xavierclavel.bankable.constants.formatRoundedAmount
import com.xavierclavel.bankable.model.AccountTrendDto
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.platform.platformFormatMonthYear
import com.xavierclavel.bankable.platform.platformShortMonthName
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.interest_metric_earned
import com.xavierclavel.bankable.resources.interest_metric_return
import com.xavierclavel.bankable.resources.interest_metric_total
import com.xavierclavel.bankable.resources.label_balance
import com.xavierclavel.bankable.resources.label_change
import com.xavierclavel.bankable.resources.label_change_percent
import com.xavierclavel.bankable.resources.label_month
import com.xavierclavel.bankable.resources.label_no_data
import com.xavierclavel.bankable.resources.label_year
import com.xavierclavel.bankable.resources.source_both
import com.xavierclavel.bankable.resources.source_interests
import com.xavierclavel.bankable.resources.source_transfers
import com.xavierclavel.bankable.ui.ChartAxisLabel
import com.xavierclavel.bankable.ui.ChartAxisLabelHeight
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.PillChip
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightDivider
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.ui.theme.ChartNeutral
import com.xavierclavel.bankable.util.currentMonth
import com.xavierclavel.bankable.util.currentYear
import com.xavierclavel.bankable.util.formatFixed
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

private val COLOR_POSITIVE = Mint
private val COLOR_NEGATIVE = Danger
private val COLOR_CONTRIB  = ChartNeutral

@Composable
fun AccountChartsScreen(viewModel: AccountsViewModel, accountId: Int?) {
    val trends by viewModel.trends.collectAsState()
    val timescale = viewModel.timescale
    val display = viewModel.chartDisplay
    val source = viewModel.chartSource
    // Interest-tracking accounts record interest directly, so their first period's
    // interest is known (only meaningful for a single account, not the all-accounts view).
    val interestMode = accountId != null && viewModel.selectedAccount?.tracking == TRACKING_INTEREST

    LaunchedEffect(accountId) { viewModel.loadTrends(accountId) }
    val locale = LocalAppLocale.current
    var highlightedIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChipRow(
            options = listOf("month" to stringResource(Res.string.label_month), "year" to stringResource(Res.string.label_year)),
            selected = timescale,
            onSelect = { viewModel.setTimescale(it, accountId) },
        )
        // For interest, "change" is the interest earned and "change %" is the return rate,
        // so relabel the display modes to avoid the misleading "Variation" wording.
        val displayLabels = if (source == "interests")
            listOf(
                "value" to stringResource(Res.string.interest_metric_total),
                "diff" to stringResource(Res.string.interest_metric_earned),
                "diff_percent" to stringResource(Res.string.interest_metric_return),
            )
        else
            listOf(
                "value" to stringResource(Res.string.label_balance),
                "diff" to stringResource(Res.string.label_change),
                "diff_percent" to stringResource(Res.string.label_change_percent),
            )
        ChipRow(options = displayLabels, selected = display, onSelect = { viewModel.setChartDisplay(it) })
        ChipRow(
            options = listOf(
                "both" to stringResource(Res.string.source_both),
                "transfers" to stringResource(Res.string.source_transfers),
                "interests" to stringResource(Res.string.source_interests),
            ),
            selected = source,
            onSelect = { viewModel.setChartSource(it) },
        )
        Spacer(Modifier.height(6.dp))

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
            // Stacked bars: contributions (blue) + interest (mint/pink), summing to balance.
            trends.mapIndexed { i, dto ->
                val balance = dto.balance.toFloatOrNull() ?: 0f
                val contrib = dto.contributions?.toFloatOrNull() ?: 0f
                val interest = balance - contrib
                BarEntry(
                    value = balance,
                    label = labels[i],
                    isCurrent = isCurrentPeriod(dto.year, dto.month.takeIf { timescale != "year" }),
                    segments = listOf(
                        BarSegment(contrib, COLOR_CONTRIB),
                        BarSegment(interest, if (interest >= 0f) COLOR_POSITIVE else COLOR_NEGATIVE),
                    ),
                )
            }
        } else {
            trends.mapIndexed { i, _ ->
                val v = when (display) {
                    "diff"         -> if (i == 0) {
                        // Interest accounts know their first period's interest (from a zero base);
                        // other series have no prior period to diff against.
                        if (source == "interests" && interestMode) cumulative[0] else 0f
                    } else cumulative[i] - cumulative[i - 1]
                    "diff_percent" -> {
                        if (source == "interests") {
                            // Interest is shown as the true return: the period's interest over its
                            // time-weighted average capital (Modified Dietz, mid-period transfers
                            // weighted by how long they were invested) — not inflated by deposits.
                            (trends[i].returnRate?.toFloatOrNull() ?: 0f) * 100f
                        } else {
                            // "both": total balance growth (deposits included).
                            // "transfers": how much your contributions grew this period.
                            val denom = if (i == 0) 0f else cumulative[i - 1]
                            if (i == 0 || denom == 0f) 0f else (cumulative[i] - cumulative[i - 1]) / kotlin.math.abs(denom) * 100f
                        }
                    }
                    else           -> cumulative[i]
                }
                BarEntry(
                    value = v,
                    label = labels[i],
                    isCurrent = isCurrentPeriod(trends[i].year, trends[i].month.takeIf { timescale != "year" }),
                )
            }
        }

        NightCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 16.dp),
        ) {
            // Legend for the stacked view (contributions + interest).
            if (source == "both" && display == "value") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LegendDot(COLOR_CONTRIB, stringResource(Res.string.source_transfers))
                    Spacer(Modifier.width(18.dp))
                    LegendDot(COLOR_POSITIVE, stringResource(Res.string.source_interests))
                }
            }

            if (bars.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.label_no_data), color = NightTextSecondary)
                }
            } else {
                // Single-source "value" bars for transfers read as contributions (blue);
                // interest keeps its sign-based mint/pink coloring.
                val accent = if (display == "value" && source == "transfers") COLOR_CONTRIB else null
                BarChart(
                    bars = bars,
                    isPercent = isPercent,
                    chartHeight = 210.dp,
                    accentColor = accent,
                    barWidth = if (timescale == "year") 56.dp else 30.dp,
                    showReadout = false,
                    valueLabels = timescale == "year",
                    onHighlightChange = { highlightedIndex = it },
                )
            }
        }

        val trend = trends.getOrNull(highlightedIndex)
        val bar = bars.getOrNull(highlightedIndex)
        if (trend != null && bar != null) {
            PeriodSummary(
                trend = trend,
                bar = bar,
                timescale = timescale,
                locale = locale,
                isPercent = isPercent,
                signed = display != "value",
                showBreakdown = source == "both" && display == "value",
            )
        }
    }
}

@Composable
private fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            PillChip(text = label, selected = selected == value, onClick = { onSelect(value) })
        }
    }
}

// Readout for the period currently centred in the chart: its name, the bar's value and,
// for the stacked balance view, how it splits between transfers and interest.
@Composable
private fun PeriodSummary(
    trend: AccountTrendDto,
    bar: BarEntry,
    timescale: String,
    locale: String,
    isPercent: Boolean,
    signed: Boolean,
    showBreakdown: Boolean,
) {
    val title = if (timescale == "year" || trend.month == null) trend.year.toString()
    else platformFormatMonthYear(trend.year, trend.month, locale).replaceFirstChar { it.titlecase() }
    val sign = if (signed && bar.value > 0f) "+" else ""
    val valueText = "$sign${formatBarValue(bar.value, isPercent)}${if (isPercent) " %" else " €"}"
    val valueColor = when {
        !signed || bar.value == 0f -> NightTextPrimary
        bar.value > 0f             -> COLOR_POSITIVE
        else                       -> COLOR_NEGATIVE
    }
    NightCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NightTextPrimary)
                if (showBreakdown) {
                    val balance = trend.balance.toDoubleOrNull() ?: 0.0
                    val contrib = trend.contributions?.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "${stringResource(Res.string.source_transfers)} ${formatAmount(contrib)} · " +
                            "${stringResource(Res.string.source_interests)} ${formatAmount(balance - contrib)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NightTextSecondary,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(valueText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawRoundRect(color = color, cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = NightTextSecondary)
    }
}

private fun formatTrendLabel(dto: AccountTrendDto, timescale: String, locale: String): String {
    if (timescale == "year" || dto.month == null) return dto.year.toString()
    val name = platformShortMonthName(dto.year, dto.month, locale)
        .replaceFirstChar { it.titlecase() }
    return if (dto.year == currentYear()) name else "$name '${dto.year % 100}"
}

// A single stacked segment of a bar, drawn as a waterfall from the running baseline.
data class BarSegment(val value: Float, val color: Color)

/**
 * The parts of each waterfall segment left visible once later segments are drawn,
 * as (low, high, colour) in value space. A later segment that goes back down (negative
 * interest after positive transfers) covers the top of the earlier one instead of
 * overlapping it, so the earlier bar ends at the running total.
 */
private fun visibleSegmentPieces(segments: List<BarSegment>): List<Triple<Float, Float, Color>> {
    val intervals = mutableListOf<Pair<Float, Float>>()
    var base = 0f
    segments.forEach { seg ->
        intervals += kotlin.math.min(base, base + seg.value) to kotlin.math.max(base, base + seg.value)
        base += seg.value
    }
    val pieces = mutableListOf<Triple<Float, Float, Color>>()
    segments.forEachIndexed { i, seg ->
        if (seg.value == 0f) return@forEachIndexed
        val (lo, hi) = intervals[i]
        val later = intervals.drop(i + 1).filter { it.first != it.second }
        if (later.isEmpty()) {
            pieces += Triple(lo, hi, seg.color)
            return@forEachIndexed
        }
        // Later segments chain on from this one's end, so together they span one interval.
        val coverLo = later.minOf { it.first }
        val coverHi = later.maxOf { it.second }
        if (lo < coverLo) pieces += Triple(lo, kotlin.math.min(hi, coverLo), seg.color)
        if (hi > coverHi) pieces += Triple(kotlin.math.max(lo, coverHi), hi, seg.color)
    }
    return pieces
}

// `value` is the net/total shown in the highlight readout. When `segments` is
// non-empty the bar is drawn stacked (segments summing to `value`); otherwise a
// single bar of `value` is drawn.
// `year` is the calendar year behind a monthly bar, for callers that group by year.
data class BarEntry(
    val value: Float,
    val label: String,
    val segments: List<BarSegment> = emptyList(),
    val year: Int? = null,
    // The period containing today, which charts select when they open.
    val isCurrent: Boolean = false,
)

/** True for the current month, or the current year when [month] is null. */
internal fun isCurrentPeriod(year: Int, month: Int?): Boolean =
    year == currentYear() && (month == null || month == currentMonth())

/** Where a chart opens: the current period, or the latest one when it has no bar. */
internal fun initialBarIndex(count: Int, isCurrent: (Int) -> Boolean): Int =
    (0 until count).lastOrNull(isCurrent) ?: (count - 1)

/**
 * Horizontally scrolling bar chart that snaps a bar to the centre and highlights it.
 * [showReadout] draws the highlighted value above the chart; callers that show their
 * own summary turn it off and listen to [onHighlightChange]. [valueLabels] prints each
 * bar's compact value above it.
 */
@Composable
fun BarChart(
    bars: List<BarEntry>,
    barWidth: Dp = 44.dp,
    chartHeight: Dp = 180.dp,
    isPercent: Boolean = false,
    accentColor: Color? = null,
    showReadout: Boolean = true,
    valueLabels: Boolean = false,
    onHighlightChange: ((Int) -> Unit)? = null,
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
    val labelAreaH  = ChartAxisLabelHeight
    val yAxisWidth  = 26.dp
    // Headroom above the tallest bar for its value label.
    val topPad      = if (valueLabels) 22.dp else 4.dp
    val textMeasurer = rememberTextMeasurer()

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
        if (bars.isNotEmpty()) listState.scrollToItem(initialBarIndex(bars.size) { bars[it].isCurrent })
    }
    LaunchedEffect(centeredIndex) { onHighlightChange?.invoke(centeredIndex) }

    val highlighted = bars.getOrNull(centeredIndex)

    Column {
        // ── Highlighted value ──────────────────────────────────────────────
        if (showReadout) {
            Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                if (highlighted != null) {
                    val displayValue = if (accentColor != null) abs(highlighted.value) else highlighted.value
                    val sign = if (accentColor == null && highlighted.value > 0f) "+" else ""
                    Text(
                        text = "$sign${formatBarValue(displayValue, isPercent)}$suffix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            highlighted.value == 0f -> NightTextSecondary
                            accentColor != null     -> accentColor
                            highlighted.value > 0f  -> COLOR_POSITIVE
                            else                    -> COLOR_NEGATIVE
                        },
                    )
                }
            }
        }

        Row {
            // ── Y-axis labels (fixed, Compose-native) ──────────────────────
            BoxWithConstraints(modifier = Modifier.width(yAxisWidth).height(chartHeight)) {
                val plotH = maxHeight - topPad
                ticks.forEach { (value, label) ->
                    val fraction = (maxPositive - value) / totalRange
                    if (fraction !in 0f..1f) return@forEach
                    val yOff = (topPad + plotH * fraction - 6.dp).coerceAtLeast(0.dp)
                    Text(
                        text = label,
                        modifier = Modifier
                            .offset(y = yOff)
                            .width(yAxisWidth - 4.dp),
                        fontSize = 9.sp,
                        color = NightTextTertiary,
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
                    val top = topPad.toPx()
                    val scale = (size.height - top) / totalRange
                    val zeroY = top + maxPositive * scale
                    ticks.forEach { (value, _) ->
                        val y = zeroY - value * scale
                        if (y < 0f || y > size.height + 0.5f) return@forEach
                        drawLine(
                            color = if (value == 0f) NightBorder else NightDivider,
                            start = Offset(0f, y),
                            end   = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
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
                        val valueLabel = if (valueLabels) formatCompactValue(bar.value, isPercent) else null
                        val valueStyle = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hi) NightTextPrimary else NightTextSecondary,
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(barWidth),
                        ) {
                            Canvas(modifier = Modifier.width(barWidth).height(chartHeight)) {
                                val w     = size.width
                                val top   = topPad.toPx()
                                val scale = (size.height - top) / totalRange
                                val zeroY = top + maxPositive * scale
                                val x      = w * 0.16f
                                val bw     = w * 0.68f
                                val radius = 5.dp.toPx()
                                val gap    = 2.dp.toPx()
                                val alpha  = if (hi) 1f else 0.55f
                                var barTop = zeroY
                                if (bar.segments.isNotEmpty()) {
                                    visibleSegmentPieces(bar.segments).forEach { (lo, hi, color) ->
                                        var top = zeroY - hi * scale
                                        var bottom = zeroY - lo * scale
                                        // Separate stacked segments with a thin gap where they meet.
                                        if (bottom - top > gap * 2) {
                                            if (hi != 0f) top += gap / 2
                                            if (lo != 0f) bottom -= gap / 2
                                        }
                                        drawRoundRect(
                                            color        = color.copy(alpha = alpha),
                                            topLeft      = Offset(x, top),
                                            size         = Size(bw, bottom - top),
                                            cornerRadius = CornerRadius(radius, radius),
                                        )
                                        barTop = kotlin.math.min(barTop, top)
                                    }
                                } else if (bar.value != 0f) {
                                    val barH = abs(bar.value) * scale
                                    val top0 = if (bar.value > 0f) zeroY - barH else zeroY
                                    drawRoundRect(
                                        color        = baseColor.copy(alpha = alpha),
                                        topLeft      = Offset(x, top0),
                                        size         = Size(bw, barH),
                                        cornerRadius = CornerRadius(radius, radius),
                                    )
                                    barTop = top0
                                }
                                if (valueLabel != null) {
                                    val layout = textMeasurer.measure(valueLabel, valueStyle)
                                    drawText(
                                        textLayoutResult = layout,
                                        topLeft = Offset(
                                            (w - layout.size.width) / 2f,
                                            (barTop - layout.size.height - 3.dp.toPx()).coerceAtLeast(0f),
                                        ),
                                    )
                                }
                            }
                            ChartAxisLabel(text = bar.label, highlighted = hi)
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
    abs(v) >= 1_000     -> "${formatFixed((v / 1_000).toDouble(), 1)}k"
    v % 1f == 0f        -> v.toInt().toString()
    else                -> formatFixed((v).toDouble(), 1)
}

// Value printed above a bar: "22.8k", "640", "4.2".
private fun formatCompactValue(v: Float, isPercent: Boolean): String = when {
    isPercent           -> formatFixed(v.toDouble(), 1)
    abs(v) >= 1_000_000 -> "${formatFixed((v / 1_000_000).toDouble(), 1)}M"
    abs(v) >= 1_000     -> "${formatFixed((v / 1_000).toDouble(), 1)}k"
    else                -> formatRoundedAmount(v.toDouble())
}

private fun formatBarValue(v: Float, isPercent: Boolean = false): String = when {
    !isPercent          -> formatRoundedAmount(v.toDouble())
    abs(v) >= 1_000_000 -> "${formatFixed((v / 1_000_000).toDouble(), 2)}M"
    abs(v) >= 1_000     -> "${formatFixed((v / 1_000).toDouble(), 1)}k"
    else                -> formatFixed((v).toDouble(), 2)
}
