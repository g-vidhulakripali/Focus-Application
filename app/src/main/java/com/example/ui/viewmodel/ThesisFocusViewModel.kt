package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.ThesisMilestoneQuestEntity
import com.example.data.model.ThesisTaskTag
import com.example.data.model.TreeSpecies
import com.example.data.model.UserRpgProfileEntity
import com.example.data.repository.ThesisFocusRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayFocusStat(
    val dateString: String,
    val dayLabel: String, // e.g. "Mon", "Tue"
    val totalMinutes: Int,
    val goalMinutes: Int,
    val isGoalAchieved: Boolean
)

data class TimerUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalDurationSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val selectedTaskTag: ThesisTaskTag = ThesisTaskTag.CHAPTER_WRITING,
    val selectedTreeSpecies: TreeSpecies = TreeSpecies.SEEDLING_OF_CLARITY,
    val sessionNotes: String = "",
    val showAbandonDialog: Boolean = false,
    val showCelebrationDialog: Boolean = false,
    val lastCompletedSession: FocusSessionEntity? = null,
    val isWithered: Boolean = false
) {
    val progress: Float
        get() = if (totalDurationSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

class ThesisFocusViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ThesisFocusRepository(
        sessionDao = database.focusSessionDao(),
        profileDao = database.userRpgProfileDao(),
        questDao = database.questDao()
    )

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0

    val userProfile: StateFlow<UserRpgProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedSessions: StateFlow<List<FocusSessionEntity>> = repository.completedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<FocusSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuests: StateFlow<List<ThesisMilestoneQuestEntity>> = repository.allQuests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDateStr = repository.getTodayDateString()

    val todaySessions: StateFlow<List<FocusSessionEntity>> = repository.getSessionsForDate(todayDateStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMinutes: StateFlow<Int> = repository.getMinutesForDate(todayDateStr)
        .combine(MutableStateFlow(0)) { minutes, _ -> minutes ?: 0 }
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
        val cal = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())

        // Past 7 days including today
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
        if (!_timerState.value.isRunning) {
            _timerState.update {
                it.copy(
                    totalDurationSeconds = minutes * 60,
                    remainingSeconds = minutes * 60,
                    isWithered = false
                )
            }
        }
    }

    fun selectTaskTag(tag: ThesisTaskTag) {
        _timerState.update { it.copy(selectedTaskTag = tag) }
    }

    fun selectTreeSpecies(species: TreeSpecies) {
        val currentLevel = userProfile.value?.scholarLevel ?: 1
        if (currentLevel >= species.requiredLevel) {
            _timerState.update { it.copy(selectedTreeSpecies = species) }
        }
    }

    fun updateSessionNotes(notes: String) {
        _timerState.update { it.copy(sessionNotes = notes) }
    }

    fun startTimer() {
        if (_timerState.value.isRunning) return

        sessionStartTime = System.currentTimeMillis()
        _timerState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isWithered = false,
                showAbandonDialog = false
            )
        }

        startCountdown()
    }

    fun pauseTimer() {
        if (!_timerState.value.isRunning || _timerState.value.isPaused) return
        timerJob?.cancel()
        _timerState.update { it.copy(isPaused = true) }
    }

    fun resumeTimer() {
        if (!_timerState.value.isRunning || !_timerState.value.isPaused) return
        _timerState.update { it.copy(isPaused = false) }
        startCountdown()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning && !_timerState.value.isPaused) {
                delay(1000L)
                _timerState.update {
                    val newRemaining = it.remainingSeconds - 1
                    it.copy(remainingSeconds = newRemaining)
                }
            }

            if (_timerState.value.remainingSeconds <= 0 && _timerState.value.isRunning) {
                onSessionCompleted()
            }
        }
    }

    private fun onSessionCompleted() {
        val currentState = _timerState.value
        val durationMinutes = currentState.totalDurationSeconds / 60
        val endTime = System.currentTimeMillis()

        triggerHapticFeedback()

        viewModelScope.launch {
            val session = repository.recordSession(
                startTime = sessionStartTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                taskTag = currentState.selectedTaskTag.id,
                treeSpeciesId = currentState.selectedTreeSpecies.id,
                isCompleted = true,
                notes = currentState.sessionNotes
            )

            _timerState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    remainingSeconds = it.totalDurationSeconds,
                    showCelebrationDialog = true,
                    lastCompletedSession = session,
                    isWithered = false
                )
            }
        }
    }

    fun promptAbandon() {
        _timerState.update { it.copy(showAbandonDialog = true) }
    }

    fun dismissAbandonDialog() {
        _timerState.update { it.copy(showAbandonDialog = false) }
    }

    fun confirmAbandon() {
        timerJob?.cancel()
        val currentState = _timerState.value
        val elapsedSeconds = currentState.totalDurationSeconds - currentState.remainingSeconds
        val elapsedMinutes = (elapsedSeconds / 60).coerceAtLeast(1)

        viewModelScope.launch {
            val session = repository.recordSession(
                startTime = sessionStartTime,
                endTime = System.currentTimeMillis(),
                durationMinutes = elapsedMinutes,
                taskTag = currentState.selectedTaskTag.id,
                treeSpeciesId = currentState.selectedTreeSpecies.id,
                isCompleted = false,
                notes = "Session abandoned early"
            )

            _timerState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    isWithered = true,
                    showAbandonDialog = false,
                    remainingSeconds = it.totalDurationSeconds,
                    lastCompletedSession = session
                )
            }
        }
    }

    fun resetWitheredState() {
        _timerState.update { it.copy(isWithered = false) }
    }

    fun dismissCelebration() {
        _timerState.update { it.copy(showCelebrationDialog = false) }
    }

    fun claimQuest(questId: String) {
        viewModelScope.launch {
            repository.claimQuest(questId)
            triggerHapticFeedback()
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

    private fun triggerHapticFeedback() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        } catch (_: Exception) {
            // Non-critical vibration fallback
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
