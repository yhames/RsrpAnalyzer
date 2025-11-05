package com.example.rsrpanalyzer.view.signal

data class SignalRecordItem(
    val latitude: Double,
    val longitude: Double,
    val rsrp: Int,
    val rsrq: Int,
)
