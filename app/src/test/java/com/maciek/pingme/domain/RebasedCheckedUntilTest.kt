package com.maciek.pingme.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RebasedCheckedUntilTest {

    private val today = date(2026, 10, 21)
    private val yesterday = date(2026, 10, 20)

    @Test
    fun runningTask_startUnchanged_keepsCheckedUntil() {
        assertEquals(yesterday, rebasedCheckedUntil(yesterday, date(2026, 9, 1), today))
    }

    @Test
    fun futureTask_movedToToday_checksFromToday() {
        // Bez przeliczenia dni 21.10–4.11 nigdy nie zostałyby oznaczone jako pominięte.
        assertEquals(yesterday, rebasedCheckedUntil(date(2026, 11, 4), today, today))
    }

    @Test
    fun futureTask_movedEarlierButStillFuture() {
        assertEquals(date(2026, 10, 24), rebasedCheckedUntil(date(2026, 11, 4), date(2026, 10, 25), today))
    }

    @Test
    fun runningTask_movedToFuture_checksFromNewStart() {
        assertEquals(date(2026, 11, 4), rebasedCheckedUntil(yesterday, date(2026, 11, 5), today))
    }

    @Test
    fun futureTask_startUnchanged_keepsCheckedUntil() {
        assertEquals(date(2026, 11, 4), rebasedCheckedUntil(date(2026, 11, 4), date(2026, 11, 5), today))
    }
}
