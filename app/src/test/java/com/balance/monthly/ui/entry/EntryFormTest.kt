package com.balance.monthly.ui.entry

import com.balance.monthly.domain.EntryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryFormTest {

    @Test
    fun `숫자만 남기고 쉼표를 지운다`() {
        assertEquals("1234567", sanitizeDigits("1,234,567"))
        assertEquals("1000", sanitizeDigits("1,000"))
        assertEquals("", sanitizeDigits(""))
        assertEquals("", sanitizeDigits(",-."))
    }

    @Test
    fun `앞의 0은 지우고 0 하나는 남긴다`() {
        assertEquals("5", sanitizeDigits("05"))
        assertEquals("0", sanitizeDigits("0"))
        assertEquals("0", sanitizeDigits("000"))
    }

    @Test
    fun `13자리로 자른다`() {
        assertEquals("9999999999999", sanitizeDigits("99999999999999"))
    }

    @Test
    fun `음수는 실제 잔고에서만`() {
        assertEquals(-30_000L, draftAmount("30000", negative = true, EntryType.BALANCE))
        assertEquals(30_000L, draftAmount("30000", negative = true, EntryType.EXPENSE))
        assertNull(draftAmount("", negative = false, EntryType.INCOME))
    }

    @Test
    fun `수입·지출은 0원과 빈 금액을 막고 실제 잔고는 0원을 허용`() {
        assertFalse(canSave(null, EntryType.INCOME))
        assertFalse(canSave(0, EntryType.INCOME))
        assertFalse(canSave(0, EntryType.EXPENSE))
        assertTrue(canSave(1, EntryType.EXPENSE))
        assertTrue(canSave(0, EntryType.BALANCE))
        assertTrue(canSave(-5, EntryType.BALANCE))
        assertFalse(canSave(null, EntryType.BALANCE))
    }
}
