package com.maciek.pingme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.data.reminder.ReminderScheduler
import com.maciek.pingme.ui.PingMeRoot
import com.maciek.pingme.ui.theme.PingMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: TaskRepository
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PingMeTheme {
                PingMeRoot()
            }
        }
    }

    /**
     * Przy każdym powrocie do aplikacji, niezależnie od otwartej zakładki: uzupełnia pominięcia
     * (żeby statystyki i serie były aktualne) i odnawia alarmy na wypadek, gdyby system je usunął.
     */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            repository.fillMissedOccurrences(LocalDate.now())
            scheduler.scheduleAll()
        }
    }
}
