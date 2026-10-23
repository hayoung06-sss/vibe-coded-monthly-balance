package com.balance.monthly.ui.home

import com.balance.monthly.data.CachedForecast
import com.balance.monthly.domain.FORECAST_ALGORITHM_VERSION
import com.balance.monthly.domain.Ledger
import com.balance.monthly.domain.Period
import com.balance.monthly.domain.PeriodRemaining
import com.balance.monthly.domain.day
import com.balance.monthly.domain.planExample
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    fun `첫 기록 이전 날은 그리지 않는다`() {
        val timeline = Ledger().apply { balance("2026-10-12", 300_000) }.timeline()

        val state = homeState(timeline, today, startDay = 1, offset = 0)

        assertEquals(11, state.chart.firstIndex)
        assertEquals(11..19, state.chart.drawnIndices)
        assertEquals(0L, state.chart.balances[10])
    }

    @Test
    fun `첫 기록이 이전 기간이면 기간 전체를 그린다`() {
        val state = homeState(planExample().timeline(), day("2026-11-03"), startDay = 1, offset = 0)

        assertEquals(0, state.chart.firstIndex)
        assertEquals(0..2, state.chart.drawnIndices)
    }

    @Test
    fun `첫 기록이 미래 날짜뿐이면 아무 선도 없다`() {
        val timeline = Ledger().apply { balance("2026-10-25", 300_000) }.timeline()

        val state = homeState(timeline, today, startDay = 1, offset = 0)

        assertTrue(state.chart.drawnIndices.isEmpty())
    }

    @Test
    fun `스냅샷으로 만든 첫 화면은 실제 데이터로 그린 화면과 같다`() {
        val fresh = homeState(planExample().timeline(), today, startDay = 1, offset = 0)
        val snapshot = fresh.toSnapshot(dataVersion = 5, today, startDay = 1)!!

        val restored = snapshot.toUiState(today, startDay = 1)!!

        assertEquals(fresh.balance, restored.balance)
        assertEquals(fresh.summary, restored.summary)
        assertEquals(fresh.periodLabel, restored.periodLabel)
        assertArrayEquals(fresh.chart.balances, restored.chart.balances)
        assertArrayEquals(fresh.chart.markers, restored.chart.markers)
        assertEquals(fresh.chart.todayIndex, restored.chart.todayIndex)
        assertEquals(fresh.chart.firstIndex, restored.chart.firstIndex)
    }

    @Test
    fun `스냅샷의 기간이나 시작일이 다르면 쓰지 않는다`() {
        val snapshot = homeState(planExample().timeline(), today, startDay = 1, offset = 0)
            .toSnapshot(dataVersion = 5, today, startDay = 1)!!

        assertNull(snapshot.toUiState(day("2026-11-01"), startDay = 1))
        assertNull(snapshot.toUiState(today, startDay = 25))
        // 같은 기간의 다음 날은 그대로 쓴다
        assertEquals(20, snapshot.toUiState(day("2026-10-21"), startDay = 1)!!.chart.todayIndex)
    }

    @Test
    fun `지난 기간과 빈 상태는 스냅샷으로 저장하지 않는다`() {
        val past = homeState(planExample().timeline(), today, startDay = 1, offset = -1)
        val empty = homeState(emptyList(), today, startDay = 1, offset = 0)

        assertNull(past.toSnapshot(1, today, 1))
        assertNull(empty.toSnapshot(1, today, 1))
    }

    private fun cachedForecast(dataVersion: Long = 3, on: LocalDate = today, startDay: Int = 1, size: Int = 12) =
        CachedForecast(dataVersion, on, startDay, FORECAST_ALGORITHM_VERSION, LongArray(size) { 625_000L - it * 10_000 })

    @Test
    fun `캐시 키가 맞으면 오늘부터 기간 끝까지 예측선을 붙인다`() {
        val state = homeState(planExample().timeline(), today, startDay = 1, offset = 0)

        val withForecast = state.withForecast(cachedForecast(), dataVersion = 3, today, startDay = 1)

        assertArrayEquals(cachedForecast().balances, withForecast.chart.forecast)
    }

    @Test
    fun `키가 하나라도 다르면 예측선을 숨긴다`() {
        val state = homeState(planExample().timeline(), today, startDay = 1, offset = 0)

        // 기록이 바뀜
        assertNull(state.withForecast(cachedForecast(dataVersion = 2), 3, today, 1).chart.forecast)
        // 자정이 지남
        assertNull(state.withForecast(cachedForecast(on = day("2026-10-19")), 3, today, 1).chart.forecast)
        // 달 시작일이 바뀜
        assertNull(state.withForecast(cachedForecast(startDay = 25), 3, today, 1).chart.forecast)
        // 길이가 맞지 않음
        assertNull(state.withForecast(cachedForecast(size = 5), 3, today, 1).chart.forecast)
        assertNull(state.withForecast(null, 3, today, 1).chart.forecast)
    }

    @Test
    fun `지난 기간과 빈 상태, 아직 선이 오늘까지 없을 때는 예측선이 없다`() {
        val past = homeState(planExample().timeline(), today, startDay = 1, offset = -1)
        val empty = homeState(emptyList(), today, startDay = 1, offset = 0)
        val loading = homeState(null, today, startDay = 1, offset = 0)

        assertNull(past.withForecast(cachedForecast(), 3, today, 1).chart.forecast)
        assertNull(empty.withForecast(cachedForecast(), 3, today, 1).chart.forecast)
        assertNull(loading.withForecast(cachedForecast(), 3, today, 1).chart.forecast)
    }

    @Test
    fun `그래프에 이번 기간 잔량을 넘긴다`() {
        val state = homeState(planExample().timeline(), today, startDay = 1, offset = 0)

        assertEquals(PeriodRemaining(base = 700_000, balance = 625_000), state.chart.remaining)
    }

    @Test
    fun `지난 기간 잔량은 기간 끝 잔고 기준`() {
        val timeline = Ledger().apply {
            balance("2026-08-20", 1_000_000)
            expense("2026-09-15", 250_000)
            expense("2026-10-05", 100_000)
        }.timeline()

        val state = homeState(timeline, today, startDay = 1, offset = -1)

        val remaining = state.chart.remaining!!
        assertEquals(75, remaining.percent)
        assertEquals(remaining.balance, remaining.balanceAtPercent(75))
    }

    @Test
    fun `빈 상태와 읽는 중에는 잔량이 없고 스냅샷에서는 복원된다`() {
        assertNull(homeState(emptyList(), today, startDay = 1, offset = 0).chart.remaining)
        assertNull(homeState(null, today, startDay = 1, offset = 0).chart.remaining)

        val fresh = homeState(planExample().timeline(), today, startDay = 1, offset = 0)
        val restored = fresh.toSnapshot(5, today, 1)!!.toUiState(today, 1)!!
        assertEquals(fresh.chart.remaining, restored.chart.remaining)
    }

    @Test
    fun `예측을 붙여도 잔량은 그대로`() {
        val state = homeState(planExample().timeline(), today, startDay = 1, offset = 0)

        assertEquals(state.chart.remaining, state.withForecast(cachedForecast(), 3, today, 1).chart.remaining)
    }

    @Test
    fun `달 시작일 25이면 날짜 범위 라벨`() {
        val state = homeState(planExample().timeline(), today, startDay = 25, offset = 0)

        assertEquals("9/25 ~ 10/24", state.periodLabel)
        assertEquals(26, state.chart.balances.size)
        assertEquals(625_000L, state.balance)
    }
}
