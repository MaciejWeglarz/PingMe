package com.maciek.pingme.data

import androidx.room.withTransaction
import com.maciek.pingme.data.reminder.Notifier
import com.maciek.pingme.data.reminder.ReminderScheduler
import com.maciek.pingme.domain.DayTask
import com.maciek.pingme.domain.Occurrence
import com.maciek.pingme.domain.OccurrenceStatus
import com.maciek.pingme.domain.Task
import com.maciek.pingme.domain.TaskStats
import com.maciek.pingme.domain.missedDates
import com.maciek.pingme.domain.monthlyStats
import com.maciek.pingme.domain.tasksForDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val db: PingMeDatabase,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
) {
    private val taskDao = db.taskDao()
    private val occurrenceDao = db.occurrenceDao()

    suspend fun getTask(id: Long): Task? = taskDao.getById(id)?.toDomain()

    suspend fun isDone(taskId: Long, date: LocalDate): Boolean {
        val day = date.toEpochDay()
        return occurrenceDao.getForTaskInRange(taskId, day, day).any { it.toDomain().status == OccurrenceStatus.DONE }
    }

    fun observeDay(date: LocalDate): Flow<List<DayTask>> =
        combine(taskDao.observeActive(), occurrenceDao.observeAll()) { tasks, occurrences ->
            tasksForDay(tasks.map { it.toDomain() }, occurrences.map { it.toDomain() }, date)
        }

    fun observeMonthlyStats(month: YearMonth): Flow<List<TaskStats>> =
        combine(taskDao.observeAll(), occurrenceDao.observeAll()) { tasks, occurrences ->
            monthlyStats(tasks.map { it.toDomain() }, occurrences.map { it.toDomain() }, month)
        }

    fun observeActiveTasks(): Flow<List<Task>> =
        taskDao.observeActive().map { tasks -> tasks.map { it.toDomain() } }

    suspend fun addTask(task: Task) {
        val id = taskDao.insert(task.toEntity())
        scheduler.schedule(task.copy(id = id))
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
        scheduler.schedule(task)
    }

    /** „Usunięcie” zadania: znika z list, ale historia zostaje w statystykach. */
    suspend fun archiveTask(id: Long) {
        val archived = getTask(id)?.copy(archived = true) ?: return
        taskDao.update(archived.toEntity())
        // Dla zarchiwizowanego zadania nie ma kolejnych terminów, więc alarmy zostaną anulowane.
        scheduler.schedule(archived)
        notifier.cancelAll(id)
    }

    suspend fun setDone(taskId: Long, date: LocalDate, done: Boolean) {
        if (done) {
            occurrenceDao.upsert(Occurrence(taskId, date, OccurrenceStatus.DONE).toEntity())
            notifier.cancelAll(taskId)
        } else {
            occurrenceDao.delete(taskId, date.toEpochDay())
        }
    }

    suspend fun exportBackup(): Backup = db.withTransaction {
        Backup(taskDao.getAll(), occurrenceDao.getAll())
    }

    /** Zastępuje całą bazę kopią: w jednej transakcji, więc albo wszystko, albo nic. */
    suspend fun importBackup(backup: Backup) {
        val oldIds = taskDao.getAll().map { it.id }
        db.withTransaction {
            // Najpierw historia, bo odwołuje się do zadań (klucz obcy).
            occurrenceDao.deleteAll()
            taskDao.deleteAll()
            taskDao.insertAll(backup.tasks)
            occurrenceDao.insertAll(backup.occurrences)
        }
        oldIds.forEach {
            scheduler.cancel(it)
            notifier.cancelAll(it)
        }
        scheduler.scheduleAll()
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
