package com.maciek.pingme.domain

import java.time.LocalDate

/** Zadanie zaplanowane na konkretny dzień wraz z informacją, czy zostało zrobione, i serią (null dla jednorazowych). */
data class DayTask(val task: Task, val done: Boolean, val streak: Int?)

/**
 * Zadania wypadające w danym dniu, posortowane po godzinie przypomnienia.
 * [occurrences] to cała historia: potrzebna do policzenia serii.
 */
fun tasksForDay(tasks: List<Task>, occurrences: List<Occurrence>, date: LocalDate): List<DayTask> {
    val byTask = occurrences.groupBy { it.taskId }
    return tasks
        .filter { !it.archived && it.occursOn(date) }
        .sortedBy { it.reminderTime }
        .map { task ->
            val history = byTask[task.id].orEmpty()
            DayTask(
                task = task,
                done = history.any { it.date == date && it.status == OccurrenceStatus.DONE },
                streak = task.streakOrNull(history),
            )
        }
}
