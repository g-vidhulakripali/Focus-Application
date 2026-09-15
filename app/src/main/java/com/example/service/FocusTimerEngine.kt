package com.example.service

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.database.AppDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.ThesisTaskTag
import com.example.data.model.TreeSpecies
import com.example.data.repository.ThesisFocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

object FocusTimerEngine {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private var targetEndRealtime: Long = 0L
    private var sessionStartTimeWall: Long = 0L
    private var tickerJob: Job? = null
    private var repository: ThesisFocusRepository? = null

    fun getRepository(context: Context): ThesisFocusRepository {
        if (repository == null) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            repository = ThesisFocusRepository(
                sessionDao = db.focusSessionDao(),
                profileDao = db.userRpgProfileDao(),
                questDao = db.questDao()
            )
        }
        return repository!!
    }

    fun selectDuration(minutes: Int) {
        if (!_timerState.value.isRunning) {
            val seconds = minutes * 60
            _timerState.update {
                it.copy(
                    totalDurationSeconds = seconds,
                    remainingSeconds = seconds,
                    isWithered = false
                )
            }
        }
    }

    fun selectTaskTag(tag: ThesisTaskTag) {
        _timerState.update { it.copy(selectedTaskTag = tag) }
    }

    fun selectTreeSpecies(species: TreeSpecies) {
        _timerState.update { it.copy(selectedTreeSpecies = species) }
    }

    fun updateSessionNotes(notes: String) {
        _timerState.update { it.copy(sessionNotes = notes) }
    }

    fun startSession(context: Context) {
        if (_timerState.value.isRunning) return

        sessionStartTimeWall = System.currentTimeMillis()
        val durationSec = _timerState.value.totalDurationSeconds
        targetEndRealtime = SystemClock.elapsedRealtime() + (durationSec * 1000L)

        _timerState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isWithered = false,
                showAbandonDialog = false,
                remainingSeconds = durationSec
            )
        }

        // Launch the foreground service to keep CPU awake during mobile sleep mode
        FocusTimerService.startService(
            context = context,
            durationSeconds = durationSec,
            taskTag = _timerState.value.selectedTaskTag.label,
            treeSpecies = _timerState.value.selectedTreeSpecies.displayName
        )

        startTicker(context)
    }

    fun pauseSession(context: Context) {
        if (!_timerState.value.isRunning || _timerState.value.isPaused) return
        syncWithRealtime(context)

        tickerJob?.cancel()
        _timerState.update { it.copy(isPaused = true) }

        FocusTimerService.pauseService(context)
    }

    fun resumeSession(context: Context) {
        if (!_timerState.value.isRunning || !_timerState.value.isPaused) return

        val remainingSec = _timerState.value.remainingSeconds
        targetEndRealtime = SystemClock.elapsedRealtime() + (remainingSec * 1000L)

        _timerState.update { it.copy(isPaused = false) }

        FocusTimerService.resumeService(
            context = context,
            remainingSeconds = remainingSec,
            taskTag = _timerState.value.selectedTaskTag.label,
            treeSpecies = _timerState.value.selectedTreeSpecies.displayName
        )

        startTicker(context)
    }

    fun promptAbandon() {
        _timerState.update { it.copy(showAbandonDialog = true) }
    }

    fun dismissAbandonDialog() {
        _timerState.update { it.copy(showAbandonDialog = false) }
    }

    fun confirmAbandon(context: Context) {
        tickerJob?.cancel()
        FocusTimerService.stopService(context)

        val currentState = _timerState.value
        val elapsedSeconds = (currentState.totalDurationSeconds - currentState.remainingSeconds).coerceAtLeast(0)
        val elapsedMinutes = (elapsedSeconds / 60).coerceAtLeast(1)

        scope.launch {
            val session = getRepository(context).recordSession(
                startTime = sessionStartTimeWall,
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

    /**
     * Ticks the countdown. Uses [SystemClock.elapsedRealtime] which monotonically counts
     * across mobile sleep and doze states, guaranteeing 100% time accuracy.
     */
    fun tick(context: Context) {
        if (!_timerState.value.isRunning || _timerState.value.isPaused) return

        val nowRealtime = SystemClock.elapsedRealtime()
        val remainingMillis = targetEndRealtime - nowRealtime
        val remainingSec = ((remainingMillis + 999L) / 1000L).toInt()

        if (remainingSec <= 0) {
            _timerState.update { it.copy(remainingSeconds = 0) }
            tickerJob?.cancel()
            completeSession(context)
        } else {
            _timerState.update { it.copy(remainingSeconds = remainingSec) }
        }
    }

    fun syncWithRealtime(context: Context) {
        if (_timerState.value.isRunning && !_timerState.value.isPaused) {
            val nowRealtime = SystemClock.elapsedRealtime()
            val remainingMillis = targetEndRealtime - nowRealtime
            val remainingSec = ((remainingMillis + 999L) / 1000L).toInt()
            if (remainingSec <= 0) {
                tickerJob?.cancel()
                completeSession(context)
            } else {
                _timerState.update { it.copy(remainingSeconds = remainingSec) }
            }
        }
    }

    private fun startTicker(context: Context) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (_timerState.value.isRunning && !_timerState.value.isPaused) {
                delay(1000L)
                tick(context)
            }
        }
    }

    fun completeSession(context: Context) {
        if (!_timerState.value.isRunning) return

        val currentState = _timerState.value
        val durationMinutes = currentState.totalDurationSeconds / 60
        val endTime = System.currentTimeMillis()

        triggerHaptic(context)

        // Inform the foreground service that session completed so it can post the celebration notification
        FocusTimerService.notifyCompletion(
            context = context,
            treeSpecies = currentState.selectedTreeSpecies.displayName,
            durationMinutes = durationMinutes
        )

        scope.launch {
            val session = getRepository(context).recordSession(
                startTime = sessionStartTimeWall,
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

    private fun triggerHaptic(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (_: Exception) {
        }
    }
}
