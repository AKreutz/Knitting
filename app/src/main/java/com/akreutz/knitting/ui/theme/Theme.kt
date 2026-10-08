package com.akreutz.knitting.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The app is light-only: no dark scheme and no dynamic (wallpaper) color.
private val KnittingColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Cream,
    primaryContainer = TerracottaLight,
    onPrimaryContainer = TerracottaDark,
    secondary = Sage,
    onSecondary = Cream,
    secondaryContainer = SageLight,
    onSecondaryContainer = SageDark,
    background = Cream,
    onBackground = Espresso,
    surface = Cream,
    onSurface = Espresso,
    surfaceVariant = Oatmeal,
    onSurfaceVariant = Taupe,
    surfaceContainerLowest = Cream,
    surfaceContainerLow = OatmealLight,
    surfaceContainer = OatmealDark,
    surfaceContainerHigh = OatmealDark,
    surfaceContainerHighest = Oatmeal,
    outline = TaupeOutline,
    outlineVariant = TaupeOutlineVariant,
)

// Minimal corners: 4dp chips, 8dp cards, 12dp FAB.
private val KnittingShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
)

@Composable
fun KnittingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KnittingColorScheme,
        typography = Typography,
        shapes = KnittingShapes,
        content = content
    )
}
