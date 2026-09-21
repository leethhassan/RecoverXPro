package com.recoverx.pro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFFBFA7FF),
    onPrimary = Color(0xFF26145B),
    secondary = Color(0xFF9FE6D1),
    background = Color(0xFF0A0B10),
    surface = Color(0xFF101118),
    surfaceVariant = Color(0xFF1C1D26),
    onSurface = Color(0xFFF4F0FF)
)

private val Light = lightColorScheme(
    primary = Color(0xFF6240C8),
    onPrimary = Color.White,
    secondary = Color(0xFF176F5D),
    background = Color(0xFFF7F5FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDE8F5),
    onSurface = Color(0xFF17131E)
)

@Composable
fun RecoverTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
