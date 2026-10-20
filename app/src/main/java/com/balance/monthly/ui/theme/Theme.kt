package com.balance.monthly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.balance.monthly.R
import com.balance.monthly.data.ThemeMode

// res/values/colors.xml의 window_background_*와 같은 값
val WindowBackgroundDark = Color(0xFF121212)
val WindowBackgroundLight = Color(0xFFFAFAFA)
val Mint = Color(0xFF4FD1A5)

/** 밝은 배경에서 대비가 충분하도록 어둡게 한 민트 */
private val MintDeep = Color(0xFF14866A)
private val OnMint = Color(0xFF00382A)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = OnMint,
    primaryContainer = Mint,
    onPrimaryContainer = OnMint,
    secondary = Mint,
    onSecondary = OnMint,
    secondaryContainer = Color(0xFF1F3D33),
    onSecondaryContainer = Mint,
    // 지출 표시에도 쓴다
    error = Color(0xFFFF8A80),
    background = WindowBackgroundDark,
    onBackground = Color(0xFFE6E6E6),
    surface = WindowBackgroundDark,
    onSurface = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF9E9E9E),
    surfaceContainer = Color(0xFF1A1A1A),
    outline = Color(0xFF5A5A5A),
    outlineVariant = Color(0xFF2E2E2E),
)

private val LightColors = lightColorScheme(
    primary = MintDeep,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = OnMint,
    secondary = MintDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF3E6),
    onSecondaryContainer = OnMint,
    error = Color(0xFFC62828),
    background = WindowBackgroundLight,
    onBackground = Color(0xFF1C1C1C),
    surface = WindowBackgroundLight,
    onSurface = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFF616161),
    surfaceContainer = Color(0xFFF0F0F0),
    outline = Color(0xFFA0A0A0),
    outlineVariant = Color(0xFFE0E0E0),
)

/** 금액 자릿수가 바뀌어도 숫자 폭이 흔들리지 않게 한다. */
val TabularNumbers = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

/** 각 테마의 창 배경 스타일(res/values/themes.xml). */
val ThemeMode.windowTheme: Int
    get() = when (this) {
        ThemeMode.DARK -> R.style.Theme_MonthlyBalance
        ThemeMode.LIGHT -> R.style.Theme_MonthlyBalance_Light
        ThemeMode.SYSTEM -> R.style.Theme_MonthlyBalance_System
    }

@Composable
fun MonthlyBalanceTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
