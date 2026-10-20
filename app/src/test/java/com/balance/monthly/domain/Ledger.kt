package com.balance.monthly.domain

import org.junit.Assert.assertEquals
import java.time.LocalDate

fun day(text: String): LocalDate = LocalDate.parse(text)

/** 입력 순서대로 createdAt이 커지는 테스트용 기록장. */
class Ledger {
    private var nextId = 1L
    val entries = mutableListOf<LedgerEntry>()

    fun income(date: String, amount: Long) = add(EntryType.INCOME, date, amount)
    fun expense(date: String, amount: Long) = add(EntryType.EXPENSE, date, amount)
    fun balance(date: String, amount: Long) = add(EntryType.BALANCE, date, amount)

    fun remove(entry: LedgerEntry) {
        entries.remove(entry)
    }

    fun timeline() = computeTimeline(entries)

    private fun add(type: EntryType, date: String, amount: Long): LedgerEntry {
        val id = nextId++
        return LedgerEntry(id, type, amount, day(date), createdAt = id * 1000).also { entries += it }
    }
}

/** 계획서의 예시 표: 10/1 실제 잔고 500,000 … 10/20 지출 15,000 → 625,000. */
fun planExample() = Ledger().apply {
    balance("2026-10-01", 500_000)
    expense("2026-10-03", 30_000)
    income("2026-10-10", 200_000)
    balance("2026-10-15", 640_000)
    expense("2026-10-20", 15_000)
}

/** 기간 시작 잔고 + 시작 잔고 차액 + 수입 − 지출 + 차액 합계 = 기간 끝 잔고 */
fun assertBalanced(summary: PeriodSummary) {
    with(summary) {
        assertEquals("집계 등식이 깨짐: $summary", endBalance, startBalance + opening + income - expense + diff)
    }
}
