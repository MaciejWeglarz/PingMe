package com.maciek.pingme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY reminderMinutes")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE archived = 0")
    suspend fun getActive(): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)
}

@Dao
interface OccurrenceDao {
    @Upsert
    suspend fun upsert(occurrence: OccurrenceEntity)

    @Query("DELETE FROM occurrences WHERE taskId = :taskId AND date = :date")
    suspend fun delete(taskId: Long, date: Long)

    /** Nie nadpisuje istniejących wpisów, np. ZROBIONE. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(occurrences: List<OccurrenceEntity>)

    @Query("SELECT * FROM occurrences WHERE date = :date")
    fun observeForDate(date: Long): Flow<List<OccurrenceEntity>>

    @Query("SELECT * FROM occurrences WHERE taskId = :taskId AND date BETWEEN :from AND :to")
    suspend fun getForTaskInRange(taskId: Long, from: Long, to: Long): List<OccurrenceEntity>
}
