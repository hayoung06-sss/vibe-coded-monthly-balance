package com.balance.monthly.data

import com.balance.monthly.domain.Period
import com.balance.monthly.domain.PeriodSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class SnapshotStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val store by lazy { SnapshotStore(folder.root) }
    private val file get() = File(folder.root, SnapshotStore.HOME_FILE)

    private val snapshot = HomeSnapshot(
        dataVersion = 42,
        writtenOn = LocalDate.parse("2026-10-20"),
        monthStartDay = 31,
        balance = -9_999_999_999_999,
        period = Period(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-30")),
        dailyBalances = LongArray(31) { it * 1_000L - 5_000 },
        firstIndex = 3,
        markers = intArrayOf(3, 14, 30),
        summary = PeriodSummary(1, 2, 3, 4, -5, 6),
    )

    @Test
    fun `쓴 값을 그대로 읽는다`() {
        store.writeHome(snapshot)

        assertEquals(snapshot, store.readHome())
    }

    @Test
    fun `31일 기간도 1KB 미만`() {
        store.writeHome(snapshot)

        assertTrue("size=${file.length()}", file.length() < 1024)
    }

    @Test
    fun `파일이 없으면 null`() {
        assertNull(store.readHome())
    }

    @Test
    fun `손상된 파일은 지우고 null`() {
        store.writeHome(snapshot)
        file.writeBytes(file.readBytes().copyOf(20))

        assertNull(store.readHome())
        assertFalse(file.exists())
    }

    @Test
    fun `포맷 버전이 다르면 지우고 null`() {
        store.writeHome(snapshot)
        val bytes = file.readBytes()
        bytes[0] = 99
        file.writeBytes(bytes)

        assertNull(store.readHome())
        assertFalse(file.exists())
    }

    @Test
    fun `덮어쓰면 새 값을 읽는다`() {
        store.writeHome(snapshot)
        val newer = HomeSnapshot(
            43, snapshot.writtenOn, 1, 0, Period(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")),
            LongArray(0), 0, IntArray(0), PeriodSummary(0, 0, 0, 0, 0, 0),
        )
        store.writeHome(newer)

        assertEquals(newer, store.readHome())
        assertFalse(File(folder.root, SnapshotStore.HOME_FILE + ".tmp").exists())
    }

    private val forecast = CachedForecast(
        dataVersion = 7,
        computedOn = LocalDate.parse("2026-10-20"),
        monthStartDay = 25,
        algorithmVersion = 1,
        balances = LongArray(31) { 1_000_000L - it * 12_345 },
    )

    @Test
    fun `예측 캐시를 쓴 값 그대로 읽고 1KB 미만`() {
        store.writeForecast(forecast)

        assertEquals(forecast, store.readForecast())
        assertTrue(File(folder.root, SnapshotStore.FORECAST_FILE).length() < 1024)
    }

    @Test
    fun `손상된 예측 캐시는 지우고 null`() {
        store.writeForecast(forecast)
        val file = File(folder.root, SnapshotStore.FORECAST_FILE)
        file.writeBytes(file.readBytes().copyOf(10))

        assertNull(store.readForecast())
        assertFalse(file.exists())
    }

    @Test
    fun `두 캐시는 서로 영향을 주지 않는다`() {
        store.writeHome(snapshot)
        store.writeForecast(forecast)
        store.clearForecast()

        assertEquals(snapshot, store.readHome())
        assertNull(store.readForecast())
    }

    @Test
    fun `지우면 없는 것으로 취급`() {
        store.writeHome(snapshot)
        store.clearHome()

        assertNull(store.readHome())
    }
}
