package com.balance.monthly.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemainingTest {

    private val today = day("2026-10-20")
    private val october = periodOf(today, 1)

    @Test
    fun `첫 기간은 시작 잔고 차액과 수입이 기준이 된다`() {
        // 시작 잔고 500,000 + 수입 200,000 = 700,000, 현재 625,000
        val remaining = periodRemaining(periodSummary(planExample().timeline(), october, today))!!

        assertEquals(700_000L, remaining.base)
        assertEquals(625_000L, remaining.balance)
        assertEquals(89, remaining.percent) // 625,000 / 700,000 = 89.3%
    }

    @Test
    fun `이전 기간에서 넘어온 잔고에 이번 기간 수입을 더한다`() {
        val timeline = Ledger().apply {
            balance("2026-09-10", 1_000_000)
            income("2026-10-05", 2_000_000)
            expense("2026-10-10", 1_200_000)
        }.timeline()

        val remaining = periodRemaining(periodSummary(timeline, october, today))!!

        assertEquals(3_000_000L, remaining.base)
        assertEquals(60, remaining.percent)
    }

    @Test
    fun `기준선 잔고는 위에서부터 75, 50, 25퍼센트`() {
        val remaining = PeriodRemaining(base = 3_000_000, balance = 0)

        assertEquals(listOf(75, 50, 25), REMAINING_GUIDE_PERCENTS)
        assertEquals(2_250_000L, remaining.balanceAtPercent(75))
        assertEquals(1_500_000L, remaining.balanceAtPercent(50))
        assertEquals(750_000L, remaining.balanceAtPercent(25))
        assertEquals(0, remaining.percent)
    }

    @Test
    fun `정확히 기준선이면 잔량 선과 같은 잔고`() {
        val remaining = PeriodRemaining(base = 1_000_000, balance = 500_000)

        assertEquals(50, remaining.percent)
        assertEquals(remaining.balance, remaining.balanceAtPercent(50))
    }

    @Test
    fun `미기록 수입이나 마이너스 잔고면 100 초과나 0 미만`() {
        assertEquals(120, PeriodRemaining(base = 1_000_000, balance = 1_200_000).percent)
        assertEquals(-30, PeriodRemaining(base = 1_000_000, balance = -300_000).percent)
    }

    @Test
    fun `쓸 수 있었던 돈이 0 이하면 잔량이 없다`() {
        assertNull(periodRemaining(PeriodSummary(0, -5_000, 0, 5_000, 0, 0)))
        assertNull(periodRemaining(PeriodSummary(-100_000, -100_000, 50_000, 50_000, 0, 0)))
    }
}
