package com.xavierclavel.bankable.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightCardRaised
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary

/**
 * A segmented control that slides a raised pill between options.
 *
 * @param options list of (value, display label) pairs
 * @param selected the currently selected value
 * @param onSelect called when the user taps a different option
 * @param selectedColor text color of the selected option
 * @param pillColor background of the sliding pill
 */
@Composable
fun SlidingToggle(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    selectedColor: Color = NightTextPrimary,
    pillColor: Color = NightCardRaised,
) {
    val count = options.size.coerceAtLeast(1)
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val outerShape = RoundedCornerShape(18.dp)
    val innerShape = RoundedCornerShape(13.dp)
    val inset = 5.dp

    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(outerShape)
            .background(NightCard)
            .border(1.dp, NightBorder, outerShape)
            .padding(inset),
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
                .clip(innerShape)
                .background(pillColor),
        )

        // Labels row
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { i, (value, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(innerShape)
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Medium,
                        color      = if (i == selectedIndex) selectedColor else NightTextSecondary,
                    )
                }
            }
        }
    }
}
