package com.balance.monthly.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.balance.monthly.BalanceApp
import com.balance.monthly.R
import com.balance.monthly.ui.home.HomeScreen
import com.balance.monthly.ui.home.HomeViewModel
import com.balance.monthly.ui.records.RecordsScreen
import com.balance.monthly.ui.settings.SettingsScreen

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    HOME("홈", R.drawable.ic_home),
    RECORDS("기록", R.drawable.ic_list),
    SETTINGS("설정", R.drawable.ic_settings),
}

/** 하단 3탭. Navigation 라이브러리 없이 상태 변수 하나로 전환한다. */
@Composable
fun AppRoot() {
    val app = LocalContext.current.applicationContext as BalanceApp
    val homeViewModel = viewModel { HomeViewModel({ app.repository }, app.settings) }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (tab) {
            // 입력 바텀시트는 5단계에서 연결한다
            Tab.HOME -> HomeScreen(homeViewModel, onAddBalance = {}, modifier)
            Tab.RECORDS -> RecordsScreen(modifier)
            Tab.SETTINGS -> SettingsScreen(modifier)
        }
    }
}
