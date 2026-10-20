package com.balance.monthly.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.balance.monthly.data.Settings
import com.balance.monthly.data.ThemeMode
import com.balance.monthly.domain.periodOf
import com.balance.monthly.ui.shortDate
import java.time.LocalDate

private val themeLabels = listOf(ThemeMode.DARK to "다크", ThemeMode.LIGHT to "라이트", ThemeMode.SYSTEM to "시스템")

/** 값을 바꾸면 즉시 저장·반영한다. 따로 저장 버튼은 없다. */
@Composable
fun SettingsScreen(settings: Settings, modifier: Modifier = Modifier) {
    val startDay by settings.monthStartDayFlow.collectAsStateWithLifecycle()
    val theme by settings.themeFlow.collectAsStateWithLifecycle()
    val showForecast by settings.showForecastFlow.collectAsStateWithLifecycle()
    var selecting by remember(startDay) { mutableIntStateOf(startDay) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SectionTitle("달 시작일")
        Text(
            "월급날처럼 한 달이 시작되는 날을 고르세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayWheel(
                selected = startDay,
                onSelecting = { selecting = it },
                onSelected = { if (it != settings.monthStartDay) settings.monthStartDay = it },
                modifier = Modifier.width(120.dp),
            )
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                val period = remember(selecting) { periodOf(LocalDate.now(), selecting) }
                Text("현재 기간", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${shortDate(period.start)} ~ ${shortDate(period.end)}", style = MaterialTheme.typography.titleLarge)
                if (selecting >= 29) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "짧은 달은 마지막 날부터 시작",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Divider()
        SectionTitle("테마")
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            themeLabels.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = theme == mode,
                    onClick = { settings.theme = mode },
                    shape = SegmentedButtonDefaults.itemShape(index, themeLabels.size),
                    label = { Text(label) },
                )
            }
        }

        Divider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionTitle("잔고 예측 표시")
                Text(
                    "최근 지출 추이로 남은 날의 잔고를 흐린 선으로 보여줍니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = showForecast, onCheckedChange = { settings.showForecast = it })
        }

        Divider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("앱 버전", style = MaterialTheme.typography.bodyLarge)
            Text(appVersion(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
}
