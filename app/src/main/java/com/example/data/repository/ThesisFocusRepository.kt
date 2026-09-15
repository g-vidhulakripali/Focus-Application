package com.example.data.repository

import com.example.data.dao.FocusSessionDao
import com.example.data.dao.QuestDao
import com.example.data.dao.UserRpgProfileDao
import com.example.data.model.FocusSessionEntity
import com.example.data.model.ThesisMilestoneQuestEntity
import com.example.data.model.ThesisTaskTag
import com.example.data.model.TreeSpecies
import com.example.data.model.UserRpgProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ThesisFocusRepository(
    private val sessionDao: FocusSessionDao,
    private val profileDao: UserRpgProfileDao,
    private val questDao: QuestDao
) {
    val allSessions: Flow<List<FocusSessionEntity>> = sessionDao.getAllSessions()
    val completedSessions: Flow<List<FocusSessionEntity>> = sessionDao.getCompletedSessions()
    val userProfile: Flow<UserRpgProfileEntity?> = profileDao.getProfile()
    val allQuests: Flow<List<ThesisMilestoneQuestEntity>> = questDao.getAllQuests()

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getSessionsForDate(dateStr: String): Flow<List<FocusSessionEntity>> {
        return sessionDao.getSessionsForDate(dateStr)
    }

    fun getMinutesForDate(dateStr: String): Flow<Int?> {
        return sessionDao.getTotalMinutesForDate(dateStr)
    }

    suspend fun initializeDefaultsIfNeeded() {
        // Init profile if none
        val existingProfile = profileDao.getProfileOnce()
        if (existingProfile == null) {
            profileDao.insertOrUpdate(
                UserRpgProfileEntity(
                    id = 1,
                    scholarLevel = 1,
                    currentXp = 0,
                    dailyGoalMinutes = 120,
                    currentStreak = 0,
                    bestStreak = 0,
                    lastStreakDate = "",
                    totalFocusMinutes = 0,
                    scholarTitle = UserRpgProfileEntity.titleForLevel(1),
                    manaPoints = 100
                )
            )
        }

        // Init default quests
        val defaultQuests = listOf(
            ThesisMilestoneQuestEntity(
                id = "first_words",
                title = "The First Inscription",
                description = "Complete your first thesis focus session and plant a tree.",
                category = "SESSIONS",
                targetValue = 1,
                currentValue = 0,
                xpReward = 100,
                iconName = "auto_awesome"
            ),
            ThesisMilestoneQuestEntity(
                id = "deep_dive",
                title = "Deep Scholar Sprint",
                description = "Complete a focus session lasting 45 minutes or longer.",
                category = "DURATION",
                targetValue = 45,
                currentValue = 0,
                xpReward = 150,
                iconName = "timer"
            ),
            ThesisMilestoneQuestEntity(
                id = "lit_master",
                title = "Literature Review Immersion",
                description = "Accumulate 60 minutes dedicated to Literature Review.",
                category = "CATEGORY",
                targetValue = 60,
                currentValue = 0,
                xpReward = 200,
                iconName = "menu_book"
            ),
            ThesisMilestoneQuestEntity(
                id = "daily_champion",
                title = "Thesis Daily Champion",
                description = "Reach your daily thesis goal today and fortify your streak.",
                category = "STREAK",
                targetValue = 1,
                currentValue = 0,
                xpReward = 250,
                iconName = "local_fire_department"
            ),
            ThesisMilestoneQuestEntity(
                id = "tri_streak",
                title = "Academic Momentum",
                description = "Maintain a 3-day thesis focus streak.",
                category = "STREAK",
                targetValue = 3,
                currentValue = 0,
                xpReward = 350,
                iconName = "trending_up"
            ),
            ThesisMilestoneQuestEntity(
                id = "grove_custodian",
                title = "Grove Custodian",
                description = "Successfully grow 5 trees in your Thesis Grove.",
                category = "SESSIONS",
                targetValue = 5,
                currentValue = 0,
                xpReward = 300,
                iconName = "park"
            ),
            ThesisMilestoneQuestEntity(
                id = "dissertation_titan",
                title = "Dissertation Titan",
                description = "Accumulate 300 total focused minutes on your thesis.",
                category = "MINUTES",
                targetValue = 300,
                currentValue = 0,
                xpReward = 500,
                iconName = "military_tech"
            )
        )
        questDao.insertDefaultQuests(defaultQuests)
    }

    suspend fun recordSession(
        startTime: Long,
        endTime: Long,
        durationMinutes: Int,
        taskTag: String,
        treeSpeciesId: String,
        isCompleted: Boolean,
        notes: String = ""
    ): FocusSessionEntity {
        val todayStr = getTodayDateString()
        val species = TreeSpecies.fromId(treeSpeciesId)

        // XP calculation: 10 XP per minute + bonus for completion with tree multiplier
        val baseXp = if (isCompleted) {
            val completionBonus = 50
            ((durationMinutes * 10 + completionBonus) * species.xpMultiplier).toInt()
        } else {
            // Withered: partial XP only for time spent
            (durationMinutes * 3).coerceAtLeast(5)
        }

        val session = FocusSessionEntity(
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes,
            taskTag = taskTag,
            treeSpeciesId = treeSpeciesId,
            isCompleted = isCompleted,
            xpEarned = baseXp,
            dateString = todayStr,
            note = notes
        )
        sessionDao.insertSession(session)

        // Update profile stats & level
        updateProfileAfterSession(baseXp, durationMinutes, isCompleted)

        // Update quests progress
        updateQuestsProgress()

        return session
    }

    private suspend fun updateProfileAfterSession(
        earnedXp: Int,
        durationMinutes: Int,
        isCompleted: Boolean
    ) {
        val currentProfile = profileDao.getProfileOnce() ?: return
        var level = currentProfile.scholarLevel
        var currentXp = currentProfile.currentXp + earnedXp
        var xpRequired = currentProfile.xpRequiredForNextLevel()

        // Level up loop
        while (currentXp >= xpRequired) {
            currentXp -= xpRequired
            level += 1
            xpRequired = level * 180 + 100
        }

        val totalMinutes = currentProfile.totalFocusMinutes + if (isCompleted) durationMinutes else 0
        val newTitle = UserRpgProfileEntity.titleForLevel(level)

        // Streak check
        val todayStr = getTodayDateString()
        val yesterdayStr = getYesterdayDateString()
        val todayMinutes = sessionDao.getTotalMinutesForDate(todayStr).firstOrNull() ?: 0
        val goalMinutes = currentProfile.dailyGoalMinutes

        var streak = currentProfile.currentStreak
        var bestStreak = currentProfile.bestStreak
        var lastStreakDate = currentProfile.lastStreakDate

        if (todayMinutes >= goalMinutes && lastStreakDate != todayStr) {
            // Achieved goal for today!
            if (lastStreakDate == yesterdayStr) {
                streak += 1
            } else if (lastStreakDate.isEmpty()) {
                streak = 1
            } else {
                streak = 1
            }
            lastStreakDate = todayStr
            if (streak > bestStreak) {
                bestStreak = streak
            }
        }

        val updatedProfile = currentProfile.copy(
            scholarLevel = level,
            currentXp = currentXp,
            totalFocusMinutes = totalMinutes,
            scholarTitle = newTitle,
            currentStreak = streak,
            bestStreak = bestStreak,
            lastStreakDate = lastStreakDate,
            manaPoints = currentProfile.manaPoints + if (isCompleted) 15 else 2
        )
        profileDao.insertOrUpdate(updatedProfile)
    }

    suspend fun updateDailyGoal(newGoalMinutes: Int) {
        profileDao.updateDailyGoal(newGoalMinutes)
        checkStreakWithNewGoal(newGoalMinutes)
    }

    private suspend fun checkStreakWithNewGoal(goalMinutes: Int) {
        val profile = profileDao.getProfileOnce() ?: return
        val todayStr = getTodayDateString()
        val yesterdayStr = getYesterdayDateString()
        val todayMinutes = sessionDao.getTotalMinutesForDate(todayStr).firstOrNull() ?: 0

        if (todayMinutes >= goalMinutes && profile.lastStreakDate != todayStr) {
            val newStreak = if (profile.lastStreakDate == yesterdayStr) {
                profile.currentStreak + 1
            } else {
                1
            }
            profileDao.insertOrUpdate(
                profile.copy(
                    dailyGoalMinutes = goalMinutes,
                    currentStreak = newStreak,
                    bestStreak = maxOf(profile.bestStreak, newStreak),
                    lastStreakDate = todayStr
                )
            )
        }
    }

    suspend fun claimQuest(questId: String) {
        val quests = questDao.getAllQuestsOnce()
        val targetQuest = quests.find { it.id == questId } ?: return
        if (!targetQuest.isCompleted || targetQuest.isClaimed) return

        questDao.claimQuest(questId)

        // Award XP to profile
        val profile = profileDao.getProfileOnce() ?: return
        var level = profile.scholarLevel
        var currentXp = profile.currentXp + targetQuest.xpReward
        var xpRequired = profile.xpRequiredForNextLevel()

        while (currentXp >= xpRequired) {
            currentXp -= xpRequired
            level += 1
            xpRequired = level * 180 + 100
        }

        profileDao.insertOrUpdate(
            profile.copy(
                scholarLevel = level,
                currentXp = currentXp,
                scholarTitle = UserRpgProfileEntity.titleForLevel(level)
            )
        )
    }

    private suspend fun updateQuestsProgress() {
        val profile = profileDao.getProfileOnce() ?: return
        val completedSessionsList = sessionDao.getCompletedSessions().firstOrNull() ?: emptyList()
        val allSessionsList = sessionDao.getAllSessions().firstOrNull() ?: emptyList()
        val quests = questDao.getAllQuestsOnce()

        for (quest in quests) {
            if (quest.isClaimed) continue

            val currentProgress = when (quest.id) {
                "first_words" -> completedSessionsList.size
                "deep_dive" -> (completedSessionsList.maxOfOrNull { it.durationMinutes } ?: 0)
                "lit_master" -> completedSessionsList
                    .filter { it.taskTag == ThesisTaskTag.LITERATURE_REVIEW.id }
                    .sumOf { it.durationMinutes }
                "daily_champion" -> {
                    val todayMinutes = sessionDao.getTotalMinutesForDate(getTodayDateString()).firstOrNull() ?: 0
                    if (todayMinutes >= profile.dailyGoalMinutes) 1 else 0
                }
                "tri_streak" -> profile.currentStreak
                "grove_custodian" -> completedSessionsList.size
                "dissertation_titan" -> profile.totalFocusMinutes
                else -> quest.currentValue
            }

            if (currentProgress != quest.currentValue) {
                questDao.updateQuest(quest.copy(currentValue = currentProgress))
            }
        }
    }

    suspend fun deleteSession(id: Long) {
        sessionDao.deleteSession(id)
    }
}
