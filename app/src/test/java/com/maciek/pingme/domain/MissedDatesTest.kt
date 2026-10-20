package com.maciek.pingme.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedDatesTest {

    @Test
    fun daily_returnsPastDaysWithoutDone() {
        val t = task(date(2026, 10, 15), Schedule.Daily)
        val missed = t.missedDates(today = date(2026, 10, 19), done = setOf(date(2026, 10, 16)))
        assertEquals(listOf(date(2026, 10, 15), date(2026, 10, 17), date(2026, 10, 18)), missed)
    }

    @Test
    fun doesNotIncludeToday() {
        val t = task(date(2026, 10, 21), Schedule.Daily)
        assertTrue(t.missedDates(today = date(2026, 10, 21), done = emptySet()).isEmpty())
    }

    @Test
    fun alreadyCheckedUntilYesterday_returnsNothing() {
        val t = task(date(2026, 10, 1), Schedule.Daily).copy(checkedUntil = date(2026, 10, 20))
        assertTrue(t.missedDates(today = date(2026, 10, 21), done = emptySet()).isEmpty())
    }

    @Test
    fun startsAfterCheckedUntil() {
        val t = task(date(2026, 10, 1), Schedule.Daily).copy(checkedUntil = date(2026, 10, 18))
        assertEquals(
            listOf(date(2026, 10, 19), date(2026, 10, 20)),
            t.missedDates(today = date(2026, 10, 21), done = emptySet()),
        )
    }

    @Test
    fun archived_returnsNothing() {
        val t = task(date(2026, 10, 1), Schedule.Daily).copy(archived = true)
        assertTrue(t.missedDates(today = date(2026, 10, 21), done = emptySet()).isEmpty())
    }

    @Test
    fun weekly_returnsOnlyScheduledDays() {
        val t = task(date(2026, 10, 7), Schedule.Weekly) // środa
        assertEquals(
            listOf(date(2026, 10, 7), date(2026, 10, 14)),
            t.missedDates(today = date(2026, 10, 21), done = setOf(date(2026, 10, 21))),
        )
    }

    @Test
    fun futureOnce_returnsNothing() {
        val t = task(date(2026, 10, 25), Schedule.Once).copy(checkedUntil = date(2026, 10, 20))
        assertTrue(t.missedDates(today = date(2026, 10, 21), done = emptySet()).isEmpty())
    }
}
