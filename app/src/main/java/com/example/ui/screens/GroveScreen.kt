package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusSessionEntity
import com.example.data.model.ThesisTaskTag
import com.example.data.model.TreeSpecies
import com.example.ui.components.ForestTreeGraphic
import com.example.ui.viewmodel.ThesisFocusViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GroveScreen(
    viewModel: ThesisFocusViewModel,
    onNavigateToSanctuary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allSessions by viewModel.allSessions.collectAsState()
    val completedSessions by viewModel.completedSessions.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedTreeDetails by remember { mutableStateOf<FocusSessionEntity?>(null) }
    var treeToDelete by remember { mutableStateOf<FocusSessionEntity?>(null) }

    val filteredList = when (selectedFilter) {
        "COMPLETED" -> allSessions.filter { it.isCompleted }
        "WITHERED" -> allSessions.filter { !it.isCompleted }
        "WRITING" -> allSessions.filter { it.taskTag == ThesisTaskTag.CHAPTER_WRITING.id }
        "LIT" -> allSessions.filter { it.taskTag == ThesisTaskTag.LITERATURE_REVIEW.id }
        else -> allSessions
    }

    val totalHours = completedSessions.sumOf { it.durationMinutes } / 60
    val totalMins = completedSessions.sumOf { it.durationMinutes } % 60
    val witheredCount = allSessions.count { !it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Overview Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Park,
                        contentDescription = "Thesis Grove",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "The Thesis Grove",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    GroveStatItem(
                        label = "Cultivated",
                        value = "${completedSessions.size}",
                        accentColor = Color(0xFF34D399)
                    )
                    GroveStatItem(
                        label = "Deep Hours",
                        value = if (totalHours > 0) "${totalHours}h ${totalMins}m" else "${totalMins}m",
                        accentColor = Color(0xFFFBBF24)
                    )
                    GroveStatItem(
                        label = "Withered",
                        value = "$witheredCount",
                        accentColor = if (witheredCount > 0) Color(0xFFEF4444) else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All Trees (${allSessions.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("filter_all_trees")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "COMPLETED",
                    onClick = { selectedFilter = "COMPLETED" },
                    label = { Text("Thriving (${completedSessions.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("filter_completed_trees")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "WRITING",
                    onClick = { selectedFilter = "WRITING" },
                    label = { Text("Chapter Writing") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF59E0B),
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("filter_writing_trees")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "LIT",
                    onClick = { selectedFilter = "LIT" },
                    label = { Text("Literature Review") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3B82F6),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_lit_trees")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "WITHERED",
                    onClick = { selectedFilter = "WITHERED" },
                    label = { Text("Withered ($witheredCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_withered_trees")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Empty state
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your Thesis Grove is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Each focus sprint plants an ancient tree dedicated to your thesis chapters. Start a session to grow your first seedling!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onNavigateToSanctuary,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.testTag("empty_grove_start_button")
                    ) {
                        Text("Enter Focus Sanctuary", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Trees Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("grove_trees_grid")
            ) {
                items(filteredList, key = { it.id }) { session ->
                    val species = TreeSpecies.fromId(session.treeSpeciesId)
                    val tag = ThesisTaskTag.fromId(session.taskTag)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTreeDetails = session }
                            .testTag("grove_tree_card_${session.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (session.isCompleted) Color(species.primaryColor).copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.25f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Mini Forest Tree Graphic
                            ForestTreeGraphic(
                                progress = if (session.isCompleted) 1.0f else 0.4f,
                                species = species,
                                isWithered = !session.isCompleted,
                                sizeDp = 100.dp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (session.isCompleted) species.displayName else "Withered Ash",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (session.isCompleted) Color.White else Color(0xFF9E9E9E)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Task Tag Chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(tag.colorLong).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tag.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(tag.colorLong),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Duration & XP
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${session.durationMinutes}m",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "+${session.xpEarned} XP",
                                    fontSize = 11.sp,
                                    color = if (session.isCompleted) Color(0xFFFBBF24) else Color(0xFF9E9E9E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog when tree is tapped
    if (selectedTreeDetails != null) {
        val session = selectedTreeDetails!!
        val species = TreeSpecies.fromId(session.treeSpeciesId)
        val tag = ThesisTaskTag.fromId(session.taskTag)
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        val formattedDate = sdf.format(Date(session.startTime))

        AlertDialog(
            onDismissRequest = { selectedTreeDetails = null },
            icon = {
                ForestTreeGraphic(
                    progress = if (session.isCompleted) 1.0f else 0.4f,
                    species = species,
                    isWithered = !session.isCompleted,
                    sizeDp = 120.dp
                )
            },
            title = {
                Text(
                    text = if (session.isCompleted) species.displayName else "Withered Tree",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (session.isCompleted) species.description else "This session was abandoned early.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            InfoRow(label = "Thesis Category", value = tag.label)
                            InfoRow(label = "Focus Duration", value = "${session.durationMinutes} minutes")
                            InfoRow(label = "Arcane XP", value = "+${session.xpEarned} XP")
                            InfoRow(label = "Planted On", value = formattedDate)
                            if (session.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notes: \"${session.note}\"",
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTreeDetails = null }) {
                    Text("Close", color = Color(0xFF10B981))
                }
            },
            dismissButton = {
                IconButton(
                    onClick = {
                        val toDel = selectedTreeDetails
                        selectedTreeDetails = null
                        treeToDelete = toDel
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (treeToDelete != null) {
        AlertDialog(
            onDismissRequest = { treeToDelete = null },
            title = { Text("Remove from Grove?") },
            text = { Text("Are you sure you want to remove this session record from your grove history?") },
            confirmButton = {
                Button(
                    onClick = {
                        treeToDelete?.let { viewModel.deleteSession(it.id) }
                        treeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { treeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GroveStatItem(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
