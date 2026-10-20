package com.maciek.pingme.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalTime

fun task(start: LocalDate, schedule: Schedule) = Task(
    name = "Test",
    reminderTime = LocalTime.of(8, 0),
    deadline = null,
    startDate = start,
    schedule = schedule,
    checkedUntil = start.minusDays(1),
)

fun date(y: Int, m: Int, d: Int): LocalDate = LocalDate.of(y, m, d)

class ScheduleRulesTest {

    /** Wszystkie dni w roku, w które zadanie wypada. */
    private fun Task.daysIn(year: Int): List<LocalDate> =
        generateSequence(date(year, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == year }
            .filter { occursOn(it) }
            .toList()

    // --- Jednorazowe ---

    @Test
    fun once_inFuture_occursOnlyOnItsDay() {
        val t = task(date(2026, 10, 25), Schedule.Once)
        assertFalse(t.occursOn(date(2026, 10, 21)))
        assertFalse(t.occursOn(date(2026, 10, 24)))
        assertTrue(t.occursOn(date(2026, 10, 25)))
        assertFalse(t.occursOn(date(2026, 10, 26)))
    }

    // --- Codziennie ---

    @Test
    fun daily_occursFromStartDate() {
        val t = task(date(2026, 10, 21), Schedule.Daily)
        assertFalse(t.occursOn(date(2026, 10, 20)))
        assertTrue(t.occursOn(date(2026, 10, 21)))
        assertTrue(t.occursOn(date(2027, 3, 1)))
    }

    // --- Co tydzień ---

    @Test
    fun weekly_occursOnSameDayOfWeek() {
        val t = task(date(2026, 10, 21), Schedule.Weekly) // środa
        assertTrue(t.occursOn(date(2026, 10, 28)))
        assertTrue(t.occursOn(date(2026, 11, 4)))
        assertFalse(t.occursOn(date(2026, 10, 22)))
        assertFalse(t.occursOn(date(2026, 10, 14))) // środa, ale przed startem
    }

    // --- Co miesiąc: koniec miesiąca ---

    @Test
    fun monthly_31st_fallsOnLastDayOfShorterMonths() {
        val t = task(date(2027, 1, 31), Schedule.Monthly)
        assertEquals(
            listOf(
                date(2027, 1, 31), date(2027, 2, 28), date(2027, 3, 31), date(2027, 4, 30),
                date(2027, 5, 31), date(2027, 6, 30), date(2027, 7, 31), date(2027, 8, 31),
                date(2027, 9, 30), date(2027, 10, 31), date(2027, 11, 30), date(2027, 12, 31),
            ),
            t.daysIn(2027),
        )
    }

    @Test
    fun monthly_30th_fallsOn28FebAnd30th() {
        val t = task(date(2027, 1, 30), Schedule.Monthly)
        assertTrue(t.occursOn(date(2027, 2, 28)))
        assertTrue(t.occursOn(date(2027, 3, 30)))
        assertFalse(t.occursOn(date(2027, 3, 31)))
        assertTrue(t.occursOn(date(2027, 4, 30)))
    }

    @Test
    fun monthly_29th_fallsOn28FebInNonLeapYear() {
        val t = task(date(2027, 1, 29), Schedule.Monthly)
        assertTrue(t.occursOn(date(2027, 2, 28)))
        assertTrue(t.occursOn(date(2027, 3, 29)))
        assertFalse(t.occursOn(date(2027, 3, 28)))
    }

    @Test
    fun monthly_31st_crossesYearBoundary() {
        val t = task(date(2026, 12, 31), Schedule.Monthly)
        assertTrue(t.occursOn(date(2026, 12, 31)))
        assertTrue(t.occursOn(date(2027, 1, 31)))
        assertFalse(t.occursOn(date(2027, 1, 30)))
    }

    @Test
    fun monthly_15th_doesNotFallOnLastDay() {
        val t = task(date(2026, 10, 15), Schedule.Monthly)
        assertTrue(t.occursOn(date(2026, 11, 15)))
        assertFalse(t.occursOn(date(2026, 11, 30)))
    }

    // --- Rok przestępny ---

    @Test
    fun monthly_31st_fallsOn29FebInLeapYear() {
        val t = task(date(2027, 1, 31), Schedule.Monthly)
        assertTrue(t.occursOn(date(2028, 2, 29)))
        assertFalse(t.occursOn(date(2028, 2, 28)))
    }

    @Test
    fun monthly_29th_fallsOn29FebInLeapYear() {
        val t = task(date(2027, 1, 29), Schedule.Monthly)
        assertTrue(t.occursOn(date(2028, 2, 29)))
        assertFalse(t.occursOn(date(2028, 2, 28)))
    }

    @Test
    fun monthly_startOn29Feb_continuesOn29thOfOtherMonths() {
        val t = task(date(2028, 2, 29), Schedule.Monthly)
        assertTrue(t.occursOn(date(2028, 3, 29)))
        assertTrue(t.occursOn(date(2029, 2, 28)))
        assertTrue(t.occursOn(date(2032, 2, 29)))
        assertFalse(t.occursOn(date(2032, 2, 28)))
    }

    @Test
    fun once_on29Feb_occursOnlyThatDay() {
        val t = task(date(2028, 2, 29), Schedule.Once)
        assertTrue(t.occursOn(date(2028, 2, 29)))
        assertFalse(t.occursOn(date(2029, 2, 28)))
        assertFalse(t.occursOn(date(2032, 2, 29)))
    }

    // --- Własne dni tygodnia ---

    @Test
    fun custom_tuesdayAndWednesday() {
        val t = task(date(2026, 10, 19), Schedule.CustomDays(setOf(TUESDAY, WEDNESDAY))) // start w poniedziałek
        val days = generateSequence(date(2026, 10, 19)) { it.plusDays(1) }
            .take(21)
            .filter { t.occursOn(it) }
            .toList()
        assertEquals(
            listOf(
                date(2026, 10, 20), date(2026, 10, 21),
                date(2026, 10, 27), date(2026, 10, 28),
                date(2026, 11, 3), date(2026, 11, 4),
            ),
            days,
        )
    }

    @Test
    fun custom_notBeforeStartDate() {
        val t = task(date(2026, 10, 21), Schedule.CustomDays(setOf(TUESDAY)))
        assertFalse(t.occursOn(date(2026, 10, 20))) // wtorek przed startem
        assertTrue(t.occursOn(date(2026, 10, 27)))
    }

    @Test
    fun custom_emptySet_neverOccurs() {
        val t = task(date(2026, 10, 21), Schedule.CustomDays(emptySet()))
        assertTrue(t.daysIn(2027).isEmpty())
    }
}
