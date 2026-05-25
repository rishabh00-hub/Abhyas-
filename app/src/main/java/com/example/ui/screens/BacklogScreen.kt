package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BacklogItem
import com.example.ui.StudyViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BacklogScreen(viewModel: StudyViewModel) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val backlogs by viewModel.allBacklogs.collectAsState()
    val context = LocalContext.current

    var showAddForm by remember { mutableStateOf(false) }

    // Form inputs
    var title by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var selectedSubject by remember { mutableStateOf("Physics") }
    var selectedType by remember { mutableStateOf("Lecture") }
    var selectedDifficulty by remember { mutableStateOf("Medium") } // "Easy", "Medium", "Critical"
    var notes by remember { mutableStateOf("") }

    // Custom subject insertion State
    var showCustomSubjectDialog by remember { mutableStateOf(false) }
    var customSubjectInputText by remember { mutableStateOf("") }

    val defaultSubjects = listOf("Physics", "Chemistry", "Maths", "Biology", "General", "Other")
    val dynamicCustomSubjects by viewModel.customSubjects
    val subjectsList = remember(dynamicCustomSubjects) {
        defaultSubjects + dynamicCustomSubjects.toList()
    }

    val typesList = listOf("Lecture", "DPP", "Homework", "Backlog", "Revision", "Self Study", "Test")
    val difficultyList = listOf("Easy", "Medium", "Critical")
    val isCompact = LocalConfiguration.current.screenWidthDp < 360

    val pendingCount = remember(backlogs) {
        backlogs.count { it.status == "pending" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
    ) {
        // --- HEADER & PENDING METRICS BADGE ---
        item {
            Column {
                Text(
                    text = "Abhyas Study Debt Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backlog Sheet Register",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Badge(
                        containerColor = if (pendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.scaleModifier(1.1f)
                    ) {
                        Text(
                            text = "$pendingCount Pending Backlogs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- ADD BACKLOG EXPANDABLE CARD ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backlog_collapsible_card"),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddForm = !showAddForm }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                            .testTag("backlog_header_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Add Debt",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Register Outstanding Backlog Debt",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showAddForm) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = {
                                    title = it
                                    titleError = false
                                },
                                label = { Text("Backlog Title (Why did it accumulate?)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                singleLine = true,
                                isError = titleError,
                                modifier = Modifier.fillMaxWidth().testTag("add_backlog_title_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = CosmicBorder
                                )
                            )

                            // Subject select with Add Custom Subject Action Button
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = if (isCompact) 1 else 2
                            ) {
                                var subExp by remember { mutableStateOf(false) }
                                Box(modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
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
                                            fontSize = 12.sp
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

                                // Custom Subject Quick Button
                                Button(
                                    onClick = { showCustomSubjectDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                                    border = BorderStroke(1.dp, CosmicBorder),
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom subject")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New sub", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = if (isCompact) 1 else 2
                            ) {
                                // Category select
                                var typeExp by remember { mutableStateOf(false) }
                                Box(modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
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
                                            fontSize = 12.sp
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

                                // Difficulty choices select
                                var diffExp by remember { mutableStateOf(false) }
                                Box(modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { diffExp = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                        border = BorderStroke(1.dp, CosmicBorder)
                                    ) {
                                        Text(
                                            text = "Priority: $selectedDifficulty",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 12.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = diffExp,
                                        onDismissRequest = { diffExp = false },
                                        modifier = Modifier.background(CosmicSurfaceVariant)
                                    ) {
                                        difficultyList.forEach { d ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = d,
                                                        color = if (d == "Critical") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = if (d == "Critical") FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    selectedDifficulty = d
                                                    diffExp = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Annotations/Notes (Optional)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                                    if (title.isBlank()) {
                                        titleError = true
                                        Toast.makeText(context, "Title is required", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.addBacklog(
                                        title = title,
                                        subject = selectedSubject,
                                        type = selectedType,
                                        difficulty = selectedDifficulty,
                                        notes = notes.ifEmpty { null }
                                    )
                                    title = ""
                                    titleError = false
                                    notes = ""
                                    showAddForm = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_backlog_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Register Backlog Debt", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }

        // --- EMPTY STATE IF NO BACKLOG DEBT ---
        if (backlogs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SentimentSatisfied,
                        contentDescription = "Zero Backlog",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Clean Ledger! Zero Backlogs Detected.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Nice going! Keep up this offline momentum.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // --- BACKLOG LOGS TIMELINE RENDER ---
            items(backlogs, key = { it.id }) { item ->
                BacklogCard(
                    item = item,
                    onToggleResolve = { viewModel.resolveBacklog(item) },
                    onDelete = { viewModel.deleteBacklog(item.id) },
                    onConvertToTarget = { viewModel.convertBacklogToTarget(item) }
                )
            }
        }
    }

    // Custom subject insertion dialogue
    if (showCustomSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSubjectDialog = false },
            title = { Text("Add Custom Study Subject", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Introduce subjects dynamically to track custom lectures.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customSubjectInputText,
                        onValueChange = { customSubjectInputText = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurface,
                            unfocusedContainerColor = CosmicSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customSubjectInputText.trim().isNotEmpty()) {
                            viewModel.addCustomSubject(customSubjectInputText)
                            selectedSubject = customSubjectInputText.trim()
                            customSubjectInputText = ""
                            showCustomSubjectDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Subject", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSubjectDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = CosmicSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- SUBCOMPONENT: INDIVIDUAL BACKLOG CARD ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BacklogCard(
    item: BacklogItem,
    onToggleResolve: () -> Unit,
    onDelete: () -> Unit,
    onConvertToTarget: () -> Unit
) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val isResolved = item.status == "resolved"
    val isCritical = item.difficulty == "Critical"
    val isCompact = LocalConfiguration.current.screenWidthDp < 360

    val ringBorderColor = when {
        isResolved -> MaterialTheme.colorScheme.tertiary
        isCritical -> MaterialTheme.colorScheme.error
        item.difficulty == "Medium" -> MaterialTheme.colorScheme.tertiary
        else -> CosmicBorder
    }

    val subjectLabelColor = when (item.subject) {
        "Physics" -> MaterialTheme.colorScheme.primary
        "Chemistry" -> MaterialTheme.colorScheme.secondary
        "Maths" -> MaterialTheme.colorScheme.tertiary
        "Biology" -> MaterialTheme.colorScheme.primary
        "General" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backlog_item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, ringBorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Status, subject, difficulty badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(subjectLabelColor)
                    )
                    Text(
                        text = "${item.subject} · ${item.type}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (item.difficulty) {
                        "Critical" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        "Medium" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    border = BorderStroke(
                        0.5.dp,
                        when (item.difficulty) {
                            "Critical" -> MaterialTheme.colorScheme.error
                            "Medium" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = item.difficulty.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.difficulty) {
                            "Critical" -> MaterialTheme.colorScheme.error
                            "Medium" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: title and details
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isResolved) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isResolved) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.notes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.notes,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Immediate actions footer bar
            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Resolve status check
                        IconButton(
                            onClick = onToggleResolve,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CosmicSurfaceVariant)
                                .testTag("resolve_backlog_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isResolved) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = "Resolve toggle",
                                tint = if (isResolved) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete debt
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CosmicSurfaceVariant)
                                .testTag("delete_backlog_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (!isResolved) {
                        Button(
                            onClick = onConvertToTarget,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("convert_backlog_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Convert",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active target me dalein", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        Text(
                            text = "RESOLVED STUDY DEBT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Resolve status check
                        IconButton(
                            onClick = onToggleResolve,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CosmicSurfaceVariant)
                                .testTag("resolve_backlog_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isResolved) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = "Resolve toggle",
                                tint = if (isResolved) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Delete debt
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CosmicSurfaceVariant)
                                .testTag("delete_backlog_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Convert to Active Daily Target
                    if (!isResolved) {
                        Button(
                            onClick = onConvertToTarget,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("convert_backlog_btn_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Convert",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active target me dalein", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    } else {
                        Text(
                            text = "RESOLVED STUDY DEBT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

// Simple modifier density scale helper
fun Modifier.scaleModifier(scale: Float) = this.then(Modifier.scale(scale))
