package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.model.ThemeMode

private val BtDarkColorScheme = darkColorScheme(
    primary = BtCrimsonBright,
    onPrimary = BtWhite,
    primaryContainer = ColorTokens.DarkPrimaryContainer,
    onPrimaryContainer = BtCrimsonSoft,
    secondary = BtCrimson,
    onSecondary = BtWhite,
    secondaryContainer = BtDarkSurfaceElevated,
    onSecondaryContainer = BtDarkTextPrimary,
    tertiary = BtGoldBadge,
    onTertiary = BtObsidian,
    background = BtObsidian,
    onBackground = BtDarkTextPrimary,
    surface = BtDarkSurface,
    onSurface = BtDarkTextPrimary,
    surfaceVariant = BtDarkSurfaceElevated,
    onSurfaceVariant = BtDarkTextSecondary,
    surfaceContainerLowest = BtObsidian,
    surfaceContainerLow = BtDarkSurface,
    surfaceContainer = BtDarkSurfaceElevated,
    surfaceContainerHigh = ColorTokens.DarkSurfaceHigh,
    outline = BtDarkOutline,
    outlineVariant = BtDarkOutline
)

private val BtLightColorScheme = lightColorScheme(
    primary = BtCrimson,
    onPrimary = BtWhite,
    primaryContainer = BtCrimsonSoft,
    onPrimaryContainer = BtCrimsonDark,
    secondary = BtCrimsonDark,
    onSecondary = BtWhite,
    secondaryContainer = BtLightSurfaceVariant,
    onSecondaryContainer = BtLightTextPrimary,
    tertiary = BtGoldBadge,
    onTertiary = BtLightTextPrimary,
    background = BtLightBackground,
    onBackground = BtLightTextPrimary,
    surface = BtLightSurface,
    onSurface = BtLightTextPrimary,
    surfaceVariant = BtLightSurfaceVariant,
    onSurfaceVariant = BtLightTextSecondary,
    surfaceContainerLowest = BtWhite,
    surfaceContainerLow = BtLightBackground,
    surfaceContainer = BtLightSurfaceVariant,
    surfaceContainerHigh = ColorTokens.LightSurfaceHigh,
    outline = BtLightOutline,
    outlineVariant = BtLightOutline
)

private object ColorTokens {
    val DarkPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF4B0C14)
    val DarkSurfaceHigh = androidx.compose.ui.graphics.Color(0xFF262632)
    val LightSurfaceHigh = androidx.compose.ui.graphics.Color(0xFFE9E9F0)
}

val BtTubeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BtTubeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) BtDarkColorScheme else BtLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BtTubeTypography,
        shapes = BtTubeShapes,
        content = content
    )
}
