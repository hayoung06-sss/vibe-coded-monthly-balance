package com.balance.monthly.ui.records

import com.balance.monthly.domain.Ledger
import com.balance.monthly.domain.day
import com.balance.monthly.domain.planExample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsStateTest {

    private val today = day("2026-10-20")

    @Test
    fun `기간 안의 기록을 최신순으로 각 시점 잔고와 함께`() {
        val state = recordsState(planExample().timeline(), today, startDay = 1, offset = 0)

        assertEquals(listOf(625_000L, 640_000L, 670_000L, 470_000L, 500_000L), state.items!!.map { it.balance })
        assertEquals(day("2026-10-20"), state.items.first().entry.date)
    }

    @Test
    fun `읽는 중이면 목록이 null`() {
        assertNull(recordsState(null, today, startDay = 1, offset = 0).items)
    }

    @Test
    fun `기간 경계를 넘는 기록은 빠진다`() {
        val state = recordsState(planExample().timeline(), today, startDay = 15, offset = 0)

        // 10/15 ~ 11/14
        assertEquals(listOf(day("2026-10-20"), day("2026-10-15")), state.items!!.map { it.entry.date })
        assertTrue(state.canGoBack)
    }

    @Test
    fun `미래 날짜 기록이 있으면 그 기간까지 앞으로 갈 수 있다`() {
        val timeline = planExample().apply { expense("2026-11-03", 10_000) }.timeline()

        val current = recordsState(timeline, today, startDay = 1, offset = 0)
        assertTrue(current.canGoForward)

        val next = recordsState(timeline, today, startDay = 1, offset = 1)
        assertEquals(1, next.items!!.size)
        assertFalse(next.canGoForward)
    }

    @Test
    fun `과거 기록을 고치면 이후 잔고와 차액이 바뀐다`() {
        val ledger = planExample()
        val before = recordsState(ledger.timeline(), today, startDay = 1, offset = 0).items!!

        // 10/3 지출 30,000 → 50,000
        val expense = ledger.entries.single { it.date == day("2026-10-03") }
        ledger.remove(expense)
        ledger.entries += expense.copy(amount = 50_000)
        val after = recordsState(ledger.timeline(), today, startDay = 1, offset = 0).items!!

        val checkpointBefore = before.single { it.entry.date == day("2026-10-15") }
        val checkpointAfter = after.single { it.entry.date == day("2026-10-15") }
        assertEquals(-30_000L, checkpointBefore.diff)
        assertEquals(-10_000L, checkpointAfter.diff)
        assertEquals(650_000L, after.single { it.entry.date == day("2026-10-10") }.balance)
        assertEquals(625_000L, after.first().balance)
    }

    @Test
    fun `기록이 없는 기간은 빈 목록`() {
        val timeline = Ledger().apply { balance("2026-08-10", 1_000) }.timeline()

        assertEquals(0, recordsState(timeline, today, startDay = 1, offset = -1).items!!.size)
    }
}
