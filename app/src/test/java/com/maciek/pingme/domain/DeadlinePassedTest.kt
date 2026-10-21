package com.maciek.pingme.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DeadlinePassedTest {

    private val day = date(2026, 10, 21)
    private val withDeadline = task(date(2026, 10, 1), Schedule.Daily).copy(deadline = LocalTime.of(9, 0))

    @Test
    fun beforeDeadline_false() {
        assertFalse(withDeadline.isDeadlinePassed(day, day.atTime(8, 59)))
    }

    @Test
    fun exactlyAtDeadline_true() {
        assertTrue(withDeadline.isDeadlinePassed(day, day.atTime(9, 0)))
    }

    @Test
    fun noDeadline_neverPassed() {
        val noDeadline = withDeadline.copy(deadline = null)
        assertFalse(noDeadline.isDeadlinePassed(day, day.atTime(23, 59)))
    }

    @Test
    fun snoozeAfterMidnight_yesterdaysDeadlinePassed() {
        assertTrue(withDeadline.isDeadlinePassed(day, day.plusDays(1).atTime(0, 5)))
    }

    @Test
    fun earlyMorningToday_yesterdaysOccurrenceNotConfusedWithToday() {
        // 00:05 nowego dnia: deadline 9:00 dla nowego dnia jeszcze nie minął.
        assertFalse(withDeadline.isDeadlinePassed(day.plusDays(1), day.plusDays(1).atTime(0, 5)))
    }
}
