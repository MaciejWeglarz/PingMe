package com.maciek.pingme.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.domain.isDeadlinePassed
import com.maciek.pingme.domain.occursOn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Odbiera odpalone alarmy i kliknięcia przycisków w powiadomieniach. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: Notifier

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val date = if (intent.hasExtra(EXTRA_DATE)) LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, 0)) else LocalDate.now()
        // Receiver ma tylko kilka sekund; goAsync() pozwala dokończyć pracę na bazie w korutynie.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(intent.action, taskId, date)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(action: String?, taskId: Long, date: LocalDate) {
        when (action) {
            ACTION_REMINDER, ACTION_SNOOZE_FIRED, ACTION_DEADLINE -> {
                val task = repository.getTask(taskId) ?: return
                if (!task.archived && task.occursOn(date) && !repository.isDone(taskId, date)) {
                    if (action == ACTION_DEADLINE) {
                        notifier.cancelReminder(taskId)
                        notifier.showAlarm(task, date)
                    } else if (!task.isDeadlinePassed(date, LocalDateTime.now())) {
                        // Po deadlinie dzwoni już alarm, więc zwykłe przypomnienie (np. po drzemce) byłoby dublem.
                        notifier.showReminder(task, date)
                    }
                }
                scheduler.schedule(task)
            }
            ACTION_DONE -> repository.setDone(taskId, date, done = true)
            ACTION_SNOOZE -> {
                notifier.cancelReminder(taskId)
                scheduler.snooze(taskId, date)
            }
            ACTION_DISMISS -> notifier.cancelAlarm(taskId)
        }
    }

    companion object {
        // Alarmy z AlarmManagera
        const val ACTION_REMINDER = "com.maciek.pingme.REMINDER"
        const val ACTION_DEADLINE = "com.maciek.pingme.DEADLINE"
        const val ACTION_SNOOZE_FIRED = "com.maciek.pingme.SNOOZE_FIRED"
        // Przyciski w powiadomieniach i na ekranie alarmu
        const val ACTION_DONE = "com.maciek.pingme.DONE"
        const val ACTION_SNOOZE = "com.maciek.pingme.SNOOZE"
        const val ACTION_DISMISS = "com.maciek.pingme.DISMISS"

        private const val EXTRA_TASK_ID = "taskId"
        private const val EXTRA_DATE = "date"

        fun intent(context: Context, action: String, taskId: Long, date: LocalDate? = null): Intent =
            Intent(context, ReminderReceiver::class.java)
                .setAction(action)
                .setData(taskUri(taskId, action))
                .putExtra(EXTRA_TASK_ID, taskId)
                .apply { date?.let { putExtra(EXTRA_DATE, it.toEpochDay()) } }
    }
}
