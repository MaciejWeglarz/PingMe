package com.maciek.pingme.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed class Schedule {
    /** Tylko w dniu startDate. */
    data object Once : Schedule()
    data object Daily : Schedule()
    /** W ten sam dzień tygodnia co startDate. */
    data object Weekly : Schedule()
    /** W ten sam dzień miesiąca co startDate (lub ostatni dzień krótszego miesiąca). */
    data object Monthly : Schedule()
    data class CustomDays(val days: Set<DayOfWeek>) : Schedule()
}

data class Task(
    val id: Long = 0,
    val name: String,
    val reminderTime: LocalTime,
    val deadline: LocalTime?,
    val startDate: LocalDate,
    val schedule: Schedule,
    val archived: Boolean = false,
    /** Ostatni dzień, za który uzupełniono już pominięte wystąpienia. */
    val checkedUntil: LocalDate,
)

enum class OccurrenceStatus { DONE, SKIPPED }

data class Occurrence(
    val taskId: Long,
    val date: LocalDate,
    val status: OccurrenceStatus,
)
