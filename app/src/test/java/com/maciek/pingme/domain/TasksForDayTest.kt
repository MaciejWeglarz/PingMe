package com.maciek.pingme.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class TasksForDayTest {

    private val today = date(2026, 10, 21)

    @Test
    fun futureTask_notShownToday_shownOnItsDay() {
        val future = task(date(2026, 10, 25), Schedule.Once).copy(id = 1)
        assertEquals(emptyList<DayTask>(), tasksForDay(listOf(future), emptyList(), today))
        assertEquals(
            listOf(DayTask(future, done = false, streak = null)),
            tasksForDay(listOf(future), emptyList(), date(2026, 10, 25)),
        )
    }

    @Test
    fun doneOccurrenceToday_marksTaskDone() {
        val t = task(today, Schedule.Daily).copy(id = 1)
        val occurrences = listOf(Occurrence(1, today, OccurrenceStatus.DONE))
        assertEquals(listOf(DayTask(t, done = true, streak = 1)), tasksForDay(listOf(t), occurrences, today))
    }

    @Test
    fun occurrenceFromOtherDayOrSkipped_doesNotMarkDone() {
        val t = task(date(2026, 10, 1), Schedule.Daily).copy(id = 1)
        val occurrences = listOf(
            Occurrence(1, today.minusDays(1), OccurrenceStatus.DONE),
            Occurrence(1, today, OccurrenceStatus.SKIPPED),
        )
        assertEquals(listOf(DayTask(t, done = false, streak = 0)), tasksForDay(listOf(t), occurrences, today))
    }

    @Test
    fun streak_countsOnlyThisTasksHistory() {
        val a = task(date(2026, 10, 1), Schedule.Daily).copy(id = 1)
        val b = task(date(2026, 10, 1), Schedule.Daily).copy(id = 2)
        val occurrences = listOf(
            Occurrence(1, today.minusDays(2), OccurrenceStatus.DONE),
            Occurrence(1, today.minusDays(1), OccurrenceStatus.DONE),
            Occurrence(2, today.minusDays(1), OccurrenceStatus.SKIPPED),
        )
        assertEquals(listOf(2, 0), tasksForDay(listOf(a, b), occurrences, today).map { it.streak })
    }

    @Test
    fun onceTask_hasNoStreak() {
        val t = task(today, Schedule.Once).copy(id = 1)
        val occurrences = listOf(Occurrence(1, today, OccurrenceStatus.DONE))
        assertNull(tasksForDay(listOf(t), occurrences, today).single().streak)
    }

    @Test
    fun archivedTask_notShown() {
        val t = task(today, Schedule.Daily).copy(id = 1, archived = true)
        assertEquals(emptyList<DayTask>(), tasksForDay(listOf(t), emptyList(), today))
    }

    @Test
    fun sortedByReminderTime() {
        val evening = task(today, Schedule.Daily).copy(id = 1, reminderTime = LocalTime.of(21, 0))
        val morning = task(today, Schedule.Daily).copy(id = 2, reminderTime = LocalTime.of(7, 0))
        assertEquals(listOf(2L, 1L), tasksForDay(listOf(evening, morning), emptyList(), today).map { it.task.id })
    }
}
