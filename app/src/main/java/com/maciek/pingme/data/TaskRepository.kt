package com.maciek.pingme.data

import androidx.room.withTransaction
import com.maciek.pingme.domain.DayTask
import com.maciek.pingme.domain.Occurrence
import com.maciek.pingme.domain.OccurrenceStatus
import com.maciek.pingme.domain.Task
import com.maciek.pingme.domain.missedDates
import com.maciek.pingme.domain.tasksForDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val db: PingMeDatabase) {
    private val taskDao = db.taskDao()
    private val occurrenceDao = db.occurrenceDao()

    fun observeDay(date: LocalDate): Flow<List<DayTask>> =
        combine(taskDao.observeActive(), occurrenceDao.observeForDate(date.toEpochDay())) { tasks, occurrences ->
            tasksForDay(tasks.map { it.toDomain() }, occurrences.map { it.toDomain() }, date)
        }

    suspend fun addTask(task: Task) {
        taskDao.insert(task.toEntity())
    }

    suspend fun setDone(taskId: Long, date: LocalDate, done: Boolean) {
        if (done) {
            occurrenceDao.upsert(Occurrence(taskId, date, OccurrenceStatus.DONE).toEntity())
        } else {
            occurrenceDao.delete(taskId, date.toEpochDay())
        }
    }

    /** Zapisuje jako pominięte wszystkie nieodhaczone wystąpienia z minionych dni. */
    suspend fun fillMissedOccurrences(today: LocalDate) = db.withTransaction {
        val yesterday = today.minusDays(1)
        for (task in taskDao.getActive().map { it.toDomain() }) {
            if (task.checkedUntil >= yesterday) continue
            val done = occurrenceDao
                .getForTaskInRange(task.id, task.checkedUntil.plusDays(1).toEpochDay(), yesterday.toEpochDay())
                .map { it.toDomain() }
                .filter { it.status == OccurrenceStatus.DONE }
                .map { it.date }
                .toSet()
            val skipped = task.missedDates(today, done).map { Occurrence(task.id, it, OccurrenceStatus.SKIPPED).toEntity() }
            occurrenceDao.insertIgnore(skipped)
            taskDao.update(task.copy(checkedUntil = yesterday).toEntity())
        }
    }
}
