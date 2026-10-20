package com.balance.monthly.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PriorBalanceTest {

    @Test
    fun `기록이 없으면 0이고 기준점도 없다`() {
        assertEquals(PriorBalance(0, hasCheckpoint = false), priorBalance(emptyList(), day("2026-10-20")))
    }

    @Test
    fun `새 기록은 같은 날짜의 기존 기록 뒤에 놓인다`() {
        val entries = planExample().entries

        assertEquals(PriorBalance(625_000, hasCheckpoint = true), priorBalance(entries, day("2026-10-20")))
        assertEquals(PriorBalance(470_000, hasCheckpoint = true), priorBalance(entries, day("2026-10-03")))
        assertEquals(PriorBalance(0, hasCheckpoint = false), priorBalance(entries, day("2026-09-30")))
    }

    @Test
    fun `수정 중인 기록은 빼고 원래 입력 순서 자리에서 계산한다`() {
        val ledger = Ledger()
        ledger.balance("2026-10-01", 100_000)
        ledger.expense("2026-10-05", 1_000)
        val checkpoint = ledger.balance("2026-10-05", 90_000)
        ledger.expense("2026-10-05", 2_000)

        // 같은 날 먼저 입력한 지출 1,000만 반영된 자리
        assertEquals(PriorBalance(99_000, hasCheckpoint = true), priorBalance(ledger.entries, day("2026-10-05"), checkpoint))
    }

    @Test
    fun `첫 실제 잔고를 수정할 때는 앞에 기준점이 없다`() {
        val ledger = Ledger()
        ledger.income("2026-10-01", 50_000)
        val opening = ledger.balance("2026-10-02", 300_000)

        assertEquals(PriorBalance(50_000, hasCheckpoint = false), priorBalance(ledger.entries, day("2026-10-02"), opening))
    }

    @Test
    fun `수정하며 날짜를 옮기면 새 날짜 기준으로 계산한다`() {
        val ledger = planExample()
        val checkpoint = ledger.entries.single { it.date == day("2026-10-15") }

        assertEquals(PriorBalance(500_000, hasCheckpoint = true), priorBalance(ledger.entries, day("2026-10-02"), checkpoint))
    }
}
