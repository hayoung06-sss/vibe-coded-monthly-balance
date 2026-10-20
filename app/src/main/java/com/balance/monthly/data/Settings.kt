package com.balance.monthly.data

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/** 작은 설정 값들. 테마는 첫 프레임에 필요하므로 동기로 읽는다. */
class Settings(private val prefs: SharedPreferences) {

    var monthStartDay: Int
        get() = prefs.getInt(KEY_MONTH_START_DAY, 1)
        set(value) {
            require(value in 1..31) { "monthStartDay must be in 1..31: $value" }
            prefs.edit().putInt(KEY_MONTH_START_DAY, value).apply()
        }

    var theme: ThemeMode
        get() = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.DARK
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var showForecast: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FORECAST, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_FORECAST, value).apply()

    /** 기록이 바뀔 때마다 1씩 증가. 캐시가 최신인지 판단하는 값이다. */
    val dataVersion: Long
        get() = prefs.getLong(KEY_DATA_VERSION, 0)

    /** EntryRepository의 쓰기 함수에서만 부른다. */
    @Synchronized
    internal fun incrementDataVersion(): Long {
        val next = dataVersion + 1
        prefs.edit().putLong(KEY_DATA_VERSION, next).apply()
        return next
    }

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_MONTH_START_DAY = "monthStartDay"
        private const val KEY_THEME = "theme"
        private const val KEY_SHOW_FORECAST = "showForecast"
        private const val KEY_DATA_VERSION = "dataVersion"

        fun create(context: Context) =
            Settings(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}
