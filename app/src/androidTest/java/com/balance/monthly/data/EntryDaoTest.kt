package com.balance.monthly.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.balance.monthly.domain.EntryType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class EntryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EntryDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.entryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entry(type: EntryType, amount: Long, date: String, createdAt: Long, memo: String? = null) =
        Entry(type = type, amount = amount, date = LocalDate.parse(date), createdAt = createdAt, memo = memo)

    @Test
    fun 저장한_값을_그대로_읽는다() = runTest {
        val id = dao.insert(entry(EntryType.BALANCE, -12_345, "2028-02-29", createdAt = 42, memo = "메모"))

        val saved = dao.getById(id)
        assertEquals(entry(EntryType.BALANCE, -12_345, "2028-02-29", createdAt = 42, memo = "메모").copy(id = id), saved)
    }

    @Test
    fun 날짜_생성시각_순으로_읽는다() = runTest {
        dao.insert(entry(EntryType.EXPENSE, 1, "2026-10-05", createdAt = 300))
        dao.insert(entry(EntryType.INCOME, 2, "2026-10-01", createdAt = 400))
        dao.insert(entry(EntryType.BALANCE, 3, "2026-10-05", createdAt = 100))
        dao.insert(entry(EntryType.EXPENSE, 4, "2026-10-05", createdAt = 200))

        assertEquals(listOf(2L, 3L, 4L, 1L), dao.getAll().map { it.amount })
    }

    @Test
    fun 수정하면_바뀐_값이_저장된다() = runTest {
        val id = dao.insert(entry(EntryType.EXPENSE, 1_000, "2026-10-05", createdAt = 1))
        val original = dao.getById(id)!!

        assertEquals(1, dao.update(original.copy(type = EntryType.INCOME, amount = 2_000, date = LocalDate.parse("2026-10-07"), memo = "수정")))
        assertEquals(original.copy(type = EntryType.INCOME, amount = 2_000, date = LocalDate.parse("2026-10-07"), memo = "수정"), dao.getById(id))
    }

    @Test
    fun 삭제하면_사라진다() = runTest {
        val id = dao.insert(entry(EntryType.EXPENSE, 1_000, "2026-10-05", createdAt = 1))

        assertEquals(1, dao.deleteById(id))
        assertNull(dao.getById(id))
        assertEquals(0, dao.deleteById(id))
    }

    @Test
    fun 관찰중인_목록은_쓰기마다_갱신된다() = runTest {
        assertEquals(emptyList<Entry>(), dao.observeAll().first())

        val id = dao.insert(entry(EntryType.INCOME, 5_000, "2026-10-05", createdAt = 1))
        assertEquals(listOf(id), dao.observeAll().first().map { it.id })

        dao.deleteById(id)
        assertEquals(emptyList<Entry>(), dao.observeAll().first())
    }

    @Test
    fun 날짜는_epochDay_종류는_이름으로_저장된다() = runTest {
        dao.insert(entry(EntryType.EXPENSE, 1, "2026-10-20", createdAt = 1))

        db.openHelper.readableDatabase.query("SELECT type, date FROM entries").use { cursor ->
            cursor.moveToFirst()
            assertEquals("EXPENSE", cursor.getString(0))
            assertEquals(LocalDate.parse("2026-10-20").toEpochDay(), cursor.getLong(1))
        }
    }
}
