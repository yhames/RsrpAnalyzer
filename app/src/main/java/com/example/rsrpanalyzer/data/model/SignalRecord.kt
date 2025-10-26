package com.example.rsrpanalyzer.data.model

data class SignalRecord(
    val id: Long,
    val sessionId: Long,
    val timestampMillis: Long,  // Timestamp in milliseconds, ex) 1761481296731 == 2025-10-26T12:22:24+00:00
    val latitude: Double,
    val longitude: Double,
    val rsrp: Int,
    val rsrq: Int,
)