package com.balance.monthly.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartScaleTest {

    @Test
    fun `최소·최대에서 위아래 10퍼센트 여유`() {
        val range = chartRange(longArrayOf(500_000, 470_000, 670_000))!!
        assertEquals(450_000.0, range.start, 0.001)
        assertEquals(690_000.0, range.endInclusive, 0.001)
    }

    @Test
    fun `값이 모두 같으면 크기의 10퍼센트 여유`() {
        val range = chartRange(longArrayOf(625_000, 625_000))!!
        assertEquals(562_500.0, range.start, 0.001)
        assertEquals(687_500.0, range.endInclusive, 0.001)
    }

    @Test
    fun `값이 0 하나뿐이어도 범위가 생긴다`() {
        val range = chartRange(longArrayOf(0))!!
        assertEquals(-1_000.0, range.start, 0.001)
        assertEquals(1_000.0, range.endInclusive, 0.001)
    }

    @Test
    fun `음수가 섞이면 0이 범위에 들어온다`() {
        val range = chartRange(longArrayOf(-10_000, 50_000))!!
        assertEquals(true, 0.0 in range)
    }

    @Test
    fun `값이 없으면 null`() {
        assertNull(chartRange(LongArray(0)))
    }

    @Test
    fun `터치 위치에서 가장 가까운 날짜`() {
        // 31일 기간, 폭 300 → 하루 10
        assertEquals(0, dayIndexAt(0f, 0f, 300f, 31))
        assertEquals(0, dayIndexAt(4f, 0f, 300f, 31))
        assertEquals(1, dayIndexAt(6f, 0f, 300f, 31))
        assertEquals(30, dayIndexAt(300f, 0f, 300f, 31))
        assertEquals(30, dayIndexAt(400f, 0f, 300f, 31))
        assertEquals(0, dayIndexAt(-50f, 0f, 300f, 31))
    }
}
