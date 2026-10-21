package com.maciek.pingme.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class MoodTest {

    private val noon = LocalTime.of(12, 0)

    private fun item(done: Boolean, deadline: LocalTime? = null) =
        DayTask(task(date(2026, 10, 1), Schedule.Daily).copy(deadline = deadline), done = done, streak = 0)

    @Test
    fun allDone_happy() {
        assertEquals(Mood.HAPPY, dayMood(listOf(item(true), item(true)), skippedYesterday = false, noon))
    }

    @Test
    fun allDone_afterSkippedYesterday_stillHappy() {
        assertEquals(Mood.HAPPY, dayMood(listOf(item(true)), skippedYesterday = true, noon))
    }

    @Test
    fun someNotDone_noDeadlines_neutral() {
        assertEquals(Mood.NEUTRAL, dayMood(listOf(item(true), item(false)), skippedYesterday = false, noon))
    }

    @Test
    fun noTasksToday_neutral() {
        assertEquals(Mood.NEUTRAL, dayMood(emptyList(), skippedYesterday = false, noon))
    }

    @Test
    fun noTasksToday_skippedYesterday_sad() {
        assertEquals(Mood.SAD, dayMood(emptyList(), skippedYesterday = true, noon))
    }

    @Test
    fun skippedYesterday_somethingLeftToday_sad() {
        assertEquals(Mood.SAD, dayMood(listOf(item(false)), skippedYesterday = true, noon))
    }

    @Test
    fun deadlinePassed_notDone_sad() {
        assertEquals(Mood.SAD, dayMood(listOf(item(false, deadline = LocalTime.of(11, 0))), skippedYesterday = false, noon))
    }

    @Test
    fun exactlyAtDeadline_sad() {
        assertEquals(Mood.SAD, dayMood(listOf(item(false, deadline = noon)), skippedYesterday = false, noon))
    }

    @Test
    fun deadlineNotYet_neutral() {
        assertEquals(Mood.NEUTRAL, dayMood(listOf(item(false, deadline = LocalTime.of(13, 0))), skippedYesterday = false, noon))
    }

    @Test
    fun deadlinePassed_butDone_notSad() {
        val items = listOf(item(true, deadline = LocalTime.of(11, 0)), item(false))
        assertEquals(Mood.NEUTRAL, dayMood(items, skippedYesterday = false, noon))
    }
}
