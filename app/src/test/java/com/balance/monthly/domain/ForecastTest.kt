package com.balance.monthly.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ForecastTest {

    private val today = day("2026-10-20")
    private val october = periodOf(today, 1)

    /** [from]부터 어제까지 매일 [amount]씩 쓴 기록장 */
    private fun dailySpending(from: String, amount: Long, opening: Long = 1_000_000) = Ledger().apply {
        balance(from, opening)
        var date = LocalDate.parse(from)
        while (date < today) {
            expense(date.toString(), amount)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `기록이 14일 미만이면 예측하지 않는다`() {
        assertNull(forecast(dailySpending("2026-10-07", 10_000).timeline(), today, october))
        assertNotNull(forecast(dailySpending("2026-10-06", 10_000).timeline(), today, october))
    }

    @Test
    fun `기록이 없거나 오늘이 기간 밖이면 null`() {
        assertNull(forecast(emptyList(), today, october))
        assertNull(forecast(dailySpending("2026-08-01", 10_000).timeline(), today, october.previous(1)))
    }

    @Test
    fun `매일 같은 금액을 쓰면 그 기울기로 기간 끝까지 내려간다`() {
        val timeline = dailySpending("2026-09-01", 10_000).timeline()
        val now = balanceAt(timeline, today)

        val result = forecast(timeline, today, october)!!

        assertEquals(12, result.size) // 10/20 ~ 10/31
        assertEquals(now, result[0])
        assertEquals(now - 110_000, result[11])
    }

    @Test
    fun `수입은 예측하지 않고 시작 잔고는 지출로 보지 않는다`() {
        val ledger = dailySpending("2026-09-01", 10_000, opening = 5_000_000).apply {
            income("2026-10-10", 3_000_000)
        }

        val result = forecast(ledger.timeline(), today, october)!!

        assertEquals(10_000L, result[0] - result[1])
    }

    @Test
    fun `미기록 지출(음수 차액)은 그날 지출에 더한다`() {
        val ledger = Ledger().apply {
            balance("2026-09-21", 1_000_000)
            // 29일 동안 기록 없이 290,000원이 빠짐 → 확인한 날 하루에 몰림
            balance("2026-10-19", 710_000)
        }

        val result = forecast(ledger.timeline(), today, october)!!

        // 29일 중 상위 5%(1일)를 빼므로 몰린 하루는 제외되고 나머지는 0
        assertEquals(0L, result[0] - result[1])

        val longer = Ledger().apply {
            balance("2026-07-22", 1_000_000)
            balance("2026-08-01", 990_000)
            balance("2026-08-11", 980_000)
            balance("2026-08-21", 970_000)
            balance("2026-08-31", 960_000)
            balance("2026-09-10", 950_000)
            balance("2026-09-20", 940_000)
            balance("2026-09-30", 930_000)
            balance("2026-10-10", 920_000)
        }
        // 90일 중 지출이 있는 날 8일(각 10,000) 중 상위 4일 제외 → 4 × 10,000 / 86
        val slope = forecast(longer.timeline(), today, october)!!.let { it[0] - it[11] }
        assertEquals(Math.round(40_000.0 / 86 * 11), slope)
    }

    @Test
    fun `큰 일회성 지출 하루는 상위 5퍼센트로 빠져 기울기가 크게 변하지 않는다`() {
        val normal = dailySpending("2026-08-01", 10_000).timeline()
        val withSpike = dailySpending("2026-08-01", 10_000).apply { expense("2026-10-01", 2_000_000) }.timeline()

        val normalSlope = forecast(normal, today, october)!!.let { it[0] - it[1] }
        val spikeSlope = forecast(withSpike, today, october)!!.let { it[0] - it[1] }

        assertEquals(10_000L, normalSlope)
        assertEquals(10_000L, spikeSlope)
    }

    @Test
    fun `최근 90일만 본다`() {
        // 오래전에는 많이 썼지만 최근 90일은 하루 5,000원
        val ledger = Ledger().apply {
            balance("2026-01-01", 50_000_000)
            var date = LocalDate.parse("2026-01-01")
            while (date < today) {
                expense(date.toString(), if (date < today.minusDays(90)) 100_000 else 5_000)
                date = date.plusDays(1)
            }
        }

        val result = forecast(ledger.timeline(), today, october)!!

        assertEquals(5_000L, result[0] - result[1])
    }

    @Test
    fun `미래 날짜 기록은 오늘 잔고에도 기울기에도 반영되지 않는다`() {
        val base = dailySpending("2026-09-01", 10_000)
        val expected = forecast(base.timeline(), today, october)!!
        base.expense("2026-10-25", 500_000)

        assertArrayEquals(expected, forecast(base.timeline(), today, october))
    }

    @Test
    fun `오늘이 기간 마지막 날이면 점 하나`() {
        val last = day("2026-10-31")
        val result = forecast(dailySpending("2026-09-01", 10_000).timeline(), last, october)

        assertTrue(result != null && result.size == 1)
    }
}
