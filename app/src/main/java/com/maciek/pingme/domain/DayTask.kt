package com.maciek.pingme.domain

import java.time.LocalDate

/** Zadanie zaplanowane na konkretny dzień wraz z informacją, czy zostało zrobione. */
data class DayTask(val task: Task, val done: Boolean)

/** Zadania wypadające w danym dniu, posortowane po godzinie przypomnienia. */
fun tasksForDay(tasks: List<Task>, occurrences: List<Occurrence>, date: LocalDate): List<DayTask> {
    val doneIds = occurrences
        .filter { it.date == date && it.status == OccurrenceStatus.DONE }
        .map { it.taskId }
        .toSet()
    return tasks
        .filter { !it.archived && it.occursOn(date) }
        .sortedBy { it.reminderTime }
        .map { DayTask(it, done = it.id in doneIds) }
}
