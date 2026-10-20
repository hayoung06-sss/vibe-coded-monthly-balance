package com.balance.monthly.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest {

    private lateinit var settings: Settings

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("settings-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        settings = Settings(prefs)
    }

    @Test
    fun 기본값() {
        assertEquals(1, settings.monthStartDay)
        assertEquals(ThemeMode.DARK, settings.theme)
        assertTrue(settings.showForecast)
        assertEquals(0L, settings.dataVersion)
    }

    @Test
    fun 바꾼_값을_그대로_읽는다() {
        settings.monthStartDay = 25
        settings.theme = ThemeMode.SYSTEM
        settings.showForecast = false

        assertEquals(25, settings.monthStartDay)
        assertEquals(ThemeMode.SYSTEM, settings.theme)
        assertFalse(settings.showForecast)
    }

    @Test
    fun 범위를_벗어난_시작일은_거부하고_값을_유지한다() {
        settings.monthStartDay = 31
        assertTrue(runCatching { settings.monthStartDay = 0 }.isFailure)
        assertTrue(runCatching { settings.monthStartDay = 32 }.isFailure)
        assertEquals(31, settings.monthStartDay)
    }

    @Test
    fun dataVersion은_1씩_증가한다() {
        assertEquals(1L, settings.incrementDataVersion())
        assertEquals(2L, settings.incrementDataVersion())
        assertEquals(2L, settings.dataVersion)
    }
}
