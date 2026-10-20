package com.balance.monthly.domain

import java.time.LocalDate

/**
 * 기록을 적용한 직후의 잔고.
 * diff는 BALANCE 기록에서만 의미가 있다(입력 금액 − 직전 계산 잔고). 다른 종류는 0.
 * isOpening은 타임라인의 첫 BALANCE 기록이며, 그 차액은 '시작 잔고'로 따로 다룬다.
 */
data class EntryWithBalance(
    val entry: LedgerEntry,
    val balance: Long,
    val diff: Long = 0,
    val isOpening: Boolean = false,
)

/**
 * 한 기간(오늘 이후는 제외)의 집계.
 * 항상 startBalance + opening + income − expense + diff = endBalance가 성립한다.
 * opening은 시작 잔고 차액으로, 수입·지출 통계와 diff에서 빠진다.
 */
data class PeriodSummary(
    val startBalance: Long,
    val endBalance: Long,
    val income: Long,
    val expense: Long,
    val diff: Long,
    val opening: Long,
)

private val timelineOrder = compareBy<LedgerEntry>({ it.date }, { it.createdAt }, { it.id })

/** 기록을 (날짜, 생성 시각) 순으로 적용해 각 기록 직후의 잔고와 차액을 구한다. */
fun computeTimeline(entries: List<LedgerEntry>): List<EntryWithBalance> {
    var balance = 0L
    var openingSeen = false
    return entries.sortedWith(timelineOrder).map { entry ->
        when (entry.type) {
            EntryType.INCOME -> {
                balance += entry.amount
                EntryWithBalance(entry, balance)
            }
            EntryType.EXPENSE -> {
                balance -= entry.amount
                EntryWithBalance(entry, balance)
            }
            EntryType.BALANCE -> {
                val diff = entry.amount - balance
                balance = entry.amount
                EntryWithBalance(entry, balance, diff, isOpening = !openingSeen).also { openingSeen = true }
            }
        }
    }
}

/** [date]가 끝났을 때의 잔고. 그 전에 기록이 없으면 0. */
fun balanceAt(timeline: List<EntryWithBalance>, date: LocalDate): Long =
    timeline.lastOrNull { it.entry.date <= date }?.balance ?: 0L

/** 기간 첫날부터 오늘(기간이 이미 끝났으면 마지막 날)까지의 일별 잔고. 기간이 아직 시작 전이면 빈 배열. */
fun dailyBalances(timeline: List<EntryWithBalance>, period: Period, today: LocalDate): LongArray {
    val firstDay = period.start.toEpochDay()
    val lastDay = minOf(period.end, today).toEpochDay()
    if (lastDay < firstDay) return LongArray(0)

    val result = LongArray((lastDay - firstDay + 1).toInt())
    var index = 0
    var balance = 0L
    for (i in result.indices) {
        val day = firstDay + i
        while (index < timeline.size && timeline[index].entry.date.toEpochDay() <= day) {
            balance = timeline[index].balance
            index++
        }
        result[i] = balance
    }
    return result
}

/** 기간 시작부터 오늘(기간이 이미 끝났으면 마지막 날)까지의 수입·지출·차액 합계. */
fun periodSummary(timeline: List<EntryWithBalance>, period: Period, today: LocalDate): PeriodSummary {
    val startBalance = balanceAt(timeline, period.start.minusDays(1))
    val lastDay = minOf(period.end, today)
    if (lastDay < period.start) return PeriodSummary(startBalance, startBalance, 0, 0, 0, 0)

    var income = 0L
    var expense = 0L
    var diff = 0L
    var opening = 0L
    for (item in timeline) {
        val date = item.entry.date
        if (date < period.start || date > lastDay) continue
        when (item.entry.type) {
            EntryType.INCOME -> income += item.entry.amount
            EntryType.EXPENSE -> expense += item.entry.amount
            EntryType.BALANCE -> if (item.isOpening) opening += item.diff else diff += item.diff
        }
    }
    return PeriodSummary(startBalance, balanceAt(timeline, lastDay), income, expense, diff, opening)
}
