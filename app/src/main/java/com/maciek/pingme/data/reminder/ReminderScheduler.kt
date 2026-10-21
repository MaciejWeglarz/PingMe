package com.maciek.pingme.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.maciek.pingme.MainActivity
import com.maciek.pingme.data.PingMeDatabase
import com.maciek.pingme.data.toDomain
import com.maciek.pingme.domain.Task
import com.maciek.pingme.domain.nextTriggerAt
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Ustawia w AlarmManagerze najbliższe przypomnienie i deadline każdego zadania. */
@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: PingMeDatabase,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: Task, now: LocalDateTime = LocalDateTime.now()) {
        setOrCancel(task.id, ReminderReceiver.ACTION_REMINDER, task.nextTriggerAt(task.reminderTime, now))
        setOrCancel(task.id, ReminderReceiver.ACTION_DEADLINE, task.deadline?.let { task.nextTriggerAt(it, now) })
    }

    suspend fun scheduleAll() {
        val now = LocalDateTime.now()
        db.taskDao().getActive().forEach { schedule(it.toDomain(), now) }
    }

    fun cancel(taskId: Long) {
        setOrCancel(taskId, ReminderReceiver.ACTION_REMINDER, null)
        setOrCancel(taskId, ReminderReceiver.ACTION_DEADLINE, null)
        setOrCancel(taskId, ReminderReceiver.ACTION_SNOOZE_FIRED, null)
    }

    /** „Przypomnij za 10 min”. [date] to dzień wystąpienia: drzemka może przejść przez północ. */
    fun snooze(taskId: Long, date: LocalDate) {
        setOrCancel(taskId, ReminderReceiver.ACTION_SNOOZE_FIRED, LocalDateTime.now().plusMinutes(SNOOZE_MINUTES), date)
    }

    private fun setOrCancel(taskId: Long, action: String, at: LocalDateTime?, date: LocalDate? = null) {
        val intent = ReminderReceiver.intent(context, action, taskId, date)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (at == null) {
            alarmManager.cancel(pending)
            return
        }
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        when {
            // Na Androidzie 12 użytkownik może cofnąć zgodę na dokładne alarmy: wtedy alarm przyjdzie z opóźnieniem.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms() ->
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            // Deadline jak budzik: system nigdy go nie opóźnia.
            action == ReminderReceiver.ACTION_DEADLINE ->
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(millis, openAppIntent()), pending)
            else ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val SNOOZE_MINUTES = 10L
    }
}

/** Unikalny adres w intencji sprawia, że każde zadanie i typ alarmu mają osobny PendingIntent. */
internal fun taskUri(taskId: Long, action: String) = "pingme://task/$taskId/$action".toUri()
