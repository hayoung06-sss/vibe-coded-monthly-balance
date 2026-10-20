package com.balance.monthly.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodTest {

    @Test
    fun `시작일 1이면 달력의 한 달`() {
        assertEquals(Period(day("2026-10-01"), day("2026-10-31")), periodOf(day("2026-10-20"), 1))
        assertEquals(Period(day("2026-02-01"), day("2026-02-28")), periodOf(day("2026-02-01"), 1))
    }

    @Test
    fun `시작일 25이면 25일부터 다음 달 24일까지`() {
        assertEquals(Period(day("2026-09-25"), day("2026-10-24")), periodOf(day("2026-10-20"), 25))
        assertEquals(Period(day("2026-09-25"), day("2026-10-24")), periodOf(day("2026-10-24"), 25))
        assertEquals(Period(day("2026-10-25"), day("2026-11-24")), periodOf(day("2026-10-25"), 25))
    }

    @Test
    fun `시작일 31이면 2월은 28일, 4월은 30일에 시작`() {
        assertEquals(Period(day("2026-01-31"), day("2026-02-27")), periodOf(day("2026-02-27"), 31))
        assertEquals(Period(day("2026-02-28"), day("2026-03-30")), periodOf(day("2026-02-28"), 31))
        assertEquals(Period(day("2026-03-31"), day("2026-04-29")), periodOf(day("2026-04-29"), 31))
        assertEquals(Period(day("2026-04-30"), day("2026-05-30")), periodOf(day("2026-04-30"), 31))
    }

    @Test
    fun `윤년 2월은 29일에 시작`() {
        assertEquals(Period(day("2028-01-31"), day("2028-02-28")), periodOf(day("2028-02-28"), 31))
        assertEquals(Period(day("2028-02-29"), day("2028-03-30")), periodOf(day("2028-02-29"), 31))
        assertEquals(Period(day("2028-02-29"), day("2028-03-29")), periodOf(day("2028-03-01"), 30))
    }

    @Test
    fun `모든 시작일에서 연속 기간은 빈 날과 겹치는 날 없이 이어지고 모든 날짜는 한 기간에만 속한다`() {
        // 윤년(2028년 2월)을 포함한 2년
        val first = day("2027-06-01")
        val last = day("2029-06-30")
        for (startDay in 1..31) {
            var period = periodOf(first, startDay)
            assertTrue(first in period)
            while (period.start <= last) {
                assertTrue("startDay=$startDay $period", period.dayCount in 28..31)
                var date = period.start
                while (date <= period.end) {
                    assertEquals("startDay=$startDay date=$date", period, periodOf(date, startDay))
                    date = date.plusDays(1)
                }
                val next = period.next(startDay)
                assertEquals(period.end.plusDays(1), next.start)
                assertEquals(period, next.previous(startDay))
                period = next
            }
        }
    }

    @Test
    fun `contains는 양 끝을 포함한다`() {
        val period = Period(day("2026-10-25"), day("2026-11-24"))
        assertTrue(day("2026-10-25") in period)
        assertTrue(day("2026-11-24") in period)
        assertFalse(day("2026-10-24") in period)
        assertFalse(day("2026-11-25") in period)
        assertEquals(31, period.dayCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `시작일 0은 거부한다`() {
        periodOf(day("2026-10-20"), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `시작일 32는 거부한다`() {
        periodOf(day("2026-10-20"), 32)
    }
}
