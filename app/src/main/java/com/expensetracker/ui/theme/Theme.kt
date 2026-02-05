package com.expensetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFF66BB6A),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFF0F1115),
    surface = Color(0xFF171A21),
    onPrimary = Color(0xFF0B1E0E),
    onSecondary = Color(0xFF0B1E0E),
    onBackground = Color(0xFFE6E9EF),
    onSurface = Color(0xFFE6E9EF)
)

@Composable
fun ExpenseTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
