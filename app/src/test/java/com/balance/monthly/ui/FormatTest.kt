package com.balance.monthly.ui

import com.balance.monthly.domain.periodOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatTest {

    @Test
    fun `금액은 천 단위 쉼표, 음수는 마이너스 기호`() {
        assertEquals("0", formatAmount(0))
        assertEquals("625,000", formatAmount(625_000))
        assertEquals("−30,000", formatAmount(-30_000))
        assertEquals("9,999,999,999,999", formatAmount(9_999_999_999_999))
        assertEquals("625,000원", formatWon(625_000))
    }

    @Test
    fun `요약 값은 양수에 플러스 부호`() {
        assertEquals("+200,000", formatSigned(200_000))
        assertEquals("−45,000", formatSigned(-45_000))
        assertEquals("0", formatSigned(0))
    }

    @Test
    fun `시작일 1이면 연월, 아니면 날짜 범위`() {
        val today = LocalDate.parse("2026-10-20")
        assertEquals("2026년 10월", periodLabel(periodOf(today, 1), 1))
        assertEquals("9/25 ~ 10/24", periodLabel(periodOf(today, 25), 25))
        assertEquals("10/25 ~ 11/24", periodLabel(periodOf(LocalDate.parse("2026-10-25"), 25), 25))
    }
}
