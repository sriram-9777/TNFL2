package com.tnfl2.v2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeIsDark = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = Dark_Primary,
    secondary = TNFL2_Secondary,
    tertiary = TNFL2_Accent,
    background = Dark_Background_Start,
    surface = Dark_Surface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Dark_TextPrimary,
    onSurface = Dark_TextPrimary,
    onSurfaceVariant = Dark_TextPrimary.copy(alpha = 0.7f)
)

private val LightColorScheme = lightColorScheme(
    primary = TNFL2_Primary,
    secondary = TNFL2_Secondary,
    tertiary = TNFL2_Accent,
    background = Premium_Background_Start,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = Color.Gray
)

@Composable
fun V2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use our custom color schemes so the manual dark-mode toggle works
    // on ALL Android versions including 12+ (no dynamic/wallpaper colors).
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}