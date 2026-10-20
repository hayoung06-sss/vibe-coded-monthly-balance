package com.balance.monthly.domain

import java.time.LocalDate
import java.time.YearMonth

/** 달 시작일부터 다음 시작일 전날까지. 양 끝을 포함한다. */
data class Period(val start: LocalDate, val end: LocalDate) {
    val dayCount: Int get() = (end.toEpochDay() - start.toEpochDay() + 1).toInt()

    operator fun contains(date: LocalDate): Boolean = date in start..end
}

/** [date]가 속한 기간. 시작일이 그 달에 없으면(예: 31일) 그 달의 마지막 날부터 시작한다. */
fun periodOf(date: LocalDate, startDay: Int): Period {
    require(startDay in 1..31) { "startDay must be in 1..31: $startDay" }
    val month = YearMonth.from(date)
    val startMonth = if (date >= actualStart(month, startDay)) month else month.minusMonths(1)
    return Period(
        start = actualStart(startMonth, startDay),
        end = actualStart(startMonth.plusMonths(1), startDay).minusDays(1),
    )
}

fun Period.previous(startDay: Int): Period = periodOf(start.minusDays(1), startDay)

fun Period.next(startDay: Int): Period = periodOf(end.plusDays(1), startDay)

private fun actualStart(month: YearMonth, startDay: Int): LocalDate =
    month.atDay(minOf(startDay, month.lengthOfMonth()))
