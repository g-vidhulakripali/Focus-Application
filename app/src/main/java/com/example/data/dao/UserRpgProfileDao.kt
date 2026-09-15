package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserRpgProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserRpgProfileDao {
    @Query("SELECT * FROM user_rpg_profile WHERE id = 1")
    fun getProfile(): Flow<UserRpgProfileEntity?>

    @Query("SELECT * FROM user_rpg_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserRpgProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserRpgProfileEntity)

    @Query("UPDATE user_rpg_profile SET dailyGoalMinutes = :goalMinutes WHERE id = 1")
    suspend fun updateDailyGoal(goalMinutes: Int)
}
