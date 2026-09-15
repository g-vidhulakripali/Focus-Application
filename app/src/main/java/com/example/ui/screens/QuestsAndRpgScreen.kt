package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThesisMilestoneQuestEntity
import com.example.ui.components.RpgHeroCard
import com.example.ui.viewmodel.ThesisFocusViewModel

@Composable
fun QuestsAndRpgScreen(
    viewModel: ThesisFocusViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val quests by viewModel.allQuests.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Quests, 1: Level Progression & Relics

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // RPG Hero Card
        RpgHeroCard(
            profile = profile,
            onViewQuests = { selectedTab = 0 }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs: Quests vs Level Milestones
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = Color(0xFF10B981),
            modifier = Modifier.clip(RoundedCornerShape(14.dp))
        ) {
            val claimableCount = quests.count { it.isCompleted && !it.isClaimed }
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Thesis Quests",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                        if (claimableCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = "$claimableCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.testTag("tab_quests")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "Ranks & Relics",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag("tab_progression")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Thesis Quests Tab
            Text(
                text = "ACADEMIC MILESTONES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            quests.forEach { quest ->
                QuestItemCard(
                    quest = quest,
                    onClaim = { viewModel.claimQuest(quest.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        } else {
            // Level Progression & Academic Relics Tab
            val userLevel = profile?.scholarLevel ?: 1

            Text(
                text = "SCHOLAR ACADEMIC RANKS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val ranks = listOf(
                Pair(1, "Novice Thesis Scribe (Level 1)"),
                Pair(2, "Scholar's Apprentice (Level 2) - Unlocks Ancient Oak"),
                Pair(4, "Thesis Researcher (Level 4) - Unlocks Aether Willow"),
                Pair(7, "Master Academician (Level 7) - Unlocks Golden Bodhi"),
                Pair(10, "Arch-Scholar of Knowledge (Level 10) - Unlocks World-Tree"),
                Pair(15, "Doctor of Philosophy (PhD Grandmaster)")
            )

            ranks.forEach { (lvl, rankTitle) ->
                val isReached = userLevel >= lvl
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isReached) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (isReached) Color(0xFF10B981).copy(alpha = 0.4f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isReached) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isReached) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isReached) Color.Black else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = rankTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isReached) FontWeight.Bold else FontWeight.Medium,
                                color = if (isReached) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isReached) "Rank Unlocked" else "Requires Level $lvl",
                                fontSize = 11.sp,
                                color = if (isReached) Color(0xFF34D399) else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SCHOLAR ARTIFACTS & RELICS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val relics = listOf(
                Triple("Golden Quill of Diligence", "Grants +10% bonus Arcane XP on all completed sprints.", userLevel >= 2),
                Triple("Codex of Endless Citations", "Reduces cognitive fatigue; radiates serene forest ambiance.", userLevel >= 4),
                Triple("Hourglass of Deep Flow", "Protects daily streak against accidental breaks once per week.", userLevel >= 6),
                Triple("Dissertation Defense Aegis", "Crown of the defended thesis; radiant cosmic aura in grove.", userLevel >= 10)
            )

            relics.forEach { (name, desc, unlocked) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (unlocked) Color(0xFFF59E0B).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(
                        1.dp,
                        if (unlocked) Color(0xFFF59E0B).copy(alpha = 0.35f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (unlocked) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (unlocked) Icons.Default.MilitaryTech else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (unlocked) Color(0xFFFBBF24) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (unlocked) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = desc,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun QuestItemCard(
    quest: ThesisMilestoneQuestEntity,
    onClaim: () -> Unit
) {
    val progressRatio = (quest.currentValue.toFloat() / quest.targetValue.toFloat()).coerceIn(0f, 1f)
    val isReadyToClaim = quest.isCompleted && !quest.isClaimed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quest_card_${quest.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.dp,
            when {
                quest.isClaimed -> Color(0xFF10B981).copy(alpha = 0.2f)
                isReadyToClaim -> Color(0xFFF59E0B)
                else -> Color.Transparent
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quest Category Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                quest.isClaimed -> Color(0xFF10B981).copy(alpha = 0.2f)
                                isReadyToClaim -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                ) {
                    Icon(
                        imageVector = getIconForQuest(quest.iconName),
                        contentDescription = null,
                        tint = when {
                            quest.isClaimed -> Color(0xFF10B981)
                            isReadyToClaim -> Color(0xFFFBBF24)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // XP Reward Pill or Claim Button
                if (isReadyToClaim) {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("claim_quest_button_${quest.id}")
                    ) {
                        Text(
                            text = "Claim +${quest.xpReward} XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                } else if (quest.isClaimed) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Claimed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "+${quest.xpReward} XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!quest.isClaimed) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (quest.isCompleted) Color(0xFF10B981) else Color(0xFF38BDF8),
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "${quest.currentValue} / ${quest.targetValue}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getIconForQuest(iconName: String): ImageVector {
    return when (iconName) {
        "timer" -> Icons.Default.Timer
        "menu_book" -> Icons.Default.MenuBook
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "trending_up" -> Icons.Default.TrendingUp
        "park" -> Icons.Default.Park
        "military_tech" -> Icons.Default.MilitaryTech
        "school" -> Icons.Default.School
        else -> Icons.Default.AutoAwesome
    }
}
