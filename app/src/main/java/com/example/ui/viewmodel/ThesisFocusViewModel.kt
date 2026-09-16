package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.ThesisMilestoneQuestEntity
import com.example.data.model.ThesisTaskTag
import com.example.data.model.TreeSpecies
import com.example.data.model.UserRpgProfileEntity
import com.example.data.repository.ThesisFocusRepository
import com.example.service.FocusTimerEngine
import com.example.service.TimerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayFocusStat(
    val dateString: String,
    val dayLabel: String, // e.g. "Mon", "Tue"
    val totalMinutes: Int,
    val goalMinutes: Int,
    val isGoalAchieved: Boolean
)

class ThesisFocusViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ThesisFocusRepository(
        sessionDao = database.focusSessionDao(),
        profileDao = database.userRpgProfileDao(),
        questDao = database.questDao()
    )

    val timerState: StateFlow<TimerUiState> = FocusTimerEngine.timerState

    val userProfile: StateFlow<UserRpgProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedSessions: StateFlow<List<FocusSessionEntity>> = repository.completedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuests: StateFlow<List<ThesisMilestoneQuestEntity>> = repository.allQuests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDateStr: String
        get() = repository.getTodayDateString()

    val todaySessions: StateFlow<List<FocusSessionEntity>> = repository.allSessions
        .map { list ->
            val today = repository.getTodayDateString()
            list.filter { it.dateString == today }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMinutes: StateFlow<Int> = repository.completedSessions
        .map { list ->
            val today = repository.getTodayDateString()
            list.filter { it.dateString == today }.sumOf { it.durationMinutes }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyStats: StateFlow<List<DayFocusStat>> = repository.allSessions
        .combine(repository.userProfile) { sessions, profile ->
            val goal = profile?.dailyGoalMinutes ?: 120
            calculatePast7Days(sessions, goal)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
        }
    }

    private fun calculatePast7Days(sessions: List<FocusSessionEntity>, goalMinutes: Int): List<DayFocusStat> {
        val list = mutableListOf<DayFocusStat>()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dateStr = sdfDate.format(dateCal.time)
            val dayLabel = sdfDay.format(dateCal.time)
            val minutes = sessions
                .filter { it.dateString == dateStr && it.isCompleted }
                .sumOf { it.durationMinutes }

            list.add(
                DayFocusStat(
                    dateString = dateStr,
                    dayLabel = dayLabel,
                    totalMinutes = minutes,
                    goalMinutes = goalMinutes,
                    isGoalAchieved = minutes >= goalMinutes
                )
            )
        }
        return list
    }

    fun selectDuration(minutes: Int) {
        FocusTimerEngine.selectDuration(minutes)
    }

    fun selectTaskTag(tag: ThesisTaskTag) {
        FocusTimerEngine.selectTaskTag(tag)
    }

    fun selectTreeSpecies(species: TreeSpecies) {
        FocusTimerEngine.selectTreeSpecies(species)
    }

    fun updateSessionNotes(notes: String) {
        FocusTimerEngine.updateSessionNotes(notes)
    }

    fun startTimer() {
        FocusTimerEngine.startSession(getApplication())
    }

    fun pauseTimer() {
        FocusTimerEngine.pauseSession(getApplication())
    }

    fun resumeTimer() {
        FocusTimerEngine.resumeSession(getApplication())
    }

    fun syncWithRealtime() {
        FocusTimerEngine.syncWithRealtime(getApplication())
    }

    fun completeTimer() {
        FocusTimerEngine.completeSession(getApplication())
    }

    fun stopTimer() {
        val state = FocusTimerEngine.timerState.value
        if (state.isOvertime) {
            FocusTimerEngine.completeSession(getApplication())
        } else {
            FocusTimerEngine.promptAbandon()
        }
    }

    fun promptAbandon() {
        FocusTimerEngine.promptAbandon()
    }

    fun dismissAbandonDialog() {
        FocusTimerEngine.dismissAbandonDialog()
    }

    fun confirmAbandon() {
        FocusTimerEngine.confirmAbandon(getApplication())
    }

    fun resetWitheredState() {
        FocusTimerEngine.resetWitheredState()
    }

    fun dismissCelebration() {
        FocusTimerEngine.dismissCelebration()
    }

    fun claimQuest(questId: String) {
        viewModelScope.launch {
            repository.claimQuest(questId)
        }
    }

    fun updateDailyGoal(newMinutes: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(newMinutes)
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }
}
