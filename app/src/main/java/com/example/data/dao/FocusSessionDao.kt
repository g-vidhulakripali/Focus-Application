package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 1 ORDER BY startTime DESC")
    fun getCompletedSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE dateString = :dateStr ORDER BY startTime DESC")
    fun getSessionsForDate(dateStr: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE dateString = :dateStr AND isCompleted = 1")
    fun getTotalMinutesForDate(dateStr: String): Flow<Int?>

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE dateString = :dateStr AND isCompleted = 1")
    suspend fun getTotalMinutesForDateSync(dateStr: String): Int?

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE isCompleted = 1")
    fun getTotalMinutesAllTime(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}
