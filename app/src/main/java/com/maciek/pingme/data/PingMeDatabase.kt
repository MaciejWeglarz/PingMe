package com.maciek.pingme.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class, OccurrenceEntity::class], version = 1, exportSchema = false)
abstract class PingMeDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun occurrenceDao(): OccurrenceDao
}
