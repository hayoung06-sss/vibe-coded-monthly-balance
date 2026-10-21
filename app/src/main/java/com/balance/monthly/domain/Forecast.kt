package com.balance.monthly.domain

import java.time.LocalDate

/** 계산 방법을 바꾸면 올린다. 캐시 키에 들어가 이전 결과를 무효로 만든다. */
const val FORECAST_ALGORITHM_VERSION = 1

private const val WINDOW_DAYS = 90L
private const val MIN_HISTORY_DAYS = 14
private const val TRIM_RATIO = 0.05

/**
 * 오늘부터 기간 마지막 날까지의 예측 잔고. [0]은 오늘 잔고다.
 *
 * 최근 90일(기록이 그보다 짧으면 첫 기록부터 어제까지)의 날짜별 지출에 미기록 지출(음수 차액)을 더하고,
 * 지출이 가장 큰 상위 5% 날을 뺀 일평균 지출 r로 `오늘 잔고 − r × 경과 일수`를 구한다. 수입은 예측하지 않는다.
 * 기록이 14일 미만이거나 오늘이 기간 밖이면 null.
 */
fun forecast(timeline: List<EntryWithBalance>, today: LocalDate, period: Period): LongArray? {
    if (today !in period || timeline.isEmpty()) return null
    val windowStart = maxOf(timeline.first().entry.date, today.minusDays(WINDOW_DAYS))
    val days = (today.toEpochDay() - windowStart.toEpochDay()).toInt()
    if (days < MIN_HISTORY_DAYS) return null

    val spending = LongArray(days)
    for (item in timeline) {
        val index = (item.entry.date.toEpochDay() - windowStart.toEpochDay()).toInt()
        if (index !in spending.indices) continue
        when (item.entry.type) {
            EntryType.EXPENSE -> spending[index] += item.entry.amount
            EntryType.BALANCE -> if (!item.isOpening && item.diff < 0) spending[index] -= item.diff
            EntryType.INCOME -> Unit
        }
    }
    val kept = spending.sorted().dropLast((days * TRIM_RATIO).toInt())
    val dailyRate = kept.sum().toDouble() / kept.size

    val start = balanceAt(timeline, today)
    val count = (period.end.toEpochDay() - today.toEpochDay() + 1).toInt()
    return LongArray(count) { day -> start - Math.round(dailyRate * day) }
}
