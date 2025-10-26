package com.example.rsrpanalyzer.data.repository

import com.example.rsrpanalyzer.data.db.SignalRecordDao
import com.example.rsrpanalyzer.data.db.SignalRecordEntity
import com.example.rsrpanalyzer.data.db.SignalSessionDao
import com.example.rsrpanalyzer.data.db.SignalSessionEntity
import com.example.rsrpanalyzer.data.model.SignalRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SignalRepository(
    private val sessionDao: SignalSessionDao,
    private val recordDao: SignalRecordDao
) {
    // 새로운 세션 생성
    suspend fun createSession(sessionName: String): SignalSessionEntity =
        withContext(Dispatchers.IO) {
            val session = SignalSessionEntity(
                sessionName = sessionName, createdAt = System.currentTimeMillis()
            )
            val id = sessionDao.save(session)
            session.copy(id = id)
        }

    // 세션 조회
    suspend fun getSessionByName(sessionName: String): SignalSessionEntity? =
        withContext(Dispatchers.IO) {
            sessionDao.findSessionByName(sessionName)
        }

    // 레코드 저장
    suspend fun saveRecord(record: SignalRecord) = withContext(Dispatchers.IO) {
        val entity = SignalRecordEntity(
            sessionId = record.sessionId,
            timestampMillis = record.timestampMillis,
            latitude = record.latitude,
            longitude = record.longitude,
            rsrp = record.rsrp,
            rsrq = record.rsrq
        )
        recordDao.save(entity)
    }

    // 세션 내 모든 레코드 조회 (List)
    suspend fun findRecordsForSession(record: SignalRecord): List<SignalRecordEntity> =
        withContext(Dispatchers.IO) {
            recordDao.findAllBySessionId(record.sessionId)
        }

    // 세션 내 모든 레코드 조회 (Flow)
    fun observeRecordsForSession(sessionId: Long): Flow<List<SignalRecord>> {
        return recordDao.observeRecordsBySessionId(sessionId).map { entities ->
            entities.map { entity ->
                SignalRecord(
                    id = entity.id,
                    sessionId = entity.sessionId,
                    timestampMillis = entity.timestampMillis,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    rsrp = entity.rsrp,
                    rsrq = entity.rsrq
                )
            }
        }
    }
}