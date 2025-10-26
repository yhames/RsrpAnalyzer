package com.example.rsrpanalyzer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalRecordDao {
    // ✅ 단일 기록 삽입
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: SignalRecordEntity)

    // ✅ 세션별 기록 조회 (Flow)
    @Query("SELECT * FROM signal_records WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun observeRecordsBySessionId(sessionId: Long): Flow<List<SignalRecordEntity>>

    // ✅ 세션별 기록 조회
    @Query("SELECT * FROM signal_records WHERE sessionId = :sessionId ORDER BY timestampMillis ASC")
    fun findAllBySessionId(sessionId: Long): List<SignalRecordEntity>

    // ✅ 특정 세션 기록 초기화
    @Query("DELETE FROM signal_records WHERE sessionId = :sessionId")
    suspend fun deleteAllBySessionId(sessionId: Long)
}