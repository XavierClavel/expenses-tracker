package com.xavierclavel.bankable.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The app ships a single dark theme; Material You is deliberately not applied so
// the palette stays consistent across devices.
private val NightColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = OnMint,
    primaryContainer = MintContainer,
    onPrimaryContainer = Mint,
    secondary = TransferBlue,
    onSecondary = OnMint,
    secondaryContainer = TransferBlueContainer,
    onSecondaryContainer = TransferBlue,
    tertiary = Mint,
    onTertiary = OnMint,
    background = NightBackground,
    onBackground = NightTextPrimary,
    surface = NightBackground,
    onSurface = NightTextPrimary,
    surfaceVariant = NightCardRaised,
    onSurfaceVariant = NightTextSecondary,
    surfaceTint = NightBackground,
    surfaceBright = NightCardRaised,
    surfaceDim = NightBackground,
    surfaceContainerLowest = NightBackground,
    surfaceContainerLow = NightCard,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightCard,
    surfaceContainerHighest = NightCardRaised,
    inverseSurface = NightTextPrimary,
    inverseOnSurface = NightBackground,
    inversePrimary = OnMint,
    error = Danger,
    onError = OnMint,
    errorContainer = DangerContainer,
    onErrorContainer = Danger,
    outline = NightBorder,
    outlineVariant = NightDivider,
    scrim = NightBackground,
)

private val NightShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NightColorScheme,
        typography = Typography,
        shapes = NightShapes,
        content = content
    )
}
