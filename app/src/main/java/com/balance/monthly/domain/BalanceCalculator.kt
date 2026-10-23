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

/** 기록이 들어갈 자리 직전의 계산 잔고와, 그 앞에 실제 잔고 기록이 있는지. */
data class PriorBalance(val balance: Long, val hasCheckpoint: Boolean)

/**
 * [date]에 기록을 넣을 때 그 직전의 계산 잔고. 실제 잔고 입력의 힌트와 예상 차액에 쓴다.
 * 새 기록은 같은 날짜의 기존 기록 뒤에 놓이고, 수정 중인 기록([editing])은 제외한 채 원래 생성 순서 자리에 놓인다.
 */
fun priorBalance(entries: List<LedgerEntry>, date: LocalDate, editing: LedgerEntry? = null): PriorBalance {
    val others = if (editing == null) entries else entries.filter { it.id != editing.id }
    val position = editing?.copy(date = date)
        ?: LedgerEntry(Long.MAX_VALUE, EntryType.BALANCE, 0, date, createdAt = Long.MAX_VALUE)
    val before = computeTimeline(others).filter { timelineOrder.compare(it.entry, position) < 0 }
    return PriorBalance(
        balance = before.lastOrNull()?.balance ?: 0L,
        hasCheckpoint = before.any { it.entry.type == EntryType.BALANCE },
    )
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

/**
 * 그래프 선용 일별 잔고. 기록이 있는 날(오늘까지)의 잔고를 직선으로 이어, 기록 사이에 돈이 천천히 바뀐 것처럼 그린다.
 * 기간 밖의 기록도 이어서 기간 경계에서 선이 끊기지 않는다. 마지막 기록 이후 오늘까지는 알 수 없으므로 수평이고,
 * 첫 기록 이전은 0이다. 잔고·합계 계산에는 쓰지 않는다.
 */
fun interpolatedDailyBalances(timeline: List<EntryWithBalance>, period: Period, today: LocalDate): LongArray {
    val firstDay = period.start.toEpochDay()
    val lastDay = minOf(period.end, today).toEpochDay()
    if (lastDay < firstDay) return LongArray(0)

    // 기록한 날과 그날이 끝났을 때의 잔고
    val anchorDays = ArrayList<Long>()
    val anchorBalances = ArrayList<Long>()
    for (item in timeline) {
        val day = item.entry.date.toEpochDay()
        if (day > today.toEpochDay()) break
        if (anchorDays.lastOrNull() == day) {
            anchorBalances[anchorBalances.lastIndex] = item.balance
        } else {
            anchorDays += day
            anchorBalances += item.balance
        }
    }

    val result = LongArray((lastDay - firstDay + 1).toInt())
    var k = -1 // day 이전(같은 날 포함)의 마지막 기록
    for (i in result.indices) {
        val day = firstDay + i
        while (k + 1 < anchorDays.size && anchorDays[k + 1] <= day) k++
        result[i] = when {
            k < 0 -> 0L
            anchorDays[k] == day || k == anchorDays.lastIndex -> anchorBalances[k]
            else -> {
                val fromDay = anchorDays[k]
                val toDay = anchorDays[k + 1]
                val from = anchorBalances[k]
                val to = anchorBalances[k + 1]
                from + Math.round((to - from).toDouble() * (day - fromDay) / (toDay - fromDay))
            }
        }
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
