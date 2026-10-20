package com.balance.monthly.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.balance.monthly.domain.EntryType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class EntryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var settings: Settings
    private lateinit var repository: EntryRepository
    private var clock = 1_000L

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val prefs = context.getSharedPreferences("repository-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        settings = Settings(prefs)
        repository = EntryRepository(db.entryDao(), settings, now = { clock++ })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val oct5 = LocalDate.parse("2026-10-05")

    @Test
    fun 추가_수정_삭제마다_dataVersion이_1씩_오른다() = runTest {
        assertEquals(0L, settings.dataVersion)

        val id = repository.add(EntryType.EXPENSE, 1_000, oct5)
        assertEquals(1L, settings.dataVersion)

        repository.update(id, EntryType.EXPENSE, 2_000, oct5, null)
        assertEquals(2L, settings.dataVersion)

        repository.delete(id)
        assertEquals(3L, settings.dataVersion)
    }

    @Test
    fun 읽기와_실패한_쓰기는_dataVersion을_올리지_않는다() = runTest {
        repository.getAll()
        assertFalse(repository.update(999, EntryType.EXPENSE, 1_000, oct5, null))
        assertFalse(repository.delete(999))
        runCatching { repository.add(EntryType.EXPENSE, 0, oct5) }

        assertEquals(0L, settings.dataVersion)
    }

    @Test
    fun 수정해도_createdAt은_바뀌지_않는다() = runTest {
        val id = repository.add(EntryType.EXPENSE, 1_000, oct5)
        val createdAt = repository.getAll().single().createdAt

        repository.update(id, EntryType.INCOME, 3_000, oct5.plusDays(2), "월급")

        val updated = repository.getAll().single()
        assertEquals(createdAt, updated.createdAt)
        assertEquals(EntryType.INCOME, updated.type)
        assertEquals(3_000L, updated.amount)
        assertEquals(oct5.plusDays(2), updated.date)
        assertEquals("월급", updated.memo)
    }

    @Test
    fun 같은_날짜에서는_입력한_순서대로_돌려준다() = runTest {
        repository.add(EntryType.EXPENSE, 1, oct5)
        repository.add(EntryType.BALANCE, 2, oct5)
        repository.add(EntryType.EXPENSE, 3, oct5)

        assertEquals(listOf(1L, 2L, 3L), repository.getAll().map { it.amount })
    }

    @Test
    fun 금액_검증() = runTest {
        assertTrue(runCatching { repository.add(EntryType.INCOME, 0, oct5) }.isFailure)
        assertTrue(runCatching { repository.add(EntryType.EXPENSE, -1, oct5) }.isFailure)
        assertTrue(runCatching { repository.add(EntryType.BALANCE, EntryRepository.MAX_AMOUNT + 1, oct5) }.isFailure)

        repository.add(EntryType.BALANCE, 0, oct5)
        repository.add(EntryType.BALANCE, -EntryRepository.MAX_AMOUNT, oct5)
        repository.add(EntryType.INCOME, EntryRepository.MAX_AMOUNT, oct5)
        assertEquals(3, repository.getAll().size)
    }

    @Test
    fun 빈_메모는_null로_저장한다() = runTest {
        repository.add(EntryType.EXPENSE, 1_000, oct5, "   ")
        repository.add(EntryType.EXPENSE, 1_000, oct5, "  점심 ")

        assertEquals(listOf(null, "점심"), repository.getAll().map { it.memo })
        assertNull(repository.getAll().first().memo)
    }
}
