package com.balance.monthly.ui.records

import com.balance.monthly.domain.EntryWithBalance
import com.balance.monthly.domain.Period
import com.balance.monthly.domain.periodAt
import com.balance.monthly.ui.periodLabel
import java.time.LocalDate

data class RecordsUiState(
    val period: Period,
    val periodLabel: String,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    /** 기간 안의 기록, 최신순. 기록을 아직 읽지 못했으면 null. */
    val items: List<EntryWithBalance>?,
)

/**
 * 기록 페이지 상태. [offset]은 이번 기간을 0으로 한 기간 이동 수.
 * 미래 날짜 기록이 있으면 그 기간까지 앞으로 갈 수 있다.
 */
fun recordsState(timeline: List<EntryWithBalance>?, today: LocalDate, startDay: Int, offset: Int): RecordsUiState {
    val period = periodAt(today, startDay, offset)
    val label = periodLabel(period, startDay)
    if (timeline == null) return RecordsUiState(period, label, canGoBack = false, canGoForward = false, items = null)

    return RecordsUiState(
        period = period,
        periodLabel = label,
        canGoBack = timeline.isNotEmpty() && timeline.first().entry.date < period.start,
        canGoForward = offset < 0 || (timeline.isNotEmpty() && timeline.last().entry.date > period.end),
        items = timeline.filter { it.entry.date in period }.asReversed(),
    )
}
