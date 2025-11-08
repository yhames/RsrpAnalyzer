package com.example.rsrpanalyzer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SignalSessionDao {
    // ✅ 새 세션 추가
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun save(session: SignalSessionEntity): Long

    // ✅ 세션 업데이트
    @Update
    suspend fun update(session: SignalSessionEntity)

    // ✅ 세션 id로 조회
    @Query("SELECT * FROM signal_sessions WHERE id = :id LIMIT 1")
    suspend fun findSessionById(id: Long): SignalSessionEntity?

    // ✅ 세션 이름으로 조회 (unique)
    @Query("SELECT * FROM signal_sessions WHERE sessionName = :name LIMIT 1")
    suspend fun findSessionByName(name: String): SignalSessionEntity?

    // ✅ 모든 세션 조회 (최신순)
    @Query("SELECT * FROM signal_sessions ORDER BY createdAt DESC")
    fun findAll(): List<SignalSessionEntity>

    // ✅ 세션 삭제 (ON DELETE CASCADE로 기록도 같이 삭제)
    @Query("DELETE FROM signal_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}