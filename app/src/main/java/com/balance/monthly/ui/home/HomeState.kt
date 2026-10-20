package com.balance.monthly.ui.home

import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.EntryWithBalance
import com.balance.monthly.domain.Period
import com.balance.monthly.domain.PeriodSummary
import com.balance.monthly.domain.balanceAt
import com.balance.monthly.domain.dailyBalances
import com.balance.monthly.domain.periodOf
import com.balance.monthly.domain.periodSummary
import com.balance.monthly.domain.previous
import com.balance.monthly.ui.chart.BalanceChartData
import com.balance.monthly.ui.periodLabel
import java.time.LocalDate

data class HomeUiState(
    val period: Period,
    val periodLabel: String,
    val isCurrentPeriod: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    /** 이번 기간이면 오늘 잔고, 지난 기간이면 기간 끝 잔고. 기록을 아직 읽지 못했으면 null. */
    val balance: Long?,
    val summary: PeriodSummary?,
    val chart: BalanceChartData,
    val isEmpty: Boolean,
)

/**
 * 홈 화면 상태. [timeline]이 null이면 아직 기록을 읽는 중이며, 축과 오늘 표시만 있는 빈 그래프를 보여준다.
 * [offset]은 이번 기간을 0으로 한 기간 이동 수(0 이하).
 */
fun homeState(timeline: List<EntryWithBalance>?, today: LocalDate, startDay: Int, offset: Int): HomeUiState {
    var period = periodOf(today, startDay)
    repeat(-offset) { period = period.previous(startDay) }
    val isCurrent = offset == 0
    val todayIndex = if (isCurrent) (today.toEpochDay() - period.start.toEpochDay()).toInt() else null

    if (timeline == null) {
        return HomeUiState(
            period = period,
            periodLabel = periodLabel(period, startDay),
            isCurrentPeriod = isCurrent,
            canGoBack = false,
            canGoForward = !isCurrent,
            balance = null,
            summary = null,
            chart = BalanceChartData(period.start, period.dayCount, LongArray(0), IntArray(0), todayIndex),
            isEmpty = false,
        )
    }

    val balances = dailyBalances(timeline, period, today)
    val markers = timeline.asSequence()
        .filter { it.entry.type == EntryType.BALANCE }
        .map { (it.entry.date.toEpochDay() - period.start.toEpochDay()).toInt() }
        .filter { it in balances.indices }
        .distinct()
        .toList()
        .toIntArray()
    val summary = periodSummary(timeline, period, today)
    // 첫 기록 이전 날은 잔고가 0으로 계산되지만 그래프에서는 비워 둔다
    val firstIndex = timeline.firstOrNull()
        ?.let { (it.entry.date.toEpochDay() - period.start.toEpochDay()).toInt().coerceIn(0, balances.size) }
        ?: balances.size

    return HomeUiState(
        period = period,
        periodLabel = periodLabel(period, startDay),
        isCurrentPeriod = isCurrent,
        canGoBack = timeline.isNotEmpty() && timeline.first().entry.date < period.start,
        canGoForward = !isCurrent,
        balance = if (isCurrent) balanceAt(timeline, today) else summary.endBalance,
        summary = summary,
        chart = BalanceChartData(period.start, period.dayCount, balances, markers, todayIndex, firstIndex),
        isEmpty = timeline.isEmpty(),
    )
}
