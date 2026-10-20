package com.balance.monthly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

// res/values/colors.xml의 window_background_dark와 같은 값
val WindowBackgroundDark = Color(0xFF121212)
val Mint = Color(0xFF4FD1A5)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF00382A),
    secondary = Mint,
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF1F3D33),
    onSecondaryContainer = Mint,
    background = WindowBackgroundDark,
    onBackground = Color(0xFFE6E6E6),
    surface = WindowBackgroundDark,
    onSurface = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF9E9E9E),
    surfaceContainer = Color(0xFF1A1A1A),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF2E2E2E),
)

/** 금액 자릿수가 바뀌어도 숫자 폭이 흔들리지 않게 한다. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun MonthlyBalanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
