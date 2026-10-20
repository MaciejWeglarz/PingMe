package com.maciek.pingme.data.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.maciek.pingme.MainActivity
import com.maciek.pingme.R
import com.maciek.pingme.domain.Task
import com.maciek.pingme.ui.alarm.AlarmActivity
import com.maciek.pingme.ui.formatTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Pokazuje i chowa powiadomienia o przypomnieniach i alarmach. */
@Singleton
class Notifier @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannels() {
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH,
        )
        val alarms = NotificationChannel(
            CHANNEL_ALARMS, context.getString(R.string.channel_alarms), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            enableVibration(true)
        }
        manager.createNotificationChannels(listOf(reminders, alarms))
    }

    fun showReminder(task: Task, date: LocalDate) {
        val text = task.deadline?.let { context.getString(R.string.today_deadline, it.formatTime()) }
            ?: context.getString(R.string.notification_reminder_text)
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.name)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_done), actionIntent(ReminderReceiver.ACTION_DONE, task.id, date))
            .addAction(0, context.getString(R.string.action_snooze), actionIntent(ReminderReceiver.ACTION_SNOOZE, task.id, date))
            .build()
        manager.notify(TAG_REMINDER, task.id.toInt(), notification)
    }

    fun showAlarm(task: Task, date: LocalDate) {
        val deadline = task.deadline?.formatTime().orEmpty()
        val fullScreen = PendingIntent.getActivity(
            context, 0, AlarmActivity.intent(context, task, date), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.name)
            .setContentText(context.getString(R.string.alarm_text, deadline))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, context.getString(R.string.action_done), actionIntent(ReminderReceiver.ACTION_DONE, task.id, date))
            .addAction(0, context.getString(R.string.action_dismiss), actionIntent(ReminderReceiver.ACTION_DISMISS, task.id, date))
            .build()
        // Dźwięk powtarza się, dopóki powiadomienie nie zniknie.
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        manager.notify(TAG_ALARM, task.id.toInt(), notification)
    }

    fun cancelReminder(taskId: Long) = manager.cancel(TAG_REMINDER, taskId.toInt())

    fun cancelAlarm(taskId: Long) = manager.cancel(TAG_ALARM, taskId.toInt())

    fun cancelAll(taskId: Long) {
        cancelReminder(taskId)
        cancelAlarm(taskId)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    private fun actionIntent(action: String, taskId: Long, date: LocalDate): PendingIntent = PendingIntent.getBroadcast(
        context, 0, ReminderReceiver.intent(context, action, taskId, date),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_ALARMS = "alarms"
        const val TAG_REMINDER = "reminder"
        const val TAG_ALARM = "alarm"
    }
}
