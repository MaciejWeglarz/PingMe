package com.maciek.pingme.domain

import java.time.YearMonth

/**
 * Seria: ile ostatnich wystąpień zostało zrobionych z rzędu, licząc od najnowszego wstecz.
 * Dzisiejsze nieodhaczone wystąpienie nie ma jeszcze wpisu, więc nie przerywa serii.
 */
fun streak(occurrences: List<Occurrence>): Int =
    occurrences.sortedByDescending { it.date }.takeWhile { it.status == OccurrenceStatus.DONE }.size

/** Seria zadania albo null dla zadań jednorazowych, które serii nie mają. */
fun Task.streakOrNull(occurrences: List<Occurrence>): Int? =
    if (schedule == Schedule.Once) null else streak(occurrences)

data class TaskStats(val task: Task, val done: Int, val skipped: Int, val streak: Int?)

/**
 * Statystyki za miesiąc dla zadań, które miały w nim co najmniej jedno wystąpienie (także usuniętych).
 * Najpierw aktywne zadania po godzinie przypomnienia, potem usunięte.
 */
fun monthlyStats(tasks: List<Task>, occurrences: List<Occurrence>, month: YearMonth): List<TaskStats> {
    val byTask = occurrences.groupBy { it.taskId }
    return tasks
        .mapNotNull { task ->
            val all = byTask[task.id].orEmpty()
            val inMonth = all.filter { YearMonth.from(it.date) == month }
            if (inMonth.isEmpty()) return@mapNotNull null
            TaskStats(
                task = task,
                done = inMonth.count { it.status == OccurrenceStatus.DONE },
                skipped = inMonth.count { it.status == OccurrenceStatus.SKIPPED },
                streak = if (task.archived) null else task.streakOrNull(all),
            )
        }
        .sortedWith(compareBy({ it.task.archived }, { it.task.reminderTime }))
}
