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
    private var targetEndRealtime: Long = 0L

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

        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        const val EXTRA_TASK_TAG = "extra_task_tag"
        const val EXTRA_TREE_SPECIES = "extra_tree_species"
        const val EXTRA_COMPLETED_DURATION = "extra_completed_duration"

        fun startService(context: Context, durationSeconds: Int, taskTag: String, treeSpecies: String) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
                putExtra(EXTRA_TASK_TAG, taskTag)
                putExtra(EXTRA_TREE_SPECIES, treeSpecies)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_PAUSE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun resumeService(context: Context, remainingSeconds: Int, taskTag: String, treeSpecies: String) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_DURATION_SECONDS, remainingSeconds)
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

        fun notifyCompletion(context: Context, treeSpecies: String, durationMinutes: Int) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_NOTIFY_COMPLETION
                putExtra(EXTRA_TREE_SPECIES, treeSpecies)
                putExtra(EXTRA_COMPLETED_DURATION, durationMinutes)
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
                val durationSec = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
                currentTaskTag = intent.getStringExtra(EXTRA_TASK_TAG) ?: "Chapter Writing"
                currentTreeSpecies = intent.getStringExtra(EXTRA_TREE_SPECIES) ?: "Seedling of Clarity"
                targetEndRealtime = SystemClock.elapsedRealtime() + (durationSec * 1000L)

                acquireWakeLock((durationSec + 120) * 1000L)
                startForeground(NOTIFICATION_ID, buildRunningNotification(durationSec))
                startBackgroundLoop()
            }
            ACTION_PAUSE -> {
                serviceJob?.cancel()
                val remaining = ((targetEndRealtime - SystemClock.elapsedRealtime() + 999L) / 1000L).coerceAtLeast(0).toInt()
                updateNotification(buildPausedNotification(remaining))
            }
            ACTION_RESUME -> {
                val remainingSec = intent.getIntExtra(EXTRA_DURATION_SECONDS, 25 * 60)
                currentTaskTag = intent.getStringExtra(EXTRA_TASK_TAG) ?: currentTaskTag
                currentTreeSpecies = intent.getStringExtra(EXTRA_TREE_SPECIES) ?: currentTreeSpecies
                targetEndRealtime = SystemClock.elapsedRealtime() + (remainingSec * 1000L)

                acquireWakeLock((remainingSec + 120) * 1000L)
                updateNotification(buildRunningNotification(remainingSec))
                startBackgroundLoop()
            }
            ACTION_STOP -> {
                shutdownService()
            }
            ACTION_NOTIFY_COMPLETION -> {
                val species = intent.getStringExtra(EXTRA_TREE_SPECIES) ?: currentTreeSpecies
                val minutes = intent.getIntExtra(EXTRA_COMPLETED_DURATION, 25)
                showCompletionNotification(species, minutes)
                shutdownService()
            }
        }
        return START_NOT_STICKY
    }

    private fun startBackgroundLoop() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val remainingSec = ((targetEndRealtime - SystemClock.elapsedRealtime() + 999L) / 1000L).toInt()

                if (remainingSec <= 0) {
                    // Time is up while in sleep mode or background!
                    FocusTimerEngine.syncWithRealtime(this@FocusTimerService)
                    break
                } else {
                    // Periodically update notification every 5 seconds or when needed
                    if (remainingSec % 5 == 0) {
                        updateNotification(buildRunningNotification(remainingSec))
                    }
                }
            }
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

    private fun buildPausedNotification(remainingSeconds: Int): Notification {
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
            .setContentTitle("⏸️ Focus Paused")
            .setContentText("$currentTreeSpecies • $timeString remaining")
            .setSubText("Paused")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun showCompletionNotification(species: String, durationMinutes: Int) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_forest_timer)
            .setContentTitle("🌟 Thesis Tree Cultivated!")
            .setContentText("Your $species is fully grown ($durationMinutes mins). Tap to view your grove!")
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
