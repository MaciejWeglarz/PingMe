package com.maciek.pingme.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciek.pingme.R
import com.maciek.pingme.domain.Schedule
import com.maciek.pingme.domain.Task
import com.maciek.pingme.domain.TaskStats
import com.maciek.pingme.ui.formatMonth
import com.maciek.pingme.ui.theme.PingMeTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsContent(state, onPrevious = viewModel::previousMonth, onNext = viewModel::nextMonth)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsContent(state: StatsState?, onPrevious: () -> Unit, onNext: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) }) { padding ->
        if (state == null) return@Scaffold
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            item {
                MonthSwitcher(state, onPrevious, onNext)
            }
            if (state.stats.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.stats_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                    )
                }
            }
            items(state.stats, key = { it.task.id }) { StatsCard(it) }
        }
    }
}

@Composable
private fun MonthSwitcher(state: StatsState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.previous_month))
        }
        Text(
            state.month.formatMonth(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = state.canGoNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.next_month))
        }
    }
}

@Composable
private fun StatsCard(stats: TaskStats) {
    val total = stats.done + stats.skipped
    val ratio = if (total == 0) 0f else stats.done.toFloat() / total
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stats.task.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (stats.task.archived) {
                    Text(
                        stringResource(R.string.stats_archived),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                stats.streak?.takeIf { it > 0 }?.let {
                    Text(stringResource(R.string.streak, it), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                stringResource(R.string.stats_counts, stats.done, stats.skipped),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { ratio }, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.stats_percent, (ratio * 100).toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsContentPreview() {
    fun task(id: Long, name: String, archived: Boolean = false) = Task(
        id = id, name = name, reminderTime = LocalTime.of(8, 0), deadline = null, startDate = LocalDate.now(),
        schedule = Schedule.Daily, archived = archived, checkedUntil = LocalDate.now(),
    )
    PingMeTheme {
        StatsContent(
            StatsState(
                YearMonth.now(),
                listOf(
                    TaskStats(task(1, "Mycie zębów rano"), done = 18, skipped = 2, streak = 7),
                    TaskStats(task(2, "Suplementy"), done = 10, skipped = 10, streak = 0),
                    TaskStats(task(3, "Stare zadanie", archived = true), done = 3, skipped = 1, streak = null),
                ),
            ),
            onPrevious = {}, onNext = {},
        )
    }
}
