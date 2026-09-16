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
    val totalDurationSeconds: Int = 15 * 60, // Sprint target duration (e.g. 15m)
    val remainingSeconds: Int = 15 * 60,
    val elapsedSeconds: Int = 0,
    val isOvertime: Boolean = false,
    val overtimeSeconds: Int = 0,
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
            (elapsedSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

object FocusTimerEngine {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _timerState = MutableStateFlow(TimerUiState())
    val timerState: StateFlow<TimerUiState> = _timerState.asStateFlow()

    private var sessionStartTimeWall: Long = 0L
    private var accumulatedElapsedMillis: Long = 0L
    private var segmentStartRealtime: Long = 0L
    private var hasTriggeredMilestone: Boolean = false

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

    fun getElapsedMillis(): Long {
        val state = _timerState.value
        return if (state.isRunning && !state.isPaused && segmentStartRealtime > 0L) {
            accumulatedElapsedMillis + (SystemClock.elapsedRealtime() - segmentStartRealtime)
        } else {
            accumulatedElapsedMillis
        }
    }

    fun getElapsedSeconds(): Int {
        return (getElapsedMillis() / 1000L).toInt().coerceAtLeast(0)
    }

    fun selectDuration(minutes: Int) {
        if (!_timerState.value.isRunning) {
            val seconds = minutes * 60
            _timerState.update {
                it.copy(
                    totalDurationSeconds = seconds,
                    remainingSeconds = seconds,
                    elapsedSeconds = 0,
                    isOvertime = false,
                    overtimeSeconds = 0,
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
        accumulatedElapsedMillis = 0L
        segmentStartRealtime = SystemClock.elapsedRealtime()
        hasTriggeredMilestone = false

        val targetSec = _timerState.value.totalDurationSeconds

        _timerState.update {
            it.copy(
                isRunning = true,
                isPaused = false,
                isWithered = false,
                showAbandonDialog = false,
                elapsedSeconds = 0,
                remainingSeconds = targetSec,
                isOvertime = false,
                overtimeSeconds = 0
            )
        }

        // Launch the foreground service to run even when mobile sleeps
        FocusTimerService.startService(
            context = context,
            durationSeconds = targetSec,
            taskTag = _timerState.value.selectedTaskTag.label,
            treeSpecies = _timerState.value.selectedTreeSpecies.displayName
        )

        startTicker(context)
    }

    fun pauseSession(context: Context) {
        val currentState = _timerState.value
        if (!currentState.isRunning || currentState.isPaused) return

        if (segmentStartRealtime > 0L) {
            accumulatedElapsedMillis += (SystemClock.elapsedRealtime() - segmentStartRealtime)
            segmentStartRealtime = 0L
        }

        tickerJob?.cancel()

        val elapsedSec = (accumulatedElapsedMillis / 1000L).toInt()
        val targetSec = currentState.totalDurationSeconds
        val isOvertime = elapsedSec >= targetSec
        val remainingSec = if (isOvertime) 0 else (targetSec - elapsedSec)
        val overtimeSec = if (isOvertime) (elapsedSec - targetSec) else 0

        _timerState.update {
            it.copy(
                isPaused = true,
                elapsedSeconds = elapsedSec,
                remainingSeconds = remainingSec,
                isOvertime = isOvertime,
                overtimeSeconds = overtimeSec
            )
        }

        FocusTimerService.pauseService(context, elapsedSec, targetSec)
    }

    fun resumeSession(context: Context) {
        val currentState = _timerState.value
        if (!currentState.isRunning || !currentState.isPaused) return

        segmentStartRealtime = SystemClock.elapsedRealtime()

        val elapsedSec = (accumulatedElapsedMillis / 1000L).toInt()
        val targetSec = currentState.totalDurationSeconds
        val isOvertime = elapsedSec >= targetSec
        val remainingSec = if (isOvertime) 0 else (targetSec - elapsedSec)
        val overtimeSec = if (isOvertime) (elapsedSec - targetSec) else 0

        _timerState.update {
            it.copy(
                isPaused = false,
                elapsedSeconds = elapsedSec,
                remainingSeconds = remainingSec,
                isOvertime = isOvertime,
                overtimeSeconds = overtimeSec
            )
        }

        FocusTimerService.resumeService(
            context = context,
            elapsedSeconds = elapsedSec,
            targetDurationSeconds = targetSec,
            taskTag = currentState.selectedTaskTag.label,
            treeSpecies = currentState.selectedTreeSpecies.displayName
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
        val currentState = _timerState.value
        if (!currentState.isRunning) return

        tickerJob?.cancel()
        FocusTimerService.stopService(context)

        val elapsedSec = getElapsedSeconds()
        val targetSec = currentState.totalDurationSeconds

        if (elapsedSec >= targetSec) {
            // Already beyond sprint: user gets the bonus harvest!
            completeSession(context)
            return
        }

        // Below focus sprint: withering as requested
        val elapsedMinutes = (elapsedSec / 60).coerceAtLeast(1)

        scope.launch {
            val session = getRepository(context).recordSession(
                startTime = sessionStartTimeWall,
                endTime = System.currentTimeMillis(),
                durationMinutes = elapsedMinutes,
                taskTag = currentState.selectedTaskTag.id,
                treeSpeciesId = currentState.selectedTreeSpecies.id,
                isCompleted = false,
                notes = if (currentState.sessionNotes.isNotBlank()) {
                    "${currentState.sessionNotes} (Withered at ${elapsedMinutes}m of ${targetSec / 60}m goal)"
                } else {
                    "Withered at ${elapsedMinutes}m of ${targetSec / 60}m goal"
                }
            )

            _timerState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    elapsedSeconds = 0,
                    remainingSeconds = it.totalDurationSeconds,
                    isOvertime = false,
                    overtimeSeconds = 0,
                    isWithered = true,
                    showAbandonDialog = false,
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
     * Ticks the countdown/countup. Uses [SystemClock.elapsedRealtime] which monotonically counts
     * across mobile sleep and doze states, guaranteeing 100% time accuracy without capping.
     */
    fun tick(context: Context) {
        syncWithRealtime(context)
    }

    fun syncWithRealtime(context: Context) {
        val currentState = _timerState.value
        if (!currentState.isRunning || currentState.isPaused) return

        val elapsedSec = getElapsedSeconds()
        val targetSec = currentState.totalDurationSeconds
        val isOvertime = elapsedSec >= targetSec
        val remainingSec = if (isOvertime) 0 else (targetSec - elapsedSec)
        val overtimeSec = if (isOvertime) (elapsedSec - targetSec) else 0

        if (isOvertime && !hasTriggeredMilestone) {
            hasTriggeredMilestone = true
            triggerMilestoneHaptic(context)
            FocusTimerService.notifySprintMilestone(context, currentState.selectedTreeSpecies.displayName)
        }

        _timerState.update {
            it.copy(
                elapsedSeconds = elapsedSec,
                remainingSeconds = remainingSec,
                isOvertime = isOvertime,
                overtimeSeconds = overtimeSec
            )
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
        val currentState = _timerState.value
        if (!currentState.isRunning) return

        val elapsedSec = getElapsedSeconds()
        val targetSec = currentState.totalDurationSeconds
        val exactMinutes = (elapsedSec / 60).coerceAtLeast(targetSec / 60)
        val bonusMinutes = (exactMinutes - (targetSec / 60)).coerceAtLeast(0)
        val endTime = System.currentTimeMillis()

        tickerJob?.cancel()
        FocusTimerService.stopService(context)

        FocusTimerService.notifyCompletion(
            context = context,
            treeSpecies = currentState.selectedTreeSpecies.displayName,
            durationMinutes = exactMinutes,
            bonusMinutes = bonusMinutes
        )

        triggerMilestoneHaptic(context)

        scope.launch {
            val notesWithBonus = when {
                bonusMinutes > 0 && currentState.sessionNotes.isNotBlank() ->
                    "${currentState.sessionNotes} (Bonus Overtime: +${bonusMinutes}m)"
                bonusMinutes > 0 ->
                    "Bonus Overtime: +${bonusMinutes}m"
                else -> currentState.sessionNotes
            }

            val session = getRepository(context).recordSession(
                startTime = sessionStartTimeWall,
                endTime = endTime,
                durationMinutes = exactMinutes,
                taskTag = currentState.selectedTaskTag.id,
                treeSpeciesId = currentState.selectedTreeSpecies.id,
                isCompleted = true,
                notes = notesWithBonus
            )

            _timerState.update {
                it.copy(
                    isRunning = false,
                    isPaused = false,
                    elapsedSeconds = 0,
                    remainingSeconds = it.totalDurationSeconds,
                    isOvertime = false,
                    overtimeSeconds = 0,
                    showCelebrationDialog = true,
                    showAbandonDialog = false,
                    lastCompletedSession = session,
                    isWithered = false
                )
            }
        }
    }

    private fun triggerMilestoneHaptic(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 250), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 150, 100, 250), -1)
            }
        } catch (_: Exception) {
        }
    }
}
