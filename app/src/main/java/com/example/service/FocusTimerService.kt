package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var serviceJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentTaskTag: String = "Chapter Writing"
    private var currentTreeSpecies: String = "Seedling of Clarity"
    private var targetDurationSeconds: Int = 15 * 60
    private var accumulatedElapsedMillis: Long = 0L
    private var segmentStartRealtime: Long = 0L
    private var isPausedState: Boolean = false
    private var hasTriggeredMilestoneNotification: Boolean = false

    companion object {
        const val CHANNEL_ID = "thesis_focus_channel"
        const val COMPLETION_CHANNEL_ID = "thesis_completion_channel"
        const val NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.example.action.START_FOCUS"
        const val ACTION_PAUSE = "com.example.action.PAUSE_FOCUS"
        const val ACTION_RESUME = "com.example.action.RESUME_FOCUS"
        const val ACTION_STOP = "com.example.action.STOP_FOCUS"
        const val ACTION_NOTIFY_COMPLETION = "com.example.action.NOTIFY_COMPLETION"
        const val ACTION_NOTIFY_MILESTONE = "com.example.action.NOTIFY_MILESTONE"

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_TARGET_DURATION = "extra_target_duration"
        const val EXTRA_ELAPSED_SECONDS = "extra_elapsed_seconds"
        const val EXTRA_TASK_TAG = "extra_task_tag"
        const val EXTRA_TREE_SPECIES = "extra_tree_species"
        const val EXTRA_COMPLETED_DURATION = "extra_completed_duration"
        const val EXTRA_BONUS_MINUTES = "extra_bonus_minutes"

        fun startService(context: Context, durationSeconds: Int, taskTag: String, treeSpecies: String) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_TASK_TAG, taskTag)
                putExtra(EXTRA_TREE_SPECIES, treeSpecies)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseService(context: Context, elapsedSeconds: Int, targetDurationSeconds: Int) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
                putExtra(EXTRA_TARGET_DURATION, targetDurationSeconds)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun resumeService(
            context: Context,
            elapsedSeconds: Int,
            targetDurationSeconds: Int,
            taskTag: String,
            treeSpecies: String
        ) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_ELAPSED_SECONDS, elapsedSeconds)
                putExtra(EXTRA_TARGET_DURATION, targetDurationSeconds)
                putExtra(EXTRA_TASK_TAG, taskTag)
                putExtra(EXTRA_TREE_SPECIES, treeSpecies)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun notifySprintMilestone(context: Context, treeSpecies: String) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_NOTIFY_MILESTONE
                putExtra(EXTRA_TREE_SPECIES, treeSpecies)
            }
            context.startService(intent)
        }

        fun notifyCompletion(context: Context, treeSpecies: String, durationMinutes: Int, bonusMinutes: Int = 0) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_NOTIFY_COMPLETION
                putExtra(EXTRA_TREE_SPECIES, treeSpecies)
                putExtra(EXTRA_COMPLETED_DURATION, durationMinutes)
                putExtra(EXTRA_BONUS_MINUTES, bonusMinutes)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                targetDurationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, 15 * 60)
                currentTaskTag = intent.getStringExtra(EXTRA_TASK_TAG) ?: "Chapter Writing"
                currentTreeSpecies = intent.getStringExtra(EXTRA_TREE_SPECIES) ?: "Seedling of Clarity"
                accumulatedElapsedMillis = 0L
                segmentStartRealtime = SystemClock.elapsedRealtime()
                isPausedState = false
                hasTriggeredMilestoneNotification = false

                acquireWakeLock(12 * 3600_000L) // Support long study sessions
                startForeground(NOTIFICATION_ID, buildRunningNotification(targetDurationSeconds))
                startBackgroundLoop()
            }
            ACTION_PAUSE -> {
                serviceJob?.cancel()
                isPausedState = true
                val elapsedSec = intent.getIntExtra(EXTRA_ELAPSED_SECONDS, (accumulatedElapsedMillis / 1000L).toInt())
                val targetSec = intent.getIntExtra(EXTRA_TARGET_DURATION, targetDurationSeconds)
                targetDurationSeconds = targetSec
                accumulatedElapsedMillis = elapsedSec * 1000L
                segmentStartRealtime = 0L

                updateNotification(buildPausedNotification(elapsedSec, targetSec))
            }
            ACTION_RESUME -> {
                val elapsedSec = intent.getIntExtra(EXTRA_ELAPSED_SECONDS, (accumulatedElapsedMillis / 1000L).toInt())
                val targetSec = intent.getIntExtra(EXTRA_TARGET_DURATION, targetDurationSeconds)
                targetDurationSeconds = targetSec
                accumulatedElapsedMillis = elapsedSec * 1000L
                segmentStartRealtime = SystemClock.elapsedRealtime()
                isPausedState = false

                currentTaskTag = intent.getStringExtra(EXTRA_TASK_TAG) ?: currentTaskTag
                currentTreeSpecies = intent.getStringExtra(EXTRA_TREE_SPECIES) ?: currentTreeSpecies

                acquireWakeLock(12 * 3600_000L)
                updateNotificationBasedOnElapsed(elapsedSec)
                startBackgroundLoop()
            }
            ACTION_STOP -> {
                shutdownService()
            }
            ACTION_NOTIFY_MILESTONE -> {
                val species = intent.getStringExtra(EXTRA_TREE_SPECIES) ?: currentTreeSpecies
                showMilestoneAchievedNotification(species)
            }
            ACTION_NOTIFY_COMPLETION -> {
                val species = intent.getStringExtra(EXTRA_TREE_SPECIES) ?: currentTreeSpecies
                val minutes = intent.getIntExtra(EXTRA_COMPLETED_DURATION, targetDurationSeconds / 60)
                val bonusMins = intent.getIntExtra(EXTRA_BONUS_MINUTES, 0)
                showCompletionNotification(species, minutes, bonusMins)
                shutdownService()
            }
        }
        return START_NOT_STICKY
    }

    private fun getCurrentElapsedSeconds(): Int {
        val totalMs = if (!isPausedState && segmentStartRealtime > 0L) {
            accumulatedElapsedMillis + (SystemClock.elapsedRealtime() - segmentStartRealtime)
        } else {
            accumulatedElapsedMillis
        }
        return (totalMs / 1000L).toInt().coerceAtLeast(0)
    }

    private fun startBackgroundLoop() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                if (isPausedState) continue

                val elapsedSec = getCurrentElapsedSeconds()
                val isOvertime = elapsedSec >= targetDurationSeconds

                if (isOvertime && !hasTriggeredMilestoneNotification) {
                    hasTriggeredMilestoneNotification = true
                    showMilestoneAchievedNotification(currentTreeSpecies)
                }

                // Periodically update notification every 5 seconds or upon crossing overtime
                if (elapsedSec % 5 == 0 || elapsedSec == targetDurationSeconds) {
                    updateNotificationBasedOnElapsed(elapsedSec)
                }
            }
        }
    }

    private fun updateNotificationBasedOnElapsed(elapsedSec: Int) {
        if (elapsedSec >= targetDurationSeconds) {
            val overtimeSec = elapsedSec - targetDurationSeconds
            updateNotification(buildOvertimeNotification(elapsedSec, overtimeSec))
        } else {
            val remainingSec = targetDurationSeconds - elapsedSec
            updateNotification(buildRunningNotification(remainingSec))
        }
    }

    private fun acquireWakeLock(timeoutMs: Long = 3600_000L) {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ThesisGrove:FocusTimerWakeLock")?.apply {
                setReferenceCounted(false)
            }
        }
        try {
            wakeLock?.acquire(timeoutMs)
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun shutdownService() {
        serviceJob?.cancel()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildRunningNotification(remainingSeconds: Int): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_forest_timer)
            .setContentTitle("🌱 Growing $currentTreeSpecies")
            .setContentText("$currentTaskTag • $timeString remaining")
            .setSubText("Sleep Mode Active")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + (remainingSeconds * 1000L))
            .build()
    }

    private fun buildOvertimeNotification(elapsedSeconds: Int, overtimeSeconds: Int): Notification {
        val totalM = elapsedSeconds / 60
        val totalS = elapsedSeconds % 60
        val otM = overtimeSeconds / 60
        val otS = overtimeSeconds % 60

        val overtimeStr = if (otM >= 60) {
            val otH = otM / 60
            val remM = otM % 60
            String.format(Locale.getDefault(), "+%02d:%02d:%02d", otH, remM, otS)
        } else {
            String.format(Locale.getDefault(), "+%02d:%02d", otM, otS)
        }

        val totalStr = if (totalM >= 60) {
            val totalH = totalM / 60
            val remM = totalM % 60
            String.format(Locale.getDefault(), "%02d:%02d:%02d", totalH, remM, totalS)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", totalM, totalS)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_forest_timer)
            .setContentTitle("🌟 Bonus Focus: $currentTreeSpecies")
            .setContentText("$currentTaskTag • $overtimeStr bonus (Total: $totalStr)")
            .setSubText("Bonus Sprint Active")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun buildPausedNotification(elapsedSeconds: Int, targetDurationSeconds: Int): Notification {
        val isOvertime = elapsedSeconds >= targetDurationSeconds
        val contentText = if (isOvertime) {
            val otSec = elapsedSeconds - targetDurationSeconds
            val otM = otSec / 60
            val otS = otSec % 60
            "$currentTreeSpecies • +%02d:%02d bonus overtime paused".format(otM, otS)
        } else {
            val remSec = targetDurationSeconds - elapsedSeconds
            val remM = remSec / 60
            val remS = remSec % 60
            "$currentTreeSpecies • %02d:%02d remaining (Paused)".format(remM, remS)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_forest_timer)
            .setContentTitle("⏸️ Focus Paused")
            .setContentText(contentText)
            .setSubText("Paused")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun showMilestoneAchievedNotification(species: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_forest_timer)
            .setContentTitle("🎉 Sprint Goal Achieved!")
            .setContentText("Your $species is flourishing! Overtime bonus is now active as long as you keep focusing.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(species: String, durationMinutes: Int, bonusMinutes: Int = 0) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val desc = if (bonusMinutes > 0) {
            "Your $species is fully cultivated ($durationMinutes mins total, including +$bonusMinutes mins bonus)! Tap to view your grove."
        } else {
            "Your $species is fully cultivated ($durationMinutes mins). Tap to view your grove!"
        }

        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_forest_timer)
            .setContentTitle("🌟 Thesis Tree Cultivated!")
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun updateNotification(notification: Notification) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusChannel = NotificationChannel(
                CHANNEL_ID,
                "Thesis Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time countdown on lockscreen and across mobile sleep mode"
                setShowBadge(false)
            }

            val completionChannel = NotificationChannel(
                COMPLETION_CHANNEL_ID,
                "Thesis Quest Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a thesis tree cultivation session completes"
                enableVibration(true)
                setShowBadge(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(focusChannel)
            manager?.createNotificationChannel(completionChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceJob?.cancel()
    }
}
