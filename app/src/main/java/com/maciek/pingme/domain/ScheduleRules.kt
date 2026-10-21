package com.maciek.pingme.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

/** Zadanie cykliczne wypada co najmniej raz na tyle dni (z zapasem dla miesięcznych). */
private const val MAX_DAYS_BETWEEN_OCCURRENCES = 62

/**
 * Najbliższy moment po [now], w którym zadanie wypada o godzinie [time].
 * null, jeśli takiego terminu nie ma (np. jednorazowe z przeszłości).
 */
fun Task.nextTriggerAt(time: LocalTime, now: LocalDateTime): LocalDateTime? {
    if (archived) return null
    if (schedule == Schedule.Once) return startDate.atTime(time).takeIf { it > now }
    var date = maxOf(now.toLocalDate(), startDate)
    repeat(MAX_DAYS_BETWEEN_OCCURRENCES) {
        if (occursOn(date)) {
            val at = date.atTime(time)
            if (at > now) return at
        }
        date = date.plusDays(1)
    }
    return null
}

/** Czy minął deadline wystąpienia z dnia [date]. Zadanie bez deadline'u nigdy go nie przekracza. */
fun Task.isDeadlinePassed(date: LocalDate, now: LocalDateTime): Boolean =
    deadline?.let { now >= date.atTime(it) } ?: false

/**
 * Nowe checkedUntil po zmianie daty startu w edycji: dni już sprawdzone (do wczoraj) zostają sprawdzone,
 * a pominięcia liczymy najwcześniej od nowej daty startu.
 */
fun rebasedCheckedUntil(oldCheckedUntil: LocalDate, newStartDate: LocalDate, today: LocalDate): LocalDate =
    maxOf(minOf(oldCheckedUntil, today.minusDays(1)), newStartDate.minusDays(1))

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
