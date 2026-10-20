package com.balance.monthly.data

import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.LedgerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 기록 읽기·쓰기의 유일한 통로. 쓰기가 성공하면 dataVersion을 올린다. */
class EntryRepository(
    private val dao: EntryDao,
    private val settings: Settings,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val entries: Flow<List<LedgerEntry>> = dao.observeAll().map { list -> list.map { it.toLedgerEntry() } }

    suspend fun getAll(): List<LedgerEntry> = dao.getAll().map { it.toLedgerEntry() }

    suspend fun add(type: EntryType, amount: Long, date: LocalDate, memo: String? = null): Long {
        validate(type, amount)
        val id = dao.insert(Entry(type = type, amount = amount, date = date, createdAt = now(), memo = memo.normalized()))
        settings.incrementDataVersion()
        return id
    }

    /** createdAt은 바꾸지 않는다. 같은 날짜 안의 순서가 처음 입력한 때로 유지된다. */
    suspend fun update(id: Long, type: EntryType, amount: Long, date: LocalDate, memo: String?): Boolean {
        validate(type, amount)
        val existing = dao.getById(id) ?: return false
        val updated = existing.copy(type = type, amount = amount, date = date, memo = memo.normalized())
        if (dao.update(updated) == 0) return false
        settings.incrementDataVersion()
        return true
    }

    suspend fun delete(id: Long): Boolean {
        if (dao.deleteById(id) == 0) return false
        settings.incrementDataVersion()
        return true
    }

    private fun validate(type: EntryType, amount: Long) {
        require(amount in -MAX_AMOUNT..MAX_AMOUNT) { "amount out of range: $amount" }
        if (type != EntryType.BALANCE) require(amount > 0) { "$type amount must be positive: $amount" }
    }

    private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        /** 입력은 13자리로 제한한다. */
        const val MAX_AMOUNT = 9_999_999_999_999L
    }
}
