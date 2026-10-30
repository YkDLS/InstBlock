package com.example.instagramblocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val AppColors = lightColorScheme(
    primary = Color(0xFF6555D9),
    secondary = Color(0xFF00A896),
    tertiary = Color(0xFFFF8A65),
    background = Color(0xFFF7F5FF),
    surface = Color.White,
)

@Composable
fun InstBlockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}
