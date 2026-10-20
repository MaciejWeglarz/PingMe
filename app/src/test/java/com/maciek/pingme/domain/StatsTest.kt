package com.maciek.pingme.domain

import com.maciek.pingme.domain.OccurrenceStatus.DONE
import com.maciek.pingme.domain.OccurrenceStatus.SKIPPED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime
import java.time.YearMonth

class StatsTest {

    private fun occ(taskId: Long, day: Int, status: OccurrenceStatus, month: Int = 10) =
        Occurrence(taskId, date(2026, month, day), status)

    // --- Seria ---

    @Test
    fun streak_noOccurrences_zero() {
        assertEquals(0, streak(emptyList()))
    }

    @Test
    fun streak_allDone() {
        assertEquals(3, streak(listOf(occ(1, 18, DONE), occ(1, 19, DONE), occ(1, 20, DONE))))
    }

    @Test
    fun streak_brokenBySkipped_countsOnlyAfterIt() {
        val history = listOf(occ(1, 16, DONE), occ(1, 17, DONE), occ(1, 18, SKIPPED), occ(1, 19, DONE), occ(1, 20, DONE))
        assertEquals(2, streak(history))
    }

    @Test
    fun streak_lastSkipped_zero() {
        assertEquals(0, streak(listOf(occ(1, 19, DONE), occ(1, 20, SKIPPED))))
    }

    @Test
    fun streak_unsortedInput() {
        assertEquals(2, streak(listOf(occ(1, 20, DONE), occ(1, 18, SKIPPED), occ(1, 19, DONE))))
    }

    @Test
    fun streak_weeklyTask_countsOccurrencesNotDays() {
        // Wpisy co 7 dni: przerwy między nimi nie zerują serii.
        val history = listOf(occ(1, 7, DONE), occ(1, 14, DONE), occ(1, 21, DONE))
        assertEquals(3, streak(history))
    }

    @Test
    fun streakOrNull_onceTask_null() {
        val t = task(date(2026, 10, 21), Schedule.Once)
        assertNull(t.streakOrNull(listOf(occ(0, 21, DONE))))
    }

    // --- Statystyki miesięczne ---

    private val october = YearMonth.of(2026, 10)

    @Test
    fun countsOnlyOccurrencesFromMonth() {
        val t = task(date(2026, 9, 1), Schedule.Daily).copy(id = 1)
        val occurrences = listOf(
            occ(1, 30, DONE, month = 9),
            occ(1, 1, DONE), occ(1, 2, SKIPPED), occ(1, 3, DONE),
            occ(1, 1, SKIPPED, month = 11),
        )
        val stats = monthlyStats(listOf(t), occurrences, october).single()
        assertEquals(2, stats.done)
        assertEquals(1, stats.skipped)
    }

    @Test
    fun includesArchivedTasks_withoutStreak_afterActive() {
        val active = task(date(2026, 9, 1), Schedule.Daily).copy(id = 1, reminderTime = LocalTime.of(20, 0))
        val archived = task(date(2026, 9, 1), Schedule.Daily).copy(id = 2, archived = true, reminderTime = LocalTime.of(7, 0))
        val occurrences = listOf(occ(1, 5, DONE), occ(2, 5, DONE))
        val stats = monthlyStats(listOf(archived, active), occurrences, october)
        assertEquals(listOf(1L, 2L), stats.map { it.task.id })
        assertEquals(1, stats[0].streak)
        assertNull(stats[1].streak)
    }

    @Test
    fun skipsTasksWithoutOccurrencesInMonth() {
        val t = task(date(2026, 11, 1), Schedule.Daily).copy(id = 1)
        assertEquals(emptyList<TaskStats>(), monthlyStats(listOf(t), emptyList(), october))
    }

    @Test
    fun streakUsesWholeHistory_notOnlyMonth() {
        val t = task(date(2026, 9, 1), Schedule.Daily).copy(id = 1)
        val occurrences = listOf(occ(1, 29, DONE, month = 9), occ(1, 30, DONE, month = 9), occ(1, 1, DONE))
        assertEquals(3, monthlyStats(listOf(t), occurrences, october).single().streak)
    }

    @Test
    fun onceTask_hasNoStreak() {
        val t = task(date(2026, 10, 5), Schedule.Once).copy(id = 1)
        assertNull(monthlyStats(listOf(t), listOf(occ(1, 5, DONE)), october).single().streak)
    }
}
