package com.tnfl2.v2.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    val isDark = LocalThemeIsDark.current
    val startColor = if (isDark) Dark_Background_Start else Premium_Background_Start
    val endColor = if (isDark) Dark_Background_End else Premium_Background_End

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        startColor,
                        endColor
                    )
                )
            )
    ) {
        content()
    }
}