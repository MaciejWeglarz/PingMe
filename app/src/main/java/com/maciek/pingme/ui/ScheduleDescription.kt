package com.maciek.pingme.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.maciek.pingme.R
import com.maciek.pingme.domain.Schedule
import com.maciek.pingme.domain.Task
import java.time.LocalDate
import java.time.format.TextStyle

/** Np. „Codziennie · 08:00”, „pon., śr. od 05.11.2026 · 07:30”. */
@Composable
fun scheduleDescription(task: Task): String {
    val start = task.startDate
    val schedule = when (val s = task.schedule) {
        Schedule.Once -> stringResource(R.string.desc_once, start.formatDate())
        Schedule.Daily -> stringResource(R.string.schedule_daily)
        Schedule.Weekly -> stringResource(R.string.desc_weekly, start.dayOfWeek.getDisplayName(TextStyle.FULL, PolishLocale))
        Schedule.Monthly -> stringResource(R.string.desc_monthly, start.dayOfMonth)
        is Schedule.CustomDays -> s.days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, PolishLocale) }
    }
    val from = if (task.schedule != Schedule.Once && start > LocalDate.now()) {
        " " + stringResource(R.string.desc_from, start.formatDate())
    } else {
        ""
    }
    return "$schedule$from · ${task.reminderTime.formatTime()}"
}
