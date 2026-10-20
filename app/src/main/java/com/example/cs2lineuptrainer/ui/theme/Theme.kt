package com.example.cs2lineuptrainer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    background = AppBackground,
    onBackground = Color.White,
    surface = AppSurface,
    onSurface = Color.White,
)

// Единая тёмная тема приложения.
@Composable
fun CS2LineupTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
