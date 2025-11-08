package com.example.rsrpanalyzer.persistence.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "signal_records",
    foreignKeys = [
        ForeignKey(
            entity = SignalSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")] // 성능 향상을 위한 인덱스
)
data class SignalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampMillis: Long,     // epoch millis, ex) 1761481296731 == 2025-10-26T12:22:24+00:00
    val latitude: Double,
    val longitude: Double,
    val rsrp: Int,
    val rsrq: Int
)