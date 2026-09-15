package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_rpg_profile")
data class UserRpgProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val scholarLevel: Int = 1,
    val currentXp: Int = 0,
    val dailyGoalMinutes: Int = 120,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastStreakDate: String = "",
    val totalFocusMinutes: Int = 0,
    val scholarTitle: String = "Novice Thesis Scribe",
    val manaPoints: Int = 100
) {
    fun xpRequiredForNextLevel(): Int {
        // Level curve: e.g. Level 1 -> 150 XP, Level 2 -> 300 XP, etc.
        return scholarLevel * 180 + 100
    }

    companion object {
        fun titleForLevel(level: Int): String = when {
            level <= 1 -> "Novice Thesis Scribe"
            level <= 3 -> "Graduate Apprentice"
            level <= 5 -> "Thesis Researcher"
            level <= 7 -> "Arcane Master Academician"
            level <= 9 -> "Distinguished Scholar"
            level <= 12 -> "Grand Scribe of Epistemology"
            else -> "Doctor of Philosophy (PhD Archmage)"
        }
    }
}
