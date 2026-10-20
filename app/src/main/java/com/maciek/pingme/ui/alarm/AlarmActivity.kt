package com.maciek.pingme.ui.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.maciek.pingme.R
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.data.reminder.Notifier
import com.maciek.pingme.domain.Task
import com.maciek.pingme.ui.formatTime
import com.maciek.pingme.ui.theme.PingMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Pełnoekranowy alarm po minięciu deadline'u. Pokazuje się także na zablokowanym ekranie. */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var notifier: Notifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val deadline = intent.getStringExtra(EXTRA_DEADLINE).orEmpty()

        setContent {
            PingMeTheme {
                AlarmContent(
                    name = name,
                    deadline = deadline,
                    onDone = {
                        lifecycleScope.launch {
                            repository.setDone(taskId, date, done = true)
                            finish()
                        }
                    },
                    onDismiss = {
                        notifier.cancelAlarm(taskId)
                        finish()
                    },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    companion object {
        private const val EXTRA_TASK_ID = "taskId"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_DEADLINE = "deadline"

        fun intent(context: Context, task: Task, date: LocalDate): Intent =
            Intent(context, AlarmActivity::class.java)
                .setData("pingme://alarm/${task.id}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(EXTRA_TASK_ID, task.id)
                .putExtra(EXTRA_DATE, date.toEpochDay())
                .putExtra(EXTRA_NAME, task.name)
                .putExtra(EXTRA_DEADLINE, task.deadline?.formatTime())
    }
}

@Composable
private fun AlarmContent(name: String, deadline: String, onDone: () -> Unit, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            Modifier.padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.alarm_title), style = MaterialTheme.typography.titleMedium)
            Text(
                name,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Text(stringResource(R.string.alarm_text, deadline), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp)
                    .height(64.dp),
            ) {
                Text(stringResource(R.string.action_done), style = MaterialTheme.typography.titleLarge)
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.action_dismiss), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview
@Composable
private fun AlarmContentPreview() {
    PingMeTheme {
        AlarmContent(name = "Leki", deadline = "9:00", onDone = {}, onDismiss = {})
    }
}
