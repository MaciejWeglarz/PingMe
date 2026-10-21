package com.maciek.pingme.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {

    private val backup = Backup(
        tasks = listOf(
            TaskEntity(1, "Mycie zębów „rano”; test", 480, 540, 20_000, "DAILY", 0, false, 20_380),
            TaskEntity(2, "Suplementy", 600, null, 20_100, "CUSTOM", 0b0000110, true, 20_380),
        ),
        occurrences = listOf(
            OccurrenceEntity(1, 20_379, "DONE"),
            OccurrenceEntity(1, 20_380, "SKIPPED"),
            OccurrenceEntity(2, 20_380, "DONE"),
        ),
    )

    @Test
    fun exportThenImport_sameData() {
        assertEquals(backup, parseBackup(backup.toJson()))
    }

    @Test
    fun emptyDatabase_roundTrip() {
        val empty = Backup(emptyList(), emptyList())
        assertEquals(empty, parseBackup(empty.toJson()))
    }

    @Test
    fun notJson_throws() {
        assertThrows(Exception::class.java) { parseBackup("to nie jest kopia") }
    }

    @Test
    fun otherJson_throws() {
        assertThrows(Exception::class.java) { parseBackup("""{"foo": 1}""") }
    }

    @Test
    fun unknownFormatVersion_throws() {
        val json = backup.toJson().replace("\"format\": 1", "\"format\": 99")
        assertThrows(IllegalArgumentException::class.java) { parseBackup(json) }
    }

    @Test
    fun unknownScheduleType_throws() {
        val json = backup.toJson().replace("\"DAILY\"", "\"YEARLY\"")
        assertThrows(IllegalArgumentException::class.java) { parseBackup(json) }
    }

    @Test
    fun unknownStatus_throws() {
        val json = backup.toJson().replace("\"SKIPPED\"", "\"MAYBE\"")
        assertThrows(IllegalArgumentException::class.java) { parseBackup(json) }
    }

    @Test
    fun occurrenceOfMissingTask_throws() {
        val broken = backup.copy(occurrences = backup.occurrences + OccurrenceEntity(7, 20_380, "DONE"))
        assertThrows(IllegalArgumentException::class.java) { parseBackup(broken.toJson()) }
    }
}
