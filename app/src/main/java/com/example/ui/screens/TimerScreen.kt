package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThesisTaskTag
import com.example.data.model.TreeSpecies
import com.example.ui.components.ForestTreeGraphic
import com.example.ui.components.StreakFlameBadge
import com.example.ui.viewmodel.ThesisFocusViewModel
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
    viewModel: ThesisFocusViewModel,
    onNavigateToAnalytics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timerState by viewModel.timerState.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()

    val goalMinutes = profile?.dailyGoalMinutes ?: 120
    val streakDays = profile?.currentStreak ?: 0
    val userLevel = profile?.scholarLevel ?: 1

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Streak & Daily Goal info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scholar Level Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lv. $userLevel Scholar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6F5F0)
                        )
                    }
                }

                // Interactive Streak Flame Badge
                StreakFlameBadge(
                    streakDays = streakDays,
                    todayMinutes = todayMinutes,
                    goalMinutes = goalMinutes,
                    onClick = onNavigateToAnalytics
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Visual: Growing Forest Tree Graphic
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                ForestTreeGraphic(
                    progress = timerState.progress,
                    species = timerState.selectedTreeSpecies,
                    isWithered = timerState.isWithered,
                    sizeDp = 250.dp
                )
            }

            // Tree species label & task tag pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(timerState.selectedTaskTag.colorLong).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(timerState.selectedTaskTag.colorLong).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = timerState.selectedTaskTag.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(timerState.selectedTaskTag.colorLong)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = timerState.selectedTreeSpecies.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large Digital Countdown Timer
            val minutes = timerState.remainingSeconds / 60
            val seconds = timerState.remainingSeconds % 60
            val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

            Text(
                text = timeString,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = if (timerState.isWithered) Color(0xFF9E9E9E) else Color(0xFFF1F5F9),
                modifier = Modifier.testTag("timer_display_text")
            )

            // Dynamic Encouragement Quote / Status
            Text(
                text = when {
                    timerState.isWithered -> "Your sapling withered. Rekindle your focus to plant anew!"
                    timerState.isRunning && timerState.isPaused -> "Quest paused. Resume to keep growing your thesis tree."
                    timerState.isRunning -> "Deep in thesis focus... do not wander away!"
                    else -> "Plant a tree to cultivate your thesis chapter."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (timerState.isWithered) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // Sleep Mode Indicator Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (timerState.isRunning) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (timerState.isRunning) Color(0xFF10B981).copy(alpha = 0.35f) else Color.Transparent
                ),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("sleep_mode_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = if (timerState.isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (timerState.isRunning) {
                            if (timerState.isPaused) "Sleep Mode Paused" else "Sleep Mode Active • Runs even when mobile sleeps"
                        } else {
                            "Sleep Mode Ready • Keeps running when screen turns off"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (timerState.isRunning) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Buttons & Setup
            if (timerState.isRunning) {
                // Running controls: Pause/Resume and Abandon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Abandon Button
                    OutlinedButton(
                        onClick = { viewModel.promptAbandon() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF87171)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("abandon_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Give Up")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Give Up", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Pause / Resume Button
                    Button(
                        onClick = {
                            if (timerState.isPaused) viewModel.resumeTimer() else viewModel.pauseTimer()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerState.isPaused) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .width(140.dp)
                            .testTag("pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (timerState.isPaused) "Resume" else "Pause",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (timerState.isPaused) "Resume" else "Pause",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Idle / Setup controls
                if (timerState.isWithered) {
                    Button(
                        onClick = { viewModel.resetWitheredState() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                            .testTag("reset_withered_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Ash & Prepare New Seed", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Duration Selector
                Text(
                    text = "FOCUS SPRINT DURATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                val durations = listOf(15, 25, 45, 60, 90)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durations.forEach { duration ->
                        val isSelected = timerState.totalDurationSeconds == duration * 60
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectDuration(duration) },
                            label = {
                                Text(
                                    text = when (duration) {
                                        25 -> "25m (Pomodoro)"
                                        60 -> "60m (Deep Draft)"
                                        90 -> "90m (Lit Sprint)"
                                        else -> "${duration}m"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("duration_chip_$duration")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thesis Task Tag Selector
                Text(
                    text = "THESIS CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThesisTaskTag.entries.forEach { tag ->
                        val isSelected = timerState.selectedTaskTag == tag
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(tag.colorLong).copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(tag.colorLong) else Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable { viewModel.selectTaskTag(tag) }
                                .testTag("tag_chip_${tag.id}")
                        ) {
                            Text(
                                text = tag.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tree Species Picker
                Text(
                    text = "MYTHICAL TREE TO GROW",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TreeSpecies.entries.forEach { species ->
                        val isUnlocked = userLevel >= species.requiredLevel
                        val isSelected = timerState.selectedTreeSpecies == species

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                !isUnlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                isSelected -> Color(species.primaryColor).copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(species.primaryColor) else Color.Transparent
                            ),
                            modifier = Modifier
                                .clickable(enabled = isUnlocked) { viewModel.selectTreeSpecies(species) }
                                .testTag("species_chip_${species.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Lv.${species.requiredLevel} ${species.displayName}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(species.foliageColor))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = species.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(species.foliageColor) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Session Note / Target
                OutlinedTextField(
                    value = timerState.sessionNotes,
                    onValueChange = { viewModel.updateSessionNotes(it) },
                    placeholder = { Text("Goal for this sprint (e.g., Draft Section 3.2 Methodology)", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_note_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // START QUEST BUTTON
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } catch (_: Exception) {
                            }
                        }
                        viewModel.startTimer()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("start_timer_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Plant Seed & Focus (${timerState.totalDurationSeconds / 60}m)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Abandon Confirmation Dialog
        if (timerState.showAbandonDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAbandonDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Abandon Focus Quest?",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Giving up now will wither your growing tree into dry ash. Are you sure you want to stop working on your thesis?",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmAbandon() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.testTag("confirm_abandon_button")
                    ) {
                        Text("Abandon & Wither", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissAbandonDialog() },
                        modifier = Modifier.testTag("cancel_abandon_button")
                    ) {
                        Text("Keep Focusing", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Celebration Dialog
        if (timerState.showCelebrationDialog && timerState.lastCompletedSession != null) {
            val session = timerState.lastCompletedSession!!
            val species = TreeSpecies.fromId(session.treeSpeciesId)

            AlertDialog(
                onDismissRequest = { viewModel.dismissCelebration() },
                icon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Thesis Quest Complete!",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You successfully cultivated a majestic ${species.displayName}!",
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Duration Focused:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${session.durationMinutes} minutes", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Arcane XP Earned:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+${session.xpEarned} XP", fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Current Streak:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$streakDays Days 🔥", fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissCelebration() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dismiss_celebration_button")
                    ) {
                        Text("Add to Grove & Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
