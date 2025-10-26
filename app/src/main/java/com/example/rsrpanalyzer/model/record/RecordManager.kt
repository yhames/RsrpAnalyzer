package com.example.rsrpanalyzer.model.record

import android.util.Log
import com.example.rsrpanalyzer.data.model.SignalRecord
import com.example.rsrpanalyzer.data.repository.SignalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RecordManager(private val signalRepository: SignalRepository) {

    private var currentSessionId: Long? = null
    private var sessionName: String? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * 새로운 세션으로 기록 시작
     */
    fun startRecording(sessionName: String) {
        this.sessionName = sessionName
        scope.launch {
            val sessionEntity = signalRepository.createSession(sessionName)
            currentSessionId = sessionEntity.id
            Log.d("RecordManager", "Recording started: $sessionName")
        }
    }

    /**
     * 기록 중지
     */
    fun stopRecording() {
        currentSessionId = null
        sessionName = null
        recordJob?.cancel()
        recordJob = null
        Log.d("RecordManager", "Recording stopped")
    }

    /**
     * 실시간 레코드 저장
     */
    fun recordSignal(record: SignalRecord) {
        val sessionId = currentSessionId ?: return
        scope.launch {
            signalRepository.saveRecord(record.copy(sessionId = sessionId))
        }
    }

    fun getCurrentSessionId(): Long? {
        return currentSessionId
    }
}