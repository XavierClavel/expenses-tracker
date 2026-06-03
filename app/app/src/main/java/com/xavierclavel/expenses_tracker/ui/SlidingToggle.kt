package com.xavierclavel.expenses_tracker.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pill-shaped toggle that slides a filled indicator between options.
 *
 * @param options list of (value, display label) pairs
 * @param selected the currently selected value
 * @param onSelect called when the user taps a different option
 */
@Composable
fun SlidingToggle(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val count = options.size.coerceAtLeast(1)
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val cornerRadius = height / 2

    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val pillWidth = maxWidth / count
        val pillOffset by animateDpAsState(
            targetValue   = pillWidth * selectedIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label         = "sliding_toggle_pill",
        )

        // Sliding pill
        Box(
            modifier = Modifier
                .width(pillWidth)
                .fillMaxHeight()
                .offset(x = pillOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.primary),
        )

        // Labels row
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
                        fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (i == selectedIndex) MaterialTheme.colorScheme.onPrimary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
