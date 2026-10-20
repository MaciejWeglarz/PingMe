package com.maciek.pingme.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Minuty od północy. */
    val reminderMinutes: Int,
    val deadlineMinutes: Int?,
    /** LocalDate.toEpochDay(). */
    val startDate: Long,
    /** Nazwa z ScheduleType. */
    val scheduleType: String,
    /** Maska bitowa dni tygodnia: bit 0 = poniedziałek ... bit 6 = niedziela. */
    val customDays: Int,
    val archived: Boolean,
    val checkedUntil: Long,
)

enum class ScheduleType { ONCE, DAILY, WEEKLY, MONTHLY, CUSTOM }

@Entity(
    tableName = "occurrences",
    primaryKeys = ["taskId", "date"],
    foreignKeys = [ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"])],
    indices = [Index("taskId")],
)
data class OccurrenceEntity(
    val taskId: Long,
    val date: Long,
    /** Nazwa z OccurrenceStatus. */
    val status: String,
)
