package com.maciek.pingme.ui.addtask

import com.maciek.pingme.domain.Schedule
import com.maciek.pingme.domain.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleOption { ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM }

enum class FormError { EMPTY_NAME, DEADLINE_NOT_AFTER_REMINDER, NO_DAYS_SELECTED, DATE_IN_PAST }

/** Stan formularza dodawania zadania. */
data class TaskForm(
    val name: String = "",
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val hasDeadline: Boolean = false,
    val deadline: LocalTime = LocalTime.of(9, 0),
    val scheduleOption: ScheduleOption = ScheduleOption.DAILY,
    val customDays: Set<DayOfWeek> = emptySet(),
    val startDate: LocalDate,
)

fun TaskForm.validate(today: LocalDate): Set<FormError> = buildSet {
    if (name.isBlank()) add(FormError.EMPTY_NAME)
    if (hasDeadline && deadline <= reminderTime) add(FormError.DEADLINE_NOT_AFTER_REMINDER)
    if (scheduleOption == ScheduleOption.CUSTOM && customDays.isEmpty()) add(FormError.NO_DAYS_SELECTED)
    if (startDate < today) add(FormError.DATE_IN_PAST)
}

fun TaskForm.toTask() = Task(
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
    checkedUntil = startDate.minusDays(1),
)
