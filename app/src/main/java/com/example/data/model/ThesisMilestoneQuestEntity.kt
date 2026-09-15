package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thesis_quests")
data class ThesisMilestoneQuestEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "SESSIONS", "MINUTES", "STREAK", "CATEGORY"
    val targetValue: Int,
    val currentValue: Int = 0,
    val xpReward: Int,
    val isClaimed: Boolean = false,
    val iconName: String = "auto_awesome"
) {
    val isCompleted: Boolean
        get() = currentValue >= targetValue
}
