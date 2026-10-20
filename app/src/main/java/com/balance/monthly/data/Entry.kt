package com.balance.monthly.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.LedgerEntry
import java.time.LocalDate

/**
 * 원본 데이터의 전부. 기간·달 시작일에 의존하는 값은 절대 넣지 않는다.
 * type은 enum 이름(TEXT), date는 epochDay(INTEGER)로 저장된다.
 */
@Entity(tableName = "entries", indices = [Index(value = ["date", "createdAt"])])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EntryType,
    val amount: Long,
    val date: LocalDate,
    val createdAt: Long,
    val memo: String? = null,
)

fun Entry.toLedgerEntry() = LedgerEntry(id, type, amount, date, createdAt, memo)

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()
}
