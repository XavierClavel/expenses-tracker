package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.constants.colorByName
import com.xavierclavel.bankable.model.AccountOut
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

data class AccountSliceEntry(
    val id: Int,
    val label: String,
    val value: Float,
    val color: Color,
)

// Balances are assets, so lead with neutral/positive hues (greens, teals, blues)
// and push red-family hues — which read as "loss" — to the end. Labels reference
// the shared palette in constants/AppColors.kt.
private val distributionPalette: List<Color> = listOf(
    "green", "teal", "blue", "cyan", "lightgreen", "lime", "lightblue",
    "indigo", "purple", "violet", "navy", "amber", "yellow", "beige",
    "brown", "gray", "magenta", "pink", "orange", "coral", "red", "crimson",
).mapNotNull { colorByName(it)?.color }

@Composable
fun AccountDistributionScreen(viewModel: AccountsViewModel) {
    val accounts by viewModel.accounts.collectAsState()

    // Only positive balances can be represented as slices of a whole.
    val entries = remember(accounts) {
        accounts
            .mapNotNull { account ->
                val v = account.amount.toFloatOrNull() ?: 0f
                if (v > 0f) account to v else null
            }
            .sortedByDescending { it.second }
            .mapIndexed { index, (account, v) ->
                AccountSliceEntry(
                    id    = account.id,
                    label = account.name,
                    value = v,
                    color = distributionPalette[index % distributionPalette.size],
                )
            }
    }

    var selectedSlice by remember(entries) { mutableIntStateOf(-1) }

    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.account_distribution_no_data),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val total = entries.sumOf { it.value.toDouble() }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        DistributionDonut(
            entries       = entries,
            total         = total,
            selectedIndex = selectedSlice,
            onSliceClick  = { i -> selectedSlice = if (selectedSlice == i) -1 else i },
            modifier      = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(16.dp))

        entries.forEachIndexed { index, entry ->
            AccountLegendRow(
                entry      = entry,
                total      = total,
                isSelected = index == selectedSlice,
                onSelect   = { selectedSlice = if (selectedSlice == index) -1 else index },
            )
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DistributionDonut(
    entries: List<AccountSliceEntry>,
    total: Float,
    selectedIndex: Int,
    onSliceClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anySelected   = selectedIndex != -1
    val selectedEntry = entries.getOrNull(selectedIndex)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(entries, total) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val dx = tap.x - cx;      val dy = tap.y - cy
                    val dist    = sqrt(dx * dx + dy * dy)
                    val outerR  = minOf(size.width, size.height) / 2f
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
                    color      = entry.color.copy(alpha = alpha),
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
        // Centre label: selected slice → its value/name, otherwise total.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(90.dp)) {
            if (selectedEntry != null) {
                Text(formatAmount(selectedEntry.value.toDouble()),
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, color = selectedEntry.color)
                Text(selectedEntry.label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Text(formatAmount(total.toDouble()),
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(R.string.label_total), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AccountLegendRow(
    entry: AccountSliceEntry,
    total: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val pct        = if (total > 0f) entry.value / total * 100f else 0f
    val fraction   = (entry.value / total).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier       = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape          = MaterialTheme.shapes.medium,
        color          = if (isSelected) entry.color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 0.dp else 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Canvas(Modifier.size(14.dp)) {
                    drawRoundRect(color = entry.color, cornerRadius = CornerRadius(3.dp.toPx()))
                }
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
                Text(formatAmount(entry.value.toDouble()), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = entry.color, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
            }
            Canvas(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp).height(4.dp)) {
                val r = CornerRadius(2.dp.toPx())
                drawRoundRect(color = trackColor, cornerRadius = r)
                if (fraction > 0f) drawRoundRect(color = entry.color, size = Size(size.width * fraction, size.height), cornerRadius = r)
            }
        }
    }
}
