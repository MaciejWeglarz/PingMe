package com.maciek.pingme.ui.alltasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciek.pingme.R
import com.maciek.pingme.domain.Schedule
import com.maciek.pingme.domain.Task
import com.maciek.pingme.ui.scheduleDescription
import com.maciek.pingme.ui.theme.PingMeTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AllTasksScreen(
    onAddClick: () -> Unit,
    onTaskClick: (taskId: Long) -> Unit,
    viewModel: AllTasksViewModel = hiltViewModel(),
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    AllTasksContent(tasks, onAddClick, onTaskClick, onDelete = viewModel::delete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllTasksContent(
    tasks: List<Task>?,
    onAddClick: () -> Unit,
    onTaskClick: (taskId: Long) -> Unit,
    onDelete: (taskId: Long) -> Unit,
) {
    var toDelete by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.all_tasks_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add_task))
            }
        },
    ) { padding ->
        if (tasks == null) return@Scaffold
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.all_tasks_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(tasks, key = { it.id }) { task ->
                    ListItem(
                        modifier = Modifier.clickable { onTaskClick(task.id) },
                        headlineContent = { Text(task.name) },
                        supportingContent = { Text(scheduleDescription(task)) },
                        trailingContent = {
                            IconButton(onClick = { toDelete = task }) {
                                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.delete))
                            }
                        },
                    )
                }
            }
        }
    }

    toDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_text, task.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(task.id)
                    toDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AllTasksContentPreview() {
    fun task(id: Long, name: String, schedule: Schedule) = Task(
        id = id, name = name, reminderTime = LocalTime.of(8, 0), deadline = null,
        startDate = LocalDate.now(), schedule = schedule, checkedUntil = LocalDate.now(),
    )
    PingMeTheme {
        AllTasksContent(
            listOf(
                task(1, "Mycie zębów rano", Schedule.Daily),
                task(2, "Suplementy", Schedule.CustomDays(setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))),
                task(3, "Wizyta u dentysty", Schedule.Once),
            ),
            onAddClick = {}, onTaskClick = {}, onDelete = {},
        )
    }
}
