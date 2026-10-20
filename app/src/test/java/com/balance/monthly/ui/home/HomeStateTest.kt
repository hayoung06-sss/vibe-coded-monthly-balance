package com.balance.monthly.ui.home

import com.balance.monthly.domain.Ledger
import com.balance.monthly.domain.Period
import com.balance.monthly.domain.day
import com.balance.monthly.domain.planExample
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {

    private val today = day("2026-10-20")

    @Test
    fun `읽는 중이면 축과 오늘 표시만 있는 빈 그래프`() {
        val state = homeState(null, today, startDay = 1, offset = 0)

        assertEquals(Period(day("2026-10-01"), day("2026-10-31")), state.period)
        assertNull(state.balance)
        assertFalse(state.isEmpty)
        assertEquals(0, state.chart.balances.size)
        assertEquals(31, state.chart.dayCount)
        assertEquals(19, state.chart.todayIndex)
    }

    @Test
    fun `기록이 없으면 빈 상태`() {
        val state = homeState(emptyList(), today, startDay = 1, offset = 0)

        assertTrue(state.isEmpty)
        assertEquals(0L, state.balance)
        assertFalse(state.canGoBack)
        assertFalse(state.canGoForward)
    }

    @Test
    fun `이번 기간은 오늘까지 그리고 실제 잔고 날짜에 마커`() {
        val state = homeState(planExample().timeline(), today, startDay = 1, offset = 0)

        assertEquals("2026년 10월", state.periodLabel)
        assertEquals(625_000L, state.balance)
        assertEquals(20, state.chart.balances.size)
        assertEquals(31, state.chart.dayCount)
        assertEquals(19, state.chart.todayIndex)
        assertArrayEquals(intArrayOf(0, 14), state.chart.markers)
        assertEquals(-30_000L, state.summary!!.diff)
    }

    @Test
    fun `지난 기간은 전체를 그리고 오늘 표시가 없으며 기간 끝 잔고를 보여준다`() {
        val timeline = Ledger().apply {
            balance("2026-09-10", 100_000)
            expense("2026-09-20", 10_000)
            expense("2026-10-05", 5_000)
        }.timeline()

        val state = homeState(timeline, today, startDay = 1, offset = -1)

        assertEquals("2026년 9월", state.periodLabel)
        assertFalse(state.isCurrentPeriod)
        assertNull(state.chart.todayIndex)
        assertEquals(30, state.chart.balances.size)
        assertEquals(90_000L, state.balance)
        assertArrayEquals(intArrayOf(9), state.chart.markers)
        assertTrue(state.canGoForward)
    }

    @Test
    fun `첫 기록이 있는 기간보다 앞으로는 갈 수 없다`() {
        val timeline = Ledger().apply { balance("2026-09-10", 100_000) }.timeline()

        assertTrue(homeState(timeline, today, startDay = 1, offset = 0).canGoBack)
        assertFalse(homeState(timeline, today, startDay = 1, offset = -1).canGoBack)
    }

    @Test
    fun `미래 날짜의 실제 잔고는 마커로 그리지 않는다`() {
        val timeline = planExample().apply { balance("2026-10-25", 1_000) }.timeline()

        val state = homeState(timeline, today, startDay = 1, offset = 0)

        assertArrayEquals(intArrayOf(0, 14), state.chart.markers)
        assertEquals(625_000L, state.balance)
    }

    @Test
    fun `달 시작일 25이면 날짜 범위 라벨`() {
        val state = homeState(planExample().timeline(), today, startDay = 25, offset = 0)

        assertEquals("9/25 ~ 10/24", state.periodLabel)
        assertEquals(26, state.chart.balances.size)
        assertEquals(625_000L, state.balance)
    }
}
