package com.example.rsrpanalyzer.persistence.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SignalRecordEntity::class, SignalSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun signalRecordDao(): SignalRecordDao
    abstract fun signalSessionDao(): SignalSessionDao

    companion object {
        const val DB_NAME = "rsrp_analyzer.db"
    }
}