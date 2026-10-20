package com.maciek.pingme.data

import com.maciek.pingme.domain.Occurrence
import com.maciek.pingme.domain.OccurrenceStatus
import com.maciek.pingme.domain.Schedule
import com.maciek.pingme.domain.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private fun LocalTime.toMinutes(): Int = hour * 60 + minute
private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)

private fun Set<DayOfWeek>.toMask(): Int = fold(0) { mask, day -> mask or (1 shl day.ordinal) }
private fun Int.toDays(): Set<DayOfWeek> = DayOfWeek.entries.filter { this and (1 shl it.ordinal) != 0 }.toSet()

fun Task.toEntity() = TaskEntity(
    id = id,
    name = name,
    reminderMinutes = reminderTime.toMinutes(),
    deadlineMinutes = deadline?.toMinutes(),
    startDate = startDate.toEpochDay(),
    scheduleType = when (schedule) {
        Schedule.Once -> ScheduleType.ONCE
        Schedule.Daily -> ScheduleType.DAILY
        Schedule.Weekly -> ScheduleType.WEEKLY
        Schedule.Monthly -> ScheduleType.MONTHLY
        is Schedule.CustomDays -> ScheduleType.CUSTOM
    }.name,
    customDays = (schedule as? Schedule.CustomDays)?.days?.toMask() ?: 0,
    archived = archived,
    checkedUntil = checkedUntil.toEpochDay(),
)

fun TaskEntity.toDomain() = Task(
    id = id,
    name = name,
    reminderTime = reminderMinutes.toLocalTime(),
    deadline = deadlineMinutes?.toLocalTime(),
    startDate = LocalDate.ofEpochDay(startDate),
    schedule = when (ScheduleType.valueOf(scheduleType)) {
        ScheduleType.ONCE -> Schedule.Once
        ScheduleType.DAILY -> Schedule.Daily
        ScheduleType.WEEKLY -> Schedule.Weekly
        ScheduleType.MONTHLY -> Schedule.Monthly
        ScheduleType.CUSTOM -> Schedule.CustomDays(customDays.toDays())
    },
    archived = archived,
    checkedUntil = LocalDate.ofEpochDay(checkedUntil),
)

fun Occurrence.toEntity() = OccurrenceEntity(taskId, date.toEpochDay(), status.name)

fun OccurrenceEntity.toDomain() =
    Occurrence(taskId, LocalDate.ofEpochDay(date), OccurrenceStatus.valueOf(status))
