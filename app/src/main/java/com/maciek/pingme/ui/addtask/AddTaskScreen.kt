package com.maciek.pingme.ui.addtask

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maciek.pingme.R
import com.maciek.pingme.ui.PolishLocale
import com.maciek.pingme.ui.formatDate
import com.maciek.pingme.ui.formatTime
import com.maciek.pingme.ui.theme.PingMeTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.TextStyle

@Composable
fun AddTaskScreen(onDone: () -> Unit, viewModel: AddTaskViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    LaunchedEffect(saved) { if (saved) onDone() }
    AddTaskContent(form, onChange = viewModel::update, onSave = viewModel::save, onBack = onDone)
}

private enum class TimeField { REMINDER, DEADLINE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskContent(
    form: TaskForm,
    onChange: ((TaskForm) -> TaskForm) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val today = LocalDate.now()
    val errors = form.validate(today)
    var timeDialog by remember { mutableStateOf<TimeField?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_task)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { name -> onChange { it.copy(name = name) } },
                label = { Text(stringResource(R.string.task_name)) },
                placeholder = { Text(stringResource(R.string.task_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            ListItem(
                modifier = Modifier.clickable { timeDialog = TimeField.REMINDER },
                headlineContent = { Text(stringResource(R.string.reminder_time)) },
                trailingContent = { Text(form.reminderTime.formatTime(), style = MaterialTheme.typography.titleMedium) },
            )
            ListItem(
                modifier = Modifier.clickable { onChange { it.copy(hasDeadline = !it.hasDeadline) } },
                headlineContent = { Text(stringResource(R.string.deadline)) },
                supportingContent = { Text(stringResource(R.string.deadline_description)) },
                trailingContent = { Switch(checked = form.hasDeadline, onCheckedChange = null) },
            )
            if (form.hasDeadline) {
                ListItem(
                    modifier = Modifier.clickable { timeDialog = TimeField.DEADLINE },
                    headlineContent = { Text(stringResource(R.string.deadline_time)) },
                    trailingContent = { Text(form.deadline.formatTime(), style = MaterialTheme.typography.titleMedium) },
                )
                if (FormError.DEADLINE_NOT_AFTER_REMINDER in errors) ErrorText(R.string.error_deadline)
            }

            Text(
                stringResource(R.string.schedule),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            Column(Modifier.selectableGroup()) {
                ScheduleOption.entries.forEach { option ->
                    val selected = form.scheduleOption == option
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected, role = Role.RadioButton) { onChange { it.copy(scheduleOption = option) } }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(stringResource(option.label), Modifier.padding(start = 12.dp))
                    }
                }
            }
            if (form.scheduleOption == ScheduleOption.CUSTOM) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in form.customDays
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onChange { it.copy(customDays = if (selected) it.customDays - day else it.customDays + day) }
                            },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, PolishLocale)) },
                        )
                    }
                }
                if (FormError.NO_DAYS_SELECTED in errors) ErrorText(R.string.error_no_days)
            }
            scheduleHint(form)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            ListItem(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { showDateDialog = true },
                headlineContent = {
                    Text(stringResource(if (form.scheduleOption == ScheduleOption.ONCE) R.string.date else R.string.start_date))
                },
                trailingContent = { Text(form.startDate.formatDate(), style = MaterialTheme.typography.titleMedium) },
            )
            if (FormError.DATE_IN_PAST in errors) ErrorText(R.string.error_past_date)

            Button(
                onClick = onSave,
                enabled = errors.isEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    timeDialog?.let { field ->
        TimeDialog(
            initial = if (field == TimeField.REMINDER) form.reminderTime else form.deadline,
            onDismiss = { timeDialog = null },
            onConfirm = { time ->
                onChange { if (field == TimeField.REMINDER) it.copy(reminderTime = time) else it.copy(deadline = time) }
                timeDialog = null
            },
        )
    }
    if (showDateDialog) {
        DateDialog(
            initial = form.startDate,
            minDate = today,
            onDismiss = { showDateDialog = false },
            onConfirm = { date ->
                onChange { it.copy(startDate = date) }
                showDateDialog = false
            },
        )
    }
}

@get:StringRes
private val ScheduleOption.label: Int
    get() = when (this) {
        ScheduleOption.ONCE -> R.string.schedule_once
        ScheduleOption.DAILY -> R.string.schedule_daily
        ScheduleOption.WEEKLY -> R.string.schedule_weekly
        ScheduleOption.MONTHLY -> R.string.schedule_monthly
        ScheduleOption.CUSTOM -> R.string.schedule_custom
    }

/** Podpowiedź, w które dni będzie wypadać zadanie tygodniowe lub miesięczne. */
@Composable
private fun scheduleHint(form: TaskForm): String? {
    val day = form.startDate
    return when (form.scheduleOption) {
        ScheduleOption.WEEKLY -> stringResource(R.string.hint_weekly, day.dayOfWeek.getDisplayName(TextStyle.FULL, PolishLocale))
        ScheduleOption.MONTHLY ->
            if (day.dayOfMonth > 28) stringResource(R.string.hint_monthly_short, day.dayOfMonth)
            else stringResource(R.string.hint_monthly, day.dayOfMonth)
        else -> null
    }
}

@Composable
private fun ErrorText(@StringRes text: Int) {
    Text(
        stringResource(text),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state) },
    )
}

/** DatePicker operuje na milisekundach od 1970 r. liczonych w strefie UTC. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(initial: LocalDate, minDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val minMillis = minDate.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onConfirm(it.toLocalDate()) } ?: onDismiss() }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(state)
    }
}

@Preview(showBackground = true)
@Composable
private fun AddTaskContentPreview() {
    PingMeTheme {
        AddTaskContent(
            TaskForm(name = "Suplementy", hasDeadline = true, scheduleOption = ScheduleOption.CUSTOM, startDate = LocalDate.now()),
            onChange = {}, onSave = {}, onBack = {},
        )
    }
}
