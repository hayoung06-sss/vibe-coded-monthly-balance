package com.balance.monthly.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class InterpolatedBalancesTest {

    private val today = day("2026-10-20")
    private val october = periodOf(today, 1)

    @Test
    fun `기록한 날 사이는 직선으로 줄어든다`() {
        val timeline = Ledger().apply {
            balance("2026-10-01", 1_000_000)
            balance("2026-10-11", 900_000)
        }.timeline()

        val line = interpolatedDailyBalances(timeline, october, day("2026-10-11"))

        assertArrayEquals(LongArray(11) { 1_000_000L - it * 10_000 }, line)
    }

    @Test
    fun `기록한 날은 실제 잔고를 지난다`() {
        val timeline = planExample().timeline()
        val actual = dailyBalances(timeline, october, today)
        val line = interpolatedDailyBalances(timeline, october, today)

        for (recorded in listOf(1, 3, 10, 15, 20)) {
            assertEquals("10/$recorded", actual[recorded - 1], line[recorded - 1])
        }
        // 10/1 500,000 → 10/3 470,000 사이
        assertEquals(485_000L, line[1])
        // 10/3 470,000 → 10/10 670,000 사이 (수입도 천천히 오른다)
        assertEquals(470_000L + Math.round(200_000.0 * 3 / 7), line[5])
    }

    @Test
    fun `마지막 기록 이후 오늘까지는 수평`() {
        val timeline = Ledger().apply {
            balance("2026-10-01", 1_000_000)
            expense("2026-10-05", 100_000)
        }.timeline()

        val line = interpolatedDailyBalances(timeline, october, today)

        assertEquals(20, line.size)
        assertEquals(900_000L, line[4])
        assertEquals(900_000L, line[19])
    }

    @Test
    fun `기간 밖 기록과도 이어져 기간 첫날이 직선 위에 놓인다`() {
        val timeline = Ledger().apply {
            balance("2026-09-21", 1_000_000)
            balance("2026-10-11", 800_000)
        }.timeline()

        val line = interpolatedDailyBalances(timeline, october, today)

        // 9/21 → 10/11 20일 동안 200,000 → 하루 10,000. 10/1은 10일째
        assertEquals(900_000L, line[0])
        assertEquals(800_000L, line[10])
    }

    @Test
    fun `같은 날 여러 기록은 그날 마지막 잔고로 잇는다`() {
        val timeline = Ledger().apply {
            balance("2026-10-01", 100_000)
            expense("2026-10-03", 10_000)
            income("2026-10-03", 50_000)
        }.timeline()

        val line = interpolatedDailyBalances(timeline, october, today)

        assertEquals(140_000L, line[2])
        assertEquals(120_000L, line[1])
    }

    @Test
    fun `미래 날짜 기록은 선에 쓰지 않는다`() {
        val timeline = Ledger().apply {
            balance("2026-10-01", 1_000_000)
            expense("2026-10-25", 500_000)
        }.timeline()

        val line = interpolatedDailyBalances(timeline, october, today)

        assertEquals(1_000_000L, line[19])
    }

    @Test
    fun `첫 기록 이전은 0이고 길이는 일별 잔고와 같다`() {
        val timeline = Ledger().apply { balance("2026-10-05", 300_000) }.timeline()

        val line = interpolatedDailyBalances(timeline, october, today)

        assertEquals(dailyBalances(timeline, october, today).size, line.size)
        assertEquals(0L, line[3])
        assertEquals(300_000L, line[4])
        assertEquals(0, interpolatedDailyBalances(timeline, october.next(1), today).size)
    }
}
