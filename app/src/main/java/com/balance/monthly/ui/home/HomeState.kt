package com.balance.monthly.ui.home

import com.balance.monthly.data.CachedForecast
import com.balance.monthly.data.HomeSnapshot
import com.balance.monthly.data.matches
import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.EntryWithBalance
import com.balance.monthly.domain.Period
import com.balance.monthly.domain.PeriodSummary
import com.balance.monthly.domain.balanceAt
import com.balance.monthly.domain.dailyBalances
import com.balance.monthly.domain.periodAt
import com.balance.monthly.domain.periodSummary
import com.balance.monthly.domain.periodRemaining
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
    val period = periodAt(today, startDay, offset)
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
        chart = BalanceChartData(
            period.start, period.dayCount, balances, markers, todayIndex, firstIndex,
            remaining = if (timeline.isEmpty()) null else periodRemaining(summary),
        ),
        isEmpty = timeline.isEmpty(),
    )
}

/** 이번 기간을 실제 데이터로 계산한 상태를 스냅샷으로 만든다. 빈 상태나 지난 기간이면 null. */
fun HomeUiState.toSnapshot(dataVersion: Long, today: LocalDate, startDay: Int): HomeSnapshot? {
    if (!isCurrentPeriod || isEmpty || balance == null || summary == null) return null
    return HomeSnapshot(
        dataVersion = dataVersion,
        writtenOn = today,
        monthStartDay = startDay,
        balance = balance,
        period = period,
        dailyBalances = chart.balances,
        firstIndex = chart.firstIndex,
        markers = chart.markers,
        summary = summary,
    )
}

/**
 * 스냅샷으로 첫 프레임 상태를 만든다. 스냅샷의 기간이 오늘이 속한 기간과 다르면 null.
 * 이전 기간으로 가는 버튼은 실제 데이터를 읽은 뒤에 켠다.
 */
fun HomeSnapshot.toUiState(today: LocalDate, startDay: Int): HomeUiState? {
    if (monthStartDay != startDay || period != periodAt(today, startDay, 0)) return null
    val todayIndex = (today.toEpochDay() - period.start.toEpochDay()).toInt()
    return HomeUiState(
        period = period,
        periodLabel = periodLabel(period, startDay),
        isCurrentPeriod = true,
        canGoBack = false,
        canGoForward = false,
        balance = balance,
        summary = summary,
        chart = BalanceChartData(
            period.start, period.dayCount, dailyBalances, markers, todayIndex, firstIndex,
            remaining = periodRemaining(summary),
        ),
        isEmpty = false,
    )
}

/**
 * 이번 기간이고 캐시 키가 현재 상태와 맞을 때만 예측선을 붙인다. 맞지 않으면 이전 예측도 보여주지 않는다.
 * 실제 잔고 선이 오늘까지 그려져 있을 때만 이어 그린다.
 */
fun HomeUiState.withForecast(forecast: CachedForecast?, dataVersion: Long, today: LocalDate, startDay: Int): HomeUiState {
    val todayIndex = chart.todayIndex ?: return this
    if (forecast == null || isEmpty || !forecast.matches(dataVersion, today, startDay)) return this
    if (chart.balances.size != todayIndex + 1 || forecast.balances.size != chart.dayCount - todayIndex) return this
    return copy(chart = chart.withForecast(forecast.balances))
}
