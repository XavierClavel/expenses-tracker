package com.xavierclavel.bankable.accounts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.constants.AccountType
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.account_distribution_no_data
import com.xavierclavel.bankable.resources.label_total
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ShareBar
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.util.formatFixed
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt
import org.jetbrains.compose.resources.stringResource

data class AccountEntry(
    val id: Int,
    val label: String,
    val value: Float,
)

data class TypeSliceEntry(
    val type: AccountType,
    val value: Float,
    val color: Color,
    val accounts: List<AccountEntry>,
)

@Composable
fun AccountDistributionScreen(viewModel: AccountsViewModel) {
    val accounts by viewModel.accounts.collectAsState()

    // Only positive balances can be represented as slices of a whole. Accounts are
    // grouped under their type (one donut slice per type, largest first).
    val entries = remember(accounts) {
        val positive = accounts.mapNotNull { account ->
            val v = account.amount.toFloatOrNull() ?: 0f
            if (v > 0f) account to v else null
        }
        AccountType.entries.mapNotNull { type ->
            val typeAccounts = positive
                .filter { AccountType.fromKey(it.first.type) == type }
                .sortedByDescending { it.second }
            if (typeAccounts.isEmpty()) return@mapNotNull null
            TypeSliceEntry(
                type     = type,
                value    = typeAccounts.sumOf { it.second.toDouble() }.toFloat(),
                color    = type.accentColor,
                accounts = typeAccounts.map { (account, v) ->
                    AccountEntry(id = account.id, label = account.name, value = v)
                },
            )
        }.sortedByDescending { it.value }
    }

    // The largest group starts highlighted; tapping it again shows the total.
    var selectedSlice by remember(entries) { mutableIntStateOf(if (entries.isEmpty()) -1 else 0) }
    var expandedTypes by remember(entries) { mutableStateOf(entries.map { it.type.key }.toSet()) }

    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.account_distribution_no_data),
                color = NightTextSecondary,
            )
        }
        return
    }

    val total = entries.sumOf { it.value.toDouble() }.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.height(14.dp))

        DistributionDonut(
            entries       = entries,
            total         = total,
            selectedIndex = selectedSlice,
            onSliceClick  = { i -> selectedSlice = if (selectedSlice == i) -1 else i },
            modifier      = Modifier.size(210.dp).align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(8.dp))

        entries.forEachIndexed { index, entry ->
            TypeGroupCard(
                entry      = entry,
                total      = total,
                isSelected = index == selectedSlice,
                isExpanded = expandedTypes.contains(entry.type.key),
                onSelect   = { selectedSlice = if (selectedSlice == index) -1 else index },
                onToggleExpand = {
                    expandedTypes =
                        if (expandedTypes.contains(entry.type.key)) expandedTypes - entry.type.key
                        else expandedTypes + entry.type.key
                },
            )
        }

        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun DistributionDonut(
    entries: List<TypeSliceEntry>,
    total: Float,
    selectedIndex: Int,
    onSliceClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedEntry = entries.getOrNull(selectedIndex)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(entries, total) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val dx = tap.x - cx;      val dy = tap.y - cy
                    val dist    = sqrt(dx * dx + dy * dy)
                    val outerR  = minOf(size.width, size.height) / 2f
                    val strokeW = outerR * 0.26f
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
            val strokeBase = diameter / 2f * 0.22f
            // No gaps for a single slice: it is the whole ring.
            val gapDeg     = if (entries.size > 1) 3f else 0f
            var startAngle = -90f
            entries.forEachIndexed { i, entry ->
                val fraction   = entry.value / total
                val sweepAngle = 360f * fraction - gapDeg
                val stroke = if (i == selectedIndex) strokeBase * 1.18f else strokeBase
                // Every slice shares the same outer edge; a thicker stroke grows inward.
                val inset  = stroke / 2f
                drawArc(
                    color      = entry.color,
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
        // Centre label: selected group → its value/type, otherwise total.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(130.dp)) {
            Text(
                text = formatAmount((selectedEntry?.value ?: total).toDouble()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = selectedEntry?.color ?: NightTextPrimary,
                maxLines = 1,
            )
            Text(
                text = stringResource(selectedEntry?.type?.labelRes ?: Res.string.label_total).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = NightTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TypeGroupCard(
    entry: TypeSliceEntry,
    total: Float,
    isSelected: Boolean,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
) {
    val pct      = if (total > 0f) entry.value / total * 100f else 0f
    val fraction = (entry.value / total).coerceIn(0f, 1f)
    val color    = entry.color

    NightCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        selected = isSelected,
        accent = color,
        contentPadding = PaddingValues(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(entry.type.icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text       = stringResource(entry.type.labelRes),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color      = NightTextPrimary,
                modifier   = Modifier.weight(1f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                "${formatFixed(pct.toDouble(), 1)} %",
                style = MaterialTheme.typography.bodySmall,
                color = NightTextSecondary,
                textAlign = TextAlign.End,
            )
            Text(
                formatAmount(entry.value.toDouble()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.End,
                modifier = Modifier.width(92.dp),
                maxLines = 1,
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = NightTextTertiary,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleExpand)
                    .padding(3.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        ShareBar(fraction = fraction, color = color, height = 4.dp, modifier = Modifier.padding(end = 4.dp))

        if (isExpanded) {
            Spacer(Modifier.height(6.dp))
            entry.accounts.forEach { account ->
                AccountShareRow(account = account, total = total)
            }
        }
    }
}

@Composable
private fun AccountShareRow(account: AccountEntry, total: Float) {
    val pct = if (total > 0f) account.value / total * 100f else 0f
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 36.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text     = account.label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = NightTextPrimary.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${formatFixed(pct.toDouble(), 1)} %",
            style = MaterialTheme.typography.bodySmall,
            color = NightTextSecondary,
            textAlign = TextAlign.End,
        )
        Text(
            formatAmount(account.value.toDouble()),
            style = MaterialTheme.typography.bodyMedium,
            color = NightTextSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(92.dp),
            maxLines = 1,
        )
    }
}
