package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThesisTaskTag
import com.example.ui.viewmodel.DayFocusStat
import com.example.ui.viewmodel.ThesisFocusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyAnalyticsScreen(
    viewModel: ThesisFocusViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val todayMinutes by viewModel.todayMinutes.collectAsState()
    val todaySessions by viewModel.todaySessions.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()

    val goalMinutes = profile?.dailyGoalMinutes ?: 120
    val currentStreak = profile?.currentStreak ?: 0
    val bestStreak = profile?.bestStreak ?: 0
    val isGoalAchieved = todayMinutes >= goalMinutes
    val progress = (todayMinutes.toFloat() / goalMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    var showGoalEditor by remember { mutableStateOf(false) }
    var tempGoalSlider by remember(goalMinutes) { mutableFloatStateOf(goalMinutes.toFloat()) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Today's Hero Goal Progress Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("today_goal_card"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(
                width = 1.dp,
                color = if (isGoalAchieved) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFFF5722).copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Today's Thesis Work",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Goal edit pill
                    Surface(
                        onClick = { showGoalEditor = !showGoalEditor },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("edit_daily_goal_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Goal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val goalH = goalMinutes / 60
                            val goalMRem = goalMinutes % 60
                            val goalText = if (goalH > 0 && goalMRem > 0) "${goalH}h ${goalMRem}m" else if (goalH > 0) "${goalH}h" else "${goalMinutes}m"
                            Text(
                                text = "Goal: $goalText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Circular Progress with minutes in the center
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = if (isGoalAchieved) Color(0xFF10B981) else Color(0xFFFF9800),
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeWidth = 12.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val hours = todayMinutes / 60
                        val mins = todayMinutes % 60
                        val displayMinutes = if (hours > 0) "${hours}h ${mins}m" else "${todayMinutes}m"

                        Text(
                            text = displayMinutes,
                            fontSize = if (hours > 0) 26.sp else 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "of ${goalMinutes}m goal",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Streak Status Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isGoalAchieved) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFFF5722).copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isGoalAchieved) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFFF5722).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isGoalAchieved) Icons.Default.WorkspacePremium else Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (isGoalAchieved) Color(0xFF10B981) else Color(0xFFFF5722),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isGoalAchieved) "Daily Goal Met! Streak Extended! 🔥" else "${(goalMinutes - todayMinutes).coerceAtLeast(0)}m remaining for streak",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGoalAchieved) Color(0xFF34D399) else Color(0xFFFFB74D)
                            )
                            Text(
                                text = if (isGoalAchieved) "You have secured your focus streak for today." else "Hit your target to build or maintain your focus streak.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Expandable Goal Editor
        if (showGoalEditor) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Customize Daily Thesis Target",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set how many focused minutes you aim for each day to earn your streak flame.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val presetGoals = listOf(60, 120, 180, 240, 300, 360, 420, 480)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetGoals.forEach { preset ->
                            FilterChip(
                                selected = tempGoalSlider.toInt() == preset,
                                onClick = {
                                    tempGoalSlider = preset.toFloat()
                                    viewModel.updateDailyGoal(preset)
                                },
                                label = { Text("${preset / 60}h (${preset}m)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = tempGoalSlider,
                        onValueChange = {
                            tempGoalSlider = (Math.round(it / 15f) * 15).coerceIn(30, 480).toFloat()
                        },
                        onValueChangeFinished = {
                            viewModel.updateDailyGoal(tempGoalSlider.toInt())
                        },
                        valueRange = 30f..480f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF10B981),
                            activeTrackColor = Color(0xFF10B981)
                        )
                    )

                    val targetH = tempGoalSlider.toInt() / 60
                    val targetM = tempGoalSlider.toInt() % 60
                    val targetDesc = if (targetH > 0 && targetM > 0) {
                        "${targetH} hours ${targetM} mins (${tempGoalSlider.toInt()}m)"
                    } else if (targetH > 0) {
                        "${targetH} hours (${tempGoalSlider.toInt()}m)"
                    } else {
                        "${tempGoalSlider.toInt()} minutes"
                    }

                    Text(
                        text = "Target: $targetDesc / day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Streak Statistics Cards (Current Streak & Best Streak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currentStreak",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB74D)
                    )
                    Text(
                        text = "Current Streak",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$bestStreak",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = "Best Streak",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 7-Day Thesis Activity Weekly Chart
        Text(
            text = "PAST 7 DAYS ACTIVITY",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxMins = (weeklyStats.maxOfOrNull { it.totalMinutes } ?: goalMinutes).coerceAtLeast(goalMinutes)

                    weeklyStats.forEach { dayStat ->
                        val barHeightRatio = (dayStat.totalMinutes.toFloat() / maxMins.toFloat()).coerceIn(0.06f, 1f)
                        val isToday = dayStat.dateString == viewModel.todayDateStr

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (dayStat.isGoalAchieved) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5722),
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Vertical Bar
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height((90 * barHeightRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        when {
                                            dayStat.isGoalAchieved -> Color(0xFF10B981)
                                            dayStat.totalMinutes > 0 -> Color(0xFFF59E0B)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = dayStat.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Today's Thesis Tasks Breakdown
        Text(
            text = "TODAY'S THESIS BREAKDOWN",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (todaySessions.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No focus sessions logged yet today. Start your first sprint in the Sanctuary!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            ThesisTaskTag.entries.forEach { tag ->
                val tagSessions = todaySessions.filter { it.taskTag == tag.id && it.isCompleted }
                val tagMinutes = tagSessions.sumOf { it.durationMinutes }

                if (tagMinutes > 0) {
                    val ratio = (tagMinutes.toFloat() / todayMinutes.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tag.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(tag.colorLong)
                            )
                            Text(
                                text = "${tagMinutes}m (${(ratio * 100).toInt()}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(tag.colorLong),
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recorded Sessions Detail List
            Text(
                text = "TODAY'S RECORDED LOGS (${todaySessions.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            todaySessions.forEach { session ->
                val tag = ThesisTaskTag.fromId(session.taskTag)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val startTimeStr = timeFormat.format(Date(session.startTime))
                val isBonus = session.note.contains("Bonus")

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        if (session.isCompleted) Color(0xFF10B981).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (session.isCompleted) Icons.Filled.Done else Icons.Filled.Close,
                                contentDescription = null,
                                tint = if (session.isCompleted) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tag.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(tag.colorLong)
                                    )
                                    if (isBonus) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "🌟 Bonus",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFBBF24),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Started at $startTimeStr • ${if (session.isCompleted) "+${session.xpEarned} XP" else "Withered"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "${session.durationMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = if (session.isCompleted) Color.White else Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
