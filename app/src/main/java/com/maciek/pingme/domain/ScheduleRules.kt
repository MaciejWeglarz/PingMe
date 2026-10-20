package com.maciek.pingme.domain

import java.time.LocalDate

/** Czy zadanie wypada w podanym dniu. */
fun Task.occursOn(date: LocalDate): Boolean {
    if (date < startDate) return false
    return when (schedule) {
        Schedule.Once -> date == startDate
        Schedule.Daily -> true
        Schedule.Weekly -> date.dayOfWeek == startDate.dayOfWeek
        Schedule.Monthly -> date.dayOfMonth == minOf(startDate.dayOfMonth, date.lengthOfMonth())
        is Schedule.CustomDays -> date.dayOfWeek in schedule.days
    }
}

/**
 * Dni od checkedUntil+1 do wczoraj, w które zadanie wypadało, a nie zostało zrobione.
 * Te dni trzeba zapisać jako pominięte.
 */
fun Task.missedDates(today: LocalDate, done: Set<LocalDate>): List<LocalDate> {
    if (archived) return emptyList()
    return generateSequence(checkedUntil.plusDays(1)) { it.plusDays(1) }
        .takeWhile { it < today }
        .filter { occursOn(it) && it !in done }
        .toList()
}
