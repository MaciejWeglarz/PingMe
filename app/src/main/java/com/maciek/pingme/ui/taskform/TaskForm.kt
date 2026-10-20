package com.maciek.pingme.ui.taskform

import com.maciek.pingme.domain.Schedule
import com.maciek.pingme.domain.Task
import com.maciek.pingme.domain.rebasedCheckedUntil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleOption { ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM }

enum class FormError { EMPTY_NAME, DEADLINE_NOT_AFTER_REMINDER, NO_DAYS_SELECTED, DATE_IN_PAST }

private val DEFAULT_DEADLINE: LocalTime = LocalTime.of(9, 0)

/** Stan formularza dodawania lub edycji zadania. */
data class TaskForm(
    val name: String = "",
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val hasDeadline: Boolean = false,
    val deadline: LocalTime = DEFAULT_DEADLINE,
    val scheduleOption: ScheduleOption = ScheduleOption.DAILY,
    val customDays: Set<DayOfWeek> = emptySet(),
    val startDate: LocalDate,
    /** Data startu edytowanego zadania: można ją zostawić, nawet jeśli jest z przeszłości. */
    val originalStartDate: LocalDate? = null,
)

/** Czy datę można wybrać: od dziś wzwyż albo pierwotna data edytowanego zadania. */
fun TaskForm.isAllowedStartDate(date: LocalDate, today: LocalDate): Boolean =
    date >= today || date == originalStartDate

fun TaskForm.validate(today: LocalDate): Set<FormError> = buildSet {
    if (name.isBlank()) add(FormError.EMPTY_NAME)
    if (hasDeadline && deadline <= reminderTime) add(FormError.DEADLINE_NOT_AFTER_REMINDER)
    if (scheduleOption == ScheduleOption.CUSTOM && customDays.isEmpty()) add(FormError.NO_DAYS_SELECTED)
    if (!isAllowedStartDate(startDate, today)) add(FormError.DATE_IN_PAST)
}

fun Task.toForm() = TaskForm(
    name = name,
    reminderTime = reminderTime,
    hasDeadline = deadline != null,
    deadline = deadline ?: DEFAULT_DEADLINE,
    scheduleOption = when (schedule) {
        Schedule.Once -> ScheduleOption.ONCE
        Schedule.Daily -> ScheduleOption.DAILY
        Schedule.Weekly -> ScheduleOption.WEEKLY
        Schedule.Monthly -> ScheduleOption.MONTHLY
        is Schedule.CustomDays -> ScheduleOption.CUSTOM
    },
    customDays = (schedule as? Schedule.CustomDays)?.days.orEmpty(),
    startDate = startDate,
    originalStartDate = startDate,
)

/** [existing] to edytowane zadanie albo null przy dodawaniu nowego. */
fun TaskForm.toTask(existing: Task?, today: LocalDate) = Task(
    id = existing?.id ?: 0,
    name = name.trim(),
    reminderTime = reminderTime,
    deadline = if (hasDeadline) deadline else null,
    startDate = startDate,
    schedule = when (scheduleOption) {
        ScheduleOption.ONCE -> Schedule.Once
        ScheduleOption.DAILY -> Schedule.Daily
        ScheduleOption.WEEKLY -> Schedule.Weekly
        ScheduleOption.MONTHLY -> Schedule.Monthly
        ScheduleOption.CUSTOM -> Schedule.CustomDays(customDays)
    },
    // Pominięcia liczymy dopiero od daty startu.
    checkedUntil = existing?.let { rebasedCheckedUntil(it.checkedUntil, startDate, today) } ?: startDate.minusDays(1),
)
