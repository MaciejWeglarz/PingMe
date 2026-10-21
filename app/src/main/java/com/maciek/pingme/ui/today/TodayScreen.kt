package com.maciek.pingme.ui.today

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciek.pingme.R
import com.maciek.pingme.domain.DayTask
import com.maciek.pingme.domain.Mood
import com.maciek.pingme.domain.Schedule
import com.maciek.pingme.domain.Task
import com.maciek.pingme.ui.formatDay
import com.maciek.pingme.ui.formatTime
import com.maciek.pingme.ui.theme.PingMeTheme
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(onAddClick: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    TodayContent(state, onAddClick, onDoneChange = viewModel::setDone)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayContent(
    state: TodayState,
    onAddClick: () -> Unit,
    onDoneChange: (taskId: Long, done: Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.today_title))
                    Text(state.date.formatDay(), style = MaterialTheme.typography.bodyMedium)
                }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add_task))
            }
        },
    ) { padding ->
        val tasks = state.tasks ?: return@Scaffold
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(state.mood.drawable()), contentDescription = null, modifier = Modifier.size(160.dp))
                    Text(stringResource(R.string.today_empty), style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(contentPadding = padding) {
                item {
                    MascotHeader(state.mood, remaining = tasks.count { !it.done })
                }
                items(tasks, key = { it.task.id }) { item ->
                    TaskRow(item, onDoneChange = { onDoneChange(item.task.id, it) })
                }
            }
        }
    }
}

private fun Mood.drawable() = when (this) {
    Mood.HAPPY -> R.drawable.mascot_happy
    Mood.NEUTRAL -> R.drawable.mascot_neutral
    Mood.SAD -> R.drawable.mascot_sad
}

@Composable
private fun MascotHeader(mood: Mood, remaining: Int) {
    val message = when (mood) {
        Mood.HAPPY -> stringResource(R.string.mood_happy)
        Mood.NEUTRAL -> stringResource(R.string.mood_neutral, remaining)
        Mood.SAD -> stringResource(R.string.mood_sad)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(painterResource(mood.drawable()), contentDescription = null, modifier = Modifier.size(80.dp))
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = if (mood == Mood.HAPPY) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun TaskRow(item: DayTask, onDoneChange: (Boolean) -> Unit) {
    val task = item.task
    val time = buildString {
        append(task.reminderTime.formatTime())
        task.deadline?.let { append(" · ").append(stringResource(R.string.today_deadline, it.formatTime())) }
        item.streak?.takeIf { it > 0 }?.let { append(" · ").append(stringResource(R.string.streak, it)) }
    }
    ListItem(
        modifier = Modifier
            .toggleable(value = item.done, role = Role.Checkbox, onValueChange = onDoneChange)
            .alpha(if (item.done) 0.5f else 1f),
        leadingContent = { Checkbox(checked = item.done, onCheckedChange = null) },
        headlineContent = {
            Text(task.name, textDecoration = if (item.done) TextDecoration.LineThrough else null)
        },
        supportingContent = { Text(time) },
    )
}

@Preview(showBackground = true)
@Composable
private fun TodayContentPreview() {
    fun task(id: Long, name: String, hour: Int, deadline: Int?) = Task(
        id = id, name = name, reminderTime = LocalTime.of(hour, 0), deadline = deadline?.let { LocalTime.of(it, 0) },
        startDate = LocalDate.now(), schedule = Schedule.Daily, checkedUntil = LocalDate.now(),
    )
    PingMeTheme {
        TodayContent(
            TodayState(
                LocalDate.now(),
                listOf(
                    DayTask(task(1, "Mycie zębów rano", 7, null), done = true, streak = 12),
                    DayTask(task(2, "Suplementy", 9, 10), done = false, streak = 0),
                ),
                Mood.SAD,
            ),
            onAddClick = {},
            onDoneChange = { _, _ -> },
        )
    }
}
