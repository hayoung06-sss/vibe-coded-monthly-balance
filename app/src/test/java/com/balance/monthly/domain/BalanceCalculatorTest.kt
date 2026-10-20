package com.balance.monthly.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceCalculatorTest {

    private val today = day("2026-10-20")
    private val october = periodOf(today, 1)

    @Test
    fun `계획서 예시 표와 같은 잔고와 차액`() {
        val timeline = planExample().timeline()

        assertEquals(listOf(500_000L, 470_000L, 670_000L, 640_000L, 625_000L), timeline.map { it.balance })
        assertEquals(listOf(500_000L, 0L, 0L, -30_000L, 0L), timeline.map { it.diff })
        assertEquals(listOf(true, false, false, false, false), timeline.map { it.isOpening })

        val summary = periodSummary(timeline, october, today)
        assertEquals(PeriodSummary(startBalance = 0, endBalance = 625_000, income = 200_000, expense = 45_000, diff = -30_000, opening = 500_000), summary)
        assertBalanced(summary)
    }

    @Test
    fun `기록이 하나도 없으면 잔고 0`() {
        val timeline = computeTimeline(emptyList())

        assertTrue(timeline.isEmpty())
        assertEquals(0L, balanceAt(timeline, today))
        assertArrayEquals(LongArray(20), dailyBalances(timeline, october, today))
        val summary = periodSummary(timeline, october, today)
        assertEquals(PeriodSummary(0, 0, 0, 0, 0, 0), summary)
        assertBalanced(summary)
    }

    @Test
    fun `실제 잔고 없이 수입·지출만 있으면 0에서 계산하고 음수 잔고도 그대로`() {
        val timeline = Ledger().apply {
            expense("2026-10-02", 10_000)
            income("2026-10-05", 3_000)
        }.timeline()

        assertEquals(listOf(-10_000L, -7_000L), timeline.map { it.balance })
        assertEquals(-7_000L, balanceAt(timeline, today))
        val summary = periodSummary(timeline, october, today)
        assertEquals(0L, summary.opening)
        assertEquals(0L, summary.diff)
        assertBalanced(summary)
    }

    @Test
    fun `첫 실제 잔고의 차액은 시작 잔고로 분류되어 차액 합계에서 빠진다`() {
        val timeline = Ledger().apply {
            income("2026-10-01", 100_000)
            balance("2026-10-02", 300_000)
            balance("2026-10-10", 290_000)
        }.timeline()

        assertTrue(timeline[1].isOpening)
        assertEquals(200_000L, timeline[1].diff)
        assertFalse(timeline[2].isOpening)

        val summary = periodSummary(timeline, october, today)
        assertEquals(200_000L, summary.opening)
        assertEquals(-10_000L, summary.diff)
        assertEquals(100_000L, summary.income)
        assertBalanced(summary)
    }

    @Test
    fun `같은 날 지출 → 실제 잔고 → 지출은 입력 순서대로 적용`() {
        val ledger = Ledger().apply {
            balance("2026-10-01", 100_000)
            expense("2026-10-05", 1_000)
            balance("2026-10-05", 90_000)
            expense("2026-10-05", 2_000)
        }
        // 저장소가 어떤 순서로 돌려줘도 결과는 같아야 한다
        val timeline = computeTimeline(ledger.entries.reversed())

        assertEquals(listOf(100_000L, 99_000L, 90_000L, 88_000L), timeline.map { it.balance })
        assertEquals(-9_000L, timeline[2].diff)
        assertBalanced(periodSummary(timeline, october, today))
    }

    @Test
    fun `실제 잔고 이전 날짜로 지출을 나중에 추가하면 그 차액이 줄고 이후 잔고는 그대로`() {
        val ledger = planExample()
        val before = ledger.timeline()

        ledger.expense("2026-10-12", 20_000)
        val after = ledger.timeline()

        val checkpointBefore = before.single { it.entry.date == day("2026-10-15") }
        val checkpointAfter = after.single { it.entry.date == day("2026-10-15") }
        assertEquals(-30_000L, checkpointBefore.diff)
        assertEquals(-10_000L, checkpointAfter.diff)
        assertEquals(checkpointBefore.balance, checkpointAfter.balance)
        assertEquals(balanceAt(before, today), balanceAt(after, today))
        assertBalanced(periodSummary(after, october, today))
    }

    @Test
    fun `실제 잔고 기록을 지우면 이전 기준점부터 다시 계산`() {
        val ledger = planExample()
        ledger.remove(ledger.entries.single { it.date == day("2026-10-15") })
        val timeline = ledger.timeline()

        assertEquals(655_000L, balanceAt(timeline, today))
        val summary = periodSummary(timeline, october, today)
        assertEquals(0L, summary.diff)
        assertBalanced(summary)
    }

    @Test
    fun `달 시작일을 1에서 25로 바꿔도 기록과 현재 잔고는 그대로이고 기간 경계만 바뀐다`() {
        val ledger = planExample()
        val entriesBefore = ledger.entries.toList()
        val timeline = ledger.timeline()

        val byFirst = periodOf(today, 1)
        val by25th = periodOf(today, 25)
        assertEquals(Period(day("2026-10-01"), day("2026-10-31")), byFirst)
        assertEquals(Period(day("2026-09-25"), day("2026-10-24")), by25th)

        val dailyFirst = dailyBalances(timeline, byFirst, today)
        val daily25th = dailyBalances(timeline, by25th, today)
        assertEquals(20, dailyFirst.size)
        assertEquals(26, daily25th.size)
        assertEquals(625_000L, dailyFirst.last())
        assertEquals(625_000L, daily25th.last())

        assertBalanced(periodSummary(timeline, byFirst, today))
        assertBalanced(periodSummary(timeline, by25th, today))
        assertBalanced(periodSummary(timeline, by25th.previous(25), today))
        assertEquals(entriesBefore, ledger.entries)
    }

    @Test
    fun `일별 잔고는 각 날짜가 끝났을 때의 잔고`() {
        val daily = dailyBalances(planExample().timeline(), october, today)

        assertEquals(20, daily.size)
        assertEquals(500_000L, daily[0]) // 10/1
        assertEquals(500_000L, daily[1]) // 10/2
        assertEquals(470_000L, daily[2]) // 10/3
        assertEquals(670_000L, daily[9]) // 10/10
        assertEquals(640_000L, daily[14]) // 10/15
        assertEquals(625_000L, daily[19]) // 10/20
    }

    @Test
    fun `오늘이 기간 첫날이면 점 하나`() {
        val firstDay = day("2026-10-25")
        val daily = dailyBalances(planExample().timeline(), periodOf(firstDay, 25), firstDay)

        assertArrayEquals(longArrayOf(625_000), daily)
    }

    @Test
    fun `지난 기간은 기간 전체를 그린다`() {
        val september = periodOf(day("2026-09-15"), 1)
        val timeline = Ledger().apply { balance("2026-09-10", 100_000) }.timeline()
        val daily = dailyBalances(timeline, september, today)

        assertEquals(30, daily.size)
        assertEquals(0L, daily[8]) // 9/9
        assertEquals(100_000L, daily[9]) // 9/10
        assertEquals(100_000L, daily[29]) // 9/30
    }

    @Test
    fun `아직 시작 전인 기간은 빈 그래프이고 합계는 0`() {
        val timeline = planExample().timeline()
        val november = october.next(1)

        assertEquals(0, dailyBalances(timeline, november, today).size)
        val summary = periodSummary(timeline, november, today)
        assertEquals(PeriodSummary(625_000, 625_000, 0, 0, 0, 0), summary)
    }

    @Test
    fun `기록 없는 기간이 중간에 끼면 이전 잔고를 이어받는 수평선이고 합계는 0`() {
        val timeline = Ledger().apply {
            balance("2026-09-10", 100_000)
            expense("2026-11-05", 1_000)
        }.timeline()
        val laterToday = day("2026-11-20")

        val daily = dailyBalances(timeline, october, laterToday)
        assertEquals(31, daily.size)
        assertTrue(daily.all { it == 100_000L })

        val summary = periodSummary(timeline, october, laterToday)
        assertEquals(PeriodSummary(100_000, 100_000, 0, 0, 0, 0), summary)
        assertBalanced(summary)
    }

    @Test
    fun `미래 날짜 기록은 오늘까지의 잔고·그래프·합계에 반영되지 않는다`() {
        val ledger = planExample().apply { expense("2026-10-28", 50_000) }
        val timeline = ledger.timeline()

        assertEquals(625_000L, balanceAt(timeline, today))
        assertEquals(625_000L, dailyBalances(timeline, october, today).last())
        val summary = periodSummary(timeline, october, today)
        assertEquals(45_000L, summary.expense)
        assertEquals(625_000L, summary.endBalance)
        assertBalanced(summary)

        // 그날이 지나면 반영된다
        val later = day("2026-10-31")
        assertEquals(575_000L, balanceAt(timeline, later))
        assertBalanced(periodSummary(timeline, october, later))
    }

    @Test
    fun `13자리 금액을 많이 쌓아도 오버플로 없음`() {
        val max = 9_999_999_999_999L
        val ledger = Ledger().apply {
            repeat(1_000) { income("2026-10-01", max) }
            expense("2026-10-02", max)
            balance("2026-10-03", -max)
        }
        val timeline = ledger.timeline()

        assertEquals(max * 999, timeline[1_000].balance)
        assertEquals(-max * 1_000, timeline[1_001].diff)
        val summary = periodSummary(timeline, october, today)
        assertEquals(max * 1_000, summary.income)
        assertBalanced(summary)
    }
}
