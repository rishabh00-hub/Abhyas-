package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudySession
import com.example.ui.StudyViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: StudyViewModel) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val sessions by viewModel.allSessions.collectAsState()

    var showManualForm by remember { mutableStateOf(false) }

    // Manual Entry Form State
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Physics") }
    var selectedType by remember { mutableStateOf("Self Study") }
    var durationMinutes by remember { mutableStateOf("45") }
    var notes by remember { mutableStateOf("") }

    val subjectsList = listOf("Physics", "Chemistry", "Maths", "Biology", "General", "Other")
    val typesList = listOf("Lecture", "DPP", "Homework", "Backlog", "Revision", "Self Study", "Test")

    // Timeline calculations setup
    val calendar = Calendar.getInstance()
    val nowTime = Date()

    // Filtered chronological sessions List
    val filteredSessions = remember(
        sessions,
        viewModel.historySubjectFilter,
        viewModel.historyTimelineFilter
    ) {
        sessions.filter { s ->
            // 1. Subject Filter check
            val matchesSubject = viewModel.historySubjectFilter == "All" || s.subject == viewModel.historySubjectFilter

            // 2. Timeline filter check (Convert startTime to Date)
            val matchesTimeline = if (viewModel.historyTimelineFilter == "All") {
                true
            } else {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                    val itemDate = sdf.parse(s.startTime) ?: nowTime
                    val diffMillis = nowTime.time - itemDate.time

                    when (viewModel.historyTimelineFilter) {
                        "Day" -> diffMillis <= 24 * 60 * 60 * 1000L
                        "Week" -> diffMillis <= 7 * 24 * 60 * 60 * 1000L
                        "Month" -> diffMillis <= 30 * 24 * 60 * 60 * 1000L
                        else -> true
                    }
                } catch (e: Exception) {
                    true
                }
            }

            matchesSubject && matchesTimeline
        }
    }

    // Cumulative minutes clocked statistics
    val cumulativeMinutesFiltered = remember(filteredSessions) {
        var totalSecs = 0
        filteredSessions.forEach {
            totalSecs += it.durationSeconds
        }
        totalSecs / 60
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
    ) {
        // --- HEADER ---
        item {
            Column {
                Text(
                    text = "Abhyas Study Archives",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicPrimary,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Historical Self-Study Logs",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (viewModel.isHistoryEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = "History State Icon",
                            tint = if (viewModel.isHistoryEnabled) CosmicSecondary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "History Logging: ${if (viewModel.isHistoryEnabled) "ON" else "OFF"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (viewModel.isHistoryEnabled) "Completed targets are saved to study archives." else "Disabled — completed targets will not be logged.",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = viewModel.isHistoryEnabled,
                        onCheckedChange = { viewModel.toggleHistoryEnabled() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CosmicPrimary,
                            checkedTrackColor = CosmicPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // --- CUMULATIVE CHRONICLE STATISTICS OVERVIEW CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timeline, contentDescription = "", tint = CosmicSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chronicle Summary Stats", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }

                        // Floating minutes badge display
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CosmicPrimary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CosmicPrimary)
                        ) {
                            Text(
                                text = "${cumulativeMinutesFiltered} Minutes Clocked",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filters Toolbar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Subject filter Selector dropdown
                        var subExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { subExp = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                border = BorderStroke(1.dp, CosmicBorder)
                            ) {
                                Text(
                                    text = "Sub: ${viewModel.historySubjectFilter}",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color.LightGray
                                )
                            }
                            DropdownMenu(
                                expanded = subExp,
                                onDismissRequest = { subExp = false },
                                modifier = Modifier.background(CosmicSurfaceVariant)
                            ) {
                                (listOf("All") + subjectsList).forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            viewModel.historySubjectFilter = s
                                            subExp = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Timeline filter Selector choice dropdown
                        var timeExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { timeExp = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                border = BorderStroke(1.dp, CosmicBorder)
                            ) {
                                Text(
                                    text = "Range: ${viewModel.historyTimelineFilter}",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = timeExp,
                                onDismissRequest = { timeExp = false },
                                modifier = Modifier.background(CosmicSurfaceVariant)
                            ) {
                                listOf("All", "Day", "Week", "Month").forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = {
                                            viewModel.historyTimelineFilter = s
                                            timeExp = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- MANUAL HISTORICAL OVERRIDE INPUT FORM ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showManualForm = !showManualForm }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Override done",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Manual Study Overrides Entry",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (showManualForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand manual override form",
                            tint = Color.Gray
                        )
                    }

                    AnimatedVisibility(
                        visible = showManualForm,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Log Entry Title / Chapter studied", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("add_history_title_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedIndicatorColor = CosmicPrimary,
                                    unfocusedIndicatorColor = CosmicBorder
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Subject chooser dropdown in manual entry
                                var subExp by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { subExp = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, CosmicBorder)
                                    ) {
                                        Text(
                                            text = "Subject: $selectedSubject",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = subExp,
                                        onDismissRequest = { subExp = false },
                                        modifier = Modifier.background(CosmicSurfaceVariant)
                                    ) {
                                        subjectsList.forEach { s ->
                                            DropdownMenuItem(
                                                text = { Text(s, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = {
                                                    selectedSubject = s
                                                    subExp = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Category choice
                                var typeExp by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { typeExp = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, CosmicBorder)
                                    ) {
                                        Text(
                                            text = "Type: $selectedType",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = typeExp,
                                        onDismissRequest = { typeExp = false },
                                        modifier = Modifier.background(CosmicSurfaceVariant)
                                    ) {
                                        typesList.forEach { t ->
                                            DropdownMenuItem(
                                                text = { Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                onClick = {
                                                    selectedType = t
                                                    typeExp = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = durationMinutes,
                                    onValueChange = { durationMinutes = it },
                                    label = { Text("Study Duration (Minutes Done)", color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Custom Notes or DPP sheets overview", color = Color.Gray) },
                                singleLine = false,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Button(
                                onClick = {
                                    val mins = durationMinutes.toIntOrNull() ?: 45
                                    if (mins > 0) {
                                        val entryTitle = title.trim().ifEmpty { "$selectedType Class Override" }
                                        viewModel.addManualStudyLog(
                                            title = entryTitle,
                                            subject = selectedSubject,
                                            type = selectedType,
                                            minutes = mins,
                                            notes = notes.ifEmpty { null }
                                        )
                                        title = ""
                                        notes = ""
                                        showManualForm = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_manual_history_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                            ) {
                                Text("Record Historical Period", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- EMPTY STATE IF NO ARCHIVED LOGS ---
        if (filteredSessions.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Empty History",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Archived Study logs empty.",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Log focused timer periods or insert override logs above!",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // --- ARCHIVED LIST TIMELINE ITEMS ---
            items(filteredSessions, key = { it.id }) { session ->
                SessionHistoryItemCard(
                    session = session,
                    onDelete = { viewModel.deleteSessionLog(session.id) }
                )
            }
        }
    }
}

// --- SUBCOMPONENT: HISTORY SESSION CARD ---
@Composable
fun SessionHistoryItemCard(
    session: StudySession,
    onDelete: () -> Unit
) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val subjectLabelColor = when (session.subject) {
        "Physics" -> ColorPhysics
        "Chemistry" -> ColorChemistry
        "Maths" -> ColorMaths
        "Biology" -> ColorBiology
        "General" -> ColorGeneral
        else -> ColorOther
    }

    val totalMin = session.durationSeconds / 60
    val leftSec = session.durationSeconds % 60

    val rawDateStr = try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(session.startTime) ?: Date()
        outputFormat.format(date)
    } catch (e: Exception) {
        session.startTime
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(subjectLabelColor))
                    Text(
                        text = "${session.subject} · ${session.type}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = if (session.notes != null) "Focus: ${session.notes}" else "Study Block Done",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (isExpanded) 20 else 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!isExpanded && session.notes != null && (session.notes.contains("\n") || session.notes.length > 40)) {
                    Text(
                        text = "▼ Tap to expand notes & checkpoints",
                        color = CosmicSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = rawDateStr,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CosmicSurfaceVariant,
                    border = BorderStroke(0.5.dp, CosmicBorder)
                ) {
                    Text(
                        text = "${totalMin}m ${leftSec}s",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_session_log_btn_${session.id}")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
