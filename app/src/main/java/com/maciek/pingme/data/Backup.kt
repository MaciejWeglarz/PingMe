package com.maciek.pingme.data

import android.content.Context
import android.net.Uri
import com.maciek.pingme.domain.OccurrenceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Cała zawartość bazy: definicje zadań i historia wystąpień. */
data class Backup(val tasks: List<TaskEntity>, val occurrences: List<OccurrenceEntity>)

private const val FORMAT_VERSION = 1

fun Backup.toJson(): String = JSONObject()
    .put("format", FORMAT_VERSION)
    .put("tasks", JSONArray(tasks.map { it.toJson() }))
    .put("occurrences", JSONArray(occurrences.map { it.toJson() }))
    .toString(2)

/** Rzuca wyjątek, jeśli plik nie jest poprawną kopią PingMe. */
fun parseBackup(json: String): Backup {
    val root = JSONObject(json)
    require(root.getInt("format") == FORMAT_VERSION) { "Nieznana wersja pliku" }
    val tasks = root.getJSONArray("tasks").objects().map { it.toTask() }
    val occurrences = root.getJSONArray("occurrences").objects().map { it.toOccurrence() }
    val taskIds = tasks.map { it.id }.toSet()
    require(occurrences.all { it.taskId in taskIds }) { "Historia odwołuje się do nieistniejącego zadania" }
    return Backup(tasks, occurrences)
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

private fun TaskEntity.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("reminderMinutes", reminderMinutes)
    .put("deadlineMinutes", deadlineMinutes ?: JSONObject.NULL)
    .put("startDate", startDate)
    .put("scheduleType", scheduleType)
    .put("customDays", customDays)
    .put("archived", archived)
    .put("checkedUntil", checkedUntil)

private fun JSONObject.toTask() = TaskEntity(
    id = getLong("id"),
    name = getString("name"),
    reminderMinutes = getInt("reminderMinutes"),
    deadlineMinutes = if (isNull("deadlineMinutes")) null else getInt("deadlineMinutes"),
    startDate = getLong("startDate"),
    // valueOf rzuca wyjątek dla nieznanej nazwy, więc zły plik nie trafi do bazy.
    scheduleType = ScheduleType.valueOf(getString("scheduleType")).name,
    customDays = getInt("customDays"),
    archived = getBoolean("archived"),
    checkedUntil = getLong("checkedUntil"),
)

private fun OccurrenceEntity.toJson() = JSONObject()
    .put("taskId", taskId)
    .put("date", date)
    .put("status", status)

private fun JSONObject.toOccurrence() = OccurrenceEntity(
    taskId = getLong("taskId"),
    date = getLong("date"),
    status = OccurrenceStatus.valueOf(getString("status")).name,
)

/** Zapis i odczyt kopii w pliku wybranym przez użytkownika (Storage Access Framework). */
@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: TaskRepository,
) {
    suspend fun export(uri: Uri) {
        val json = repository.exportBackup().toJson()
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(json) }
        }
    }

    suspend fun import(uri: Uri) {
        val json = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        }
        // Najpierw parsowanie: jeśli plik jest zły, obecne dane zostają nietknięte.
        repository.importBackup(parseBackup(json))
        repository.fillMissedOccurrences(LocalDate.now())
    }
}
