package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val taskTag: String,
    val treeSpeciesId: String,
    val isCompleted: Boolean,
    val xpEarned: Int,
    val dateString: String, // Format "yyyy-MM-dd"
    val note: String = ""
)
