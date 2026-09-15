package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ThesisMilestoneQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM thesis_quests")
    fun getAllQuests(): Flow<List<ThesisMilestoneQuestEntity>>

    @Query("SELECT * FROM thesis_quests")
    suspend fun getAllQuestsOnce(): List<ThesisMilestoneQuestEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultQuests(quests: List<ThesisMilestoneQuestEntity>)

    @Update
    suspend fun updateQuest(quest: ThesisMilestoneQuestEntity)

    @Query("UPDATE thesis_quests SET isClaimed = 1 WHERE id = :questId")
    suspend fun claimQuest(questId: String)
}
