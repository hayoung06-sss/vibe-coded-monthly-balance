package com.balance.monthly.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY date, createdAt, id")
    suspend fun getAll(): List<Entry>

    @Query("SELECT * FROM entries ORDER BY date, createdAt, id")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): Entry?

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry): Int

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
