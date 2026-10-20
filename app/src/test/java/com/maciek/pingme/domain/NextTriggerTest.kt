package com.maciek.pingme.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.LocalTime

class NextTriggerTest {

    private val eight = LocalTime.of(8, 0)

    @Test
    fun daily_laterToday() {
        val t = task(date(2026, 10, 21), Schedule.Daily)
        val now = date(2026, 10, 21).atTime(7, 30)
        assertEquals(date(2026, 10, 21).atTime(8, 0), t.nextTriggerAt(eight, now))
    }

    @Test
    fun daily_alreadyPassedToday_tomorrow() {
        val t = task(date(2026, 10, 21), Schedule.Daily)
        val now = date(2026, 10, 21).atTime(8, 0)
        assertEquals(date(2026, 10, 22).atTime(8, 0), t.nextTriggerAt(eight, now))
    }

    @Test
    fun weekly_passedToday_nextWeek() {
        val t = task(date(2026, 10, 21), Schedule.Weekly)
        val now = date(2026, 10, 21).atTime(9, 0)
        assertEquals(date(2026, 10, 28).atTime(8, 0), t.nextTriggerAt(eight, now))
    }

    @Test
    fun startInFuture_firstTriggerOnStartDate() {
        val t = task(date(2026, 11, 5), Schedule.Daily)
        val now = date(2026, 10, 21).atTime(12, 0)
        assertEquals(date(2026, 11, 5).atTime(8, 0), t.nextTriggerAt(eight, now))
    }

    @Test
    fun once_farInFuture() {
        val t = task(date(2028, 5, 1), Schedule.Once)
        val now = date(2026, 10, 21).atTime(12, 0)
        assertEquals(date(2028, 5, 1).atTime(8, 0), t.nextTriggerAt(eight, now))
    }

    @Test
    fun once_passed_null() {
        val t = task(date(2026, 10, 21), Schedule.Once)
        assertNull(t.nextTriggerAt(eight, date(2026, 10, 21).atTime(8, 1)))
        assertNull(t.nextTriggerAt(eight, date(2026, 10, 25).atTime(7, 0)))
    }

    @Test
    fun custom_nextSelectedDay() {
        val t = task(date(2026, 10, 1), Schedule.CustomDays(setOf(MONDAY)))
        val now = date(2026, 10, 21).atTime(12, 0) // środa
        assertEquals(date(2026, 10, 26).atTime(8, 0), t.nextTriggerAt(eight, now))
    }

    @Test
    fun custom_noDays_null() {
        val t = task(date(2026, 10, 1), Schedule.CustomDays(emptySet()))
        assertNull(t.nextTriggerAt(eight, date(2026, 10, 21).atTime(12, 0)))
    }

    @Test
    fun monthly31_passed_lastDayOfNextMonth() {
        val t = task(date(2026, 10, 31), Schedule.Monthly)
        val now = date(2026, 10, 31).atTime(9, 0)
        assertEquals(date(2026, 11, 30).atTime(8, 0), t.nextTriggerAt(eight, now))
    }

    @Test
    fun archived_null() {
        val t = task(date(2026, 10, 21), Schedule.Daily).copy(archived = true)
        assertNull(t.nextTriggerAt(eight, date(2026, 10, 21).atTime(7, 0)))
    }
}
