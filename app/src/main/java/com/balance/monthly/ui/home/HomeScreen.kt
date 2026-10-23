package com.balance.monthly.ui.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.balance.monthly.R
import com.balance.monthly.domain.PeriodSummary
import com.balance.monthly.ui.chart.BalanceChart
import com.balance.monthly.ui.formatSigned
import com.balance.monthly.ui.formatWon
import com.balance.monthly.ui.shortDate
import com.balance.monthly.ui.theme.TabularNumbers

@Composable
fun HomeScreen(viewModel: HomeViewModel, onAddBalance: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    // 실제 데이터로 그려지면 reportFullyDrawn (TTFD)
    ReportDrawnWhen { loaded }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    HomeContent(state, viewModel::showPrevious, viewModel::showNext, onAddBalance, modifier)
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAddBalance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = state.canGoBack) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "이전 기간")
            }
            Text(
                text = state.periodLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext, enabled = state.canGoForward) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "다음 기간")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            // 지난 기간은 그 기간 마지막 날의 잔고. 위에 기간이 표시되므로 '잔고'로 충분하다.
            text = if (state.isCurrentPeriod) "현재 잔고" else "잔고",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = state.balance?.let(::formatWon) ?: "",
            style = MaterialTheme.typography.displaySmall.merge(TabularNumbers),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))

        if (state.isEmpty) {
            EmptyState(onAddBalance, Modifier.weight(1f).fillMaxWidth())
        } else {
            BalanceChart(state.chart, Modifier.weight(1f).fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                AxisLabel(shortDate(state.period.start))
                Spacer(Modifier.weight(1f))
                AxisLabel(shortDate(state.period.end))
            }
            Spacer(Modifier.height(20.dp))
            state.summary?.let { SummaryRow(it) }
        }
        // + 버튼이 요약을 가리지 않도록 그 높이만큼 비운다
        Spacer(Modifier.height(FabClearance))
    }
}

private val FabClearance = 88.dp

@Composable
private fun AxisLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EmptyState(onAddBalance: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "현재 잔고를 입력해 시작하세요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddBalance) { Text("잔고 입력") }
    }
}

@Composable
private fun SummaryRow(summary: PeriodSummary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SummaryItem("수입", formatSigned(summary.income))
        SummaryItem("지출", formatSigned(-summary.expense))
        SummaryItem("차액", formatSigned(summary.diff))
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers))
    }
}
