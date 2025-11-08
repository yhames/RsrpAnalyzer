package com.example.rsrpanalyzer.view.history

data class SessionItem(
    val id: Long,
    val sessionName: String,
    val createdAt: Long,
    val recordCount: Int
)
