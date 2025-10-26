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
    suspend fun insertRecord(record: SignalRecordEntity)

    // ✅ 여러 개 삽입 (배치)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<SignalRecordEntity>)

    // ✅ 세션별 기록 조회 (Flow로 observe 가능)
    @Query("SELECT * FROM signal_records WHERE sessionId = :sessionId ORDER BY startTimestampMillis ASC")
    fun getRecordsBySession(sessionId: Long): Flow<List<SignalRecordEntity>>

    // ✅ 특정 세션 기록 초기화
    @Query("DELETE FROM signal_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsBySession(sessionId: Long)
}