package com.balance.monthly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// res/values/colors.xml의 window_background_dark와 같은 값
val WindowBackgroundDark = Color(0xFF121212)

private val DarkColors = darkColorScheme(
    background = WindowBackgroundDark,
    surface = WindowBackgroundDark,
)

@Composable
fun MonthlyBalanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
