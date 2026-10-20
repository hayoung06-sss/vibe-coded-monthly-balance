package com.balance.monthly

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.balance.monthly.ui.AppRoot
import com.balance.monthly.ui.theme.MonthlyBalanceTheme
import com.balance.monthly.ui.theme.isDark
import com.balance.monthly.ui.theme.windowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val settings = (application as BalanceApp).settings
        // 테마는 첫 프레임에 필요하므로 동기로 읽어 창 배경부터 맞춘다
        setTheme(settings.theme.windowTheme)
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by settings.themeFlow.collectAsStateWithLifecycle()
            val dark = themeMode.isDark()
            MonthlyBalanceTheme(darkTheme = dark) {
                val background = MaterialTheme.colorScheme.background.toArgb()
                LaunchedEffect(themeMode, dark) {
                    val barStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                    window.setBackgroundDrawable(ColorDrawable(background))
                    // 다음 실행의 스플래시도 고른 테마의 배경으로 띄운다
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        splashScreen.setSplashScreenTheme(themeMode.windowTheme)
                    }
                }
                AppRoot()
            }
        }
    }
}
