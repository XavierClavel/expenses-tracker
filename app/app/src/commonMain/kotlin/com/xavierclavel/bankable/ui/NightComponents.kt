package com.xavierclavel.bankable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xavierclavel.bankable.ui.theme.Danger
import com.xavierclavel.bankable.ui.theme.DangerBorder
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.MintBorder
import com.xavierclavel.bankable.ui.theme.MintContainer
import com.xavierclavel.bankable.ui.theme.NightBorder
import com.xavierclavel.bankable.ui.theme.NightCard
import com.xavierclavel.bankable.ui.theme.NightCardRaised
import com.xavierclavel.bankable.ui.theme.NightDivider
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import com.xavierclavel.bankable.ui.theme.NightTextTertiary
import com.xavierclavel.bankable.ui.theme.NightTrack
import com.xavierclavel.bankable.ui.theme.OnMint

// Building blocks for the "Night desk" design. Screens compose these rather than
// raw Material surfaces so cards, buttons and headers stay consistent.

/** Horizontal gutter shared by every screen. */
val ScreenGutter: Dp = 12.dp

val CardShape: Shape = RoundedCornerShape(18.dp)

/** Amount color for income/expense rows: mint for income, primary text otherwise. */
fun amountColor(isIncome: Boolean): Color = if (isIncome) Mint else NightTextPrimary

/**
 * The basic slate card with a hairline border. [selected] switches it to the
 * mint-tinted "active" look used for selection and the highlighted item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NightCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    accent: Color = Mint,
    shape: Shape = CardShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = if (selected) accent.copy(alpha = 0.08f).compositeOver(NightCard) else NightCard
    val border = if (selected) accent.copy(alpha = 0.55f) else NightBorder
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(BorderStroke(1.dp, border), shape)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
                } else Modifier
            )
            .padding(contentPadding),
        content = content,
    )
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}

/** A rounded square holding an icon tinted with [color] over a faint wash of it. */
@Composable
fun IconTile(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(color.copy(alpha = 0.16f).compositeOver(NightCard)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = color, modifier = Modifier.size(iconSize))
    }
}

/** Round, bordered icon button used for back/close and header actions. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = NightTextPrimary,
    borderColor: Color = NightBorder,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(NightCard)
            .border(1.dp, borderColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

/**
 * Screen title row: optional leading circle button, a heavy title with an
 * optional subtitle, and trailing actions.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigate: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    large: Boolean = navigationIcon == null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter)
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null && onNavigate != null) {
            CircleIconButton(navigationIcon, navigationContentDescription, onNavigate)
            Spacer(Modifier.width(14.dp))
        }
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                color = NightTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NightTextSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}

/** Uppercase, tracked label for list sections ("TODAY · SAT 31 OCT", "LANGUAGE"). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingColor: Color = NightTextSecondary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = NightTextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.bodyMedium, color = trailingColor)
        }
    }
}

/** Big mint call-to-action with a soft glow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.shadow(18.dp, shape, ambientColor = Mint, spotColor = Mint)
                else Modifier
            )
            .clip(shape)
            .background(if (enabled) Mint else Mint.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 58.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = OnMint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnMint)
    }
}

/** Outlined secondary button ("Continue with Google"). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NightCard)
            .border(1.dp, NightBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = NightTextPrimary)
    }
}

/** Pink-outlined destructive button ("Delete category"). */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, DangerBorder, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Danger)
    }
}

/** Mint floating action button with a glow. */
@Composable
fun NightFab(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(20.dp, CircleShape, ambientColor = Mint, spotColor = Mint)
            .clip(CircleShape)
            .background(Mint)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = OnMint, modifier = Modifier.size(28.dp))
    }
}

/** Rounded pill used for filter options ("In / Out", "This year", "Month"). */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val shape = CircleShape
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) MintContainer else NightCard)
            .border(1.dp, if (selected) MintBorder else NightBorder, shape)
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (selected) Mint else NightTextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Mint else NightTextSecondary,
        )
    }
}

/** Small tinted tag pill ("Weekly shop"), and its dashed-looking "+ Add" sibling. */
@Composable
fun TagPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MintContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Mint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Mint)
    }
}

@Composable
fun AddPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, NightTextTertiary.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = NightTextSecondary)
    }
}

/**
 * A form row inside a card: a muted label on the left, the value [content] in the
 * middle and an optional chevron. Tapping the row calls [onClick].
 */
@Composable
fun FormRow(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    trailing: (@Composable () -> Unit)? = null,
    labelWidth: Dp = 110.dp,
    content: @Composable RowScope.() -> Unit,
) {
    NightCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextSecondary,
                modifier = Modifier.widthIn(min = labelWidth).width(labelWidth),
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
            if (trailing != null) trailing()
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NightTextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Underlined tab strip ("Reports · Transfers · Charts", "Balance · Distribution · Charts"). */
@Composable
fun UnderlineTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                Column(
                    modifier = (if (fillWidth) Modifier.weight(1f) else Modifier)
                        .clickable { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) NightTextPrimary else NightTextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                    Box(
                        modifier = Modifier
                            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(80.dp))
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (selected) Mint else Color.Transparent),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(NightDivider))
    }
}

/** Thin rounded progress track with a colored fill (category share bars). */
@Composable
fun ShareBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(NightTrack),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(height)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Radio indicator: an empty ring, or a mint ring with a mint dot. */
@Composable
fun RadioDot(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .border(2.dp, if (selected) Mint else NightTextTertiary.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(Mint))
        }
    }
}

/** Multi-select indicator: an empty ring, or a filled mint disc with a check. */
@Composable
fun CheckDot(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    if (checked) {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(Mint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = OnMint, modifier = Modifier.size(size * 0.65f))
        }
    } else {
        Box(modifier = modifier.size(size).border(2.dp, NightTextTertiary.copy(alpha = 0.6f), CircleShape))
    }
}

/** Hairline divider used between rows inside a grouped card. */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(NightDivider))
}

/** Grouped card with no inner padding, for stacked rows separated by [CardDivider]. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    NightCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
        content = content,
    )
}

/** Small stat tile ("TOTAL 21 457 €"). */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = NightTextPrimary) {
    NightCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = NightTextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Text input inside a card. With [stackedLabel] the label sits above the value
 * in small caps (auth screens); otherwise it is a [FormRow]-style inline label.
 */
@Composable
fun NightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    stackedLabel: Boolean = false,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = CardShape
    val borderColor = if (isError) DangerBorder else NightBorder
    val field: @Composable (Modifier) -> Unit = { fieldModifier ->
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            textStyle = textStyle.merge(TextStyle(color = NightTextPrimary)),
            cursorBrush = SolidColor(Mint),
            modifier = fieldModifier,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = textStyle, color = NightTextTertiary)
                    }
                    inner()
                }
            },
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NightCard)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 18.dp, vertical = if (stackedLabel) 14.dp else 0.dp)
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (stackedLabel) {
            Column(Modifier.weight(1f)) {
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isError) Danger else NightTextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                field(Modifier.fillMaxWidth())
            }
        } else {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = NightTextSecondary,
                modifier = Modifier.width(110.dp),
            )
            field(Modifier.weight(1f))
        }
        if (trailing != null) trailing()
    }
}

/** Height a chart reserves under its bars for a [ChartAxisLabel] of up to two lines. */
val ChartAxisLabelHeight = 40.dp

/** Period label under a chart bar ("Jan", "Jan '25", "2025"), shared so all charts match. */
@Composable
fun ChartAxisLabel(text: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.labelSmall,
        fontSize   = 11.sp,
        // The label style's wide tracking makes "Jan '25" overflow a bar's width.
        letterSpacing = 0.sp,
        lineHeight = 13.sp,
        color      = if (highlighted) NightTextPrimary else NightTextTertiary,
        fontWeight = if (highlighted) FontWeight.ExtraBold else FontWeight.Medium,
        textAlign  = TextAlign.Center,
        maxLines   = 2,
        modifier   = modifier.padding(top = 6.dp),
    )
}
