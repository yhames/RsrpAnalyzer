package com.example.rsrpanalyzer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalSessionDao {
    // ✅ 새 세션 추가
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: SignalSessionEntity): Long

    // ✅ 세션 이름으로 조회 (unique)
    @Query("SELECT * FROM signal_sessions WHERE sessionName = :name LIMIT 1")
    suspend fun getSessionByName(name: String): SignalSessionEntity?

    // ✅ 모든 세션 조회 (최신순)
    @Query("SELECT * FROM signal_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SignalSessionEntity>>

    // ✅ 세션 삭제 (ON DELETE CASCADE로 기록도 같이 삭제)
    @Query("DELETE FROM signal_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}