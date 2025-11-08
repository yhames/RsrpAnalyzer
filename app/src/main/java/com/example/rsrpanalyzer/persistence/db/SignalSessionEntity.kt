package com.example.rsrpanalyzer.persistence.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "signal_sessions",
    indices = [Index(value = ["sessionName"], unique = true)]
)
data class SignalSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionName: String,
    val createdAt: Long
)
