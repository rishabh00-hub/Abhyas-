package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyAspiration
import com.example.data.DailyTarget
import com.example.ui.AutoAddConfig
import com.example.ui.StudyViewModel
import com.example.ui.TargetTemplate
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetsScreen(viewModel: StudyViewModel) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val targets by viewModel.allTargets.collectAsState()
    val aspirations by viewModel.allAspirations.collectAsState()

    // Form states
    var title by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("45") }
    var selectedSubject by remember { mutableStateOf("Physics") }
    var selectedType by remember { mutableStateOf("Lecture") }
    var targetDateText by remember { mutableStateOf("") } // YYYY-MM-DD
    var questionsCount by remember { mutableStateOf("") } // optional DPP count
    var dateError by remember { mutableStateOf<String?>(null) }
    
    // New customized fields
    var chapterText by remember { mutableStateOf("") }
    var lectureNumberText by remember { mutableStateOf("") }
    var batchText by remember { mutableStateOf("") }
    var autoAddEnabled by remember { mutableStateOf(false) }
    var autoAddMaxLecture by remember { mutableStateOf("") }
    var autoAddEndDate by remember { mutableStateOf("") }
    var autoAddError by remember { mutableStateOf<String?>(null) }

    // Dialog flags
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showAddPresetDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val prescheduleIndex = 6
    val scrollToPrescheduleForm = {
        viewModel.isTargetFormExpanded = true
        coroutineScope.launch {
            listState.animateScrollToItem(prescheduleIndex)
        }
    }

    val subjectsList = listOf("Physics", "Chemistry", "Maths", "Biology", "General", "Other")
    val typesList = listOf("Lecture", "DPP", "Homework", "Backlog", "Revision", "Self Study", "Test")

    // Filter targets list based on tab
    val activeFilter = viewModel.targetDateFilter
    val todayStr = viewModel.todayDate

    val filteredTargets = remember(targets, activeFilter, todayStr) {
        targets.filter { target ->
            when (activeFilter) {
                "today" -> target.targetDate == todayStr
                "upcoming" -> target.targetDate > todayStr
                "all" -> true
                else -> true
            }
        }
    }

    // Pre-fill helper from preset template
    val onSelectTemplate = { template: TargetTemplate ->
        selectedSubject = template.subject
        selectedType = template.type
        batchText = template.batch
        chapterText = template.chapter
        durationMinutes = template.durationMinutes.toString()
        
        // Dynamic smart lecture estimator
        val matchingTargets = targets.filter { 
            it.subject == template.subject && 
            it.type == template.type && 
            it.chapter?.equals(template.chapter, ignoreCase = true) == true 
        }
        val highLec = matchingTargets.mapNotNull { target ->
            val lecStr = target.lectureNumber ?: target.title
            val regex = "Lec[\\s-_]*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(lecStr)
            match?.groupValues?.get(1)?.toIntOrNull()
        }.maxOrNull()

        if (highLec != null) {
            val nextLec = highLec + 1
            lectureNumberText = "Lec ${String.format("%02d", nextLec)}"
            title = "${template.chapter} - Lec ${String.format("%02d", nextLec)}"
        } else {
            lectureNumberText = "Lec 01"
            title = "${template.chapter} - Lec 01"
        }
        
        // Open the scheduling form automatically so the user immediately sees it filled
        scrollToPrescheduleForm()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
    ) {
        // --- 1. USER PROFILE SUMMARY SECTION (TOPMOST) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile initials avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(CosmicPrimary, CosmicSecondary))
                            )
                            .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.4f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = viewModel.userName.split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                        Text(
                            text = initials.ifEmpty { "A" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.userName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Exam Goal",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = viewModel.userPreparation,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${viewModel.userPlatform} • ${viewModel.userBatch}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (viewModel.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showProfileEditDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- TODAY'S PROGRESS OVERVIEW TARGETS CARD ---
        item {
            val todayTargets = remember(targets, todayStr) { targets.filter { it.targetDate == todayStr } }
            val totalToday = todayTargets.size
            val completedToday = todayTargets.count { it.status == "completed" }
            val todayPercent = if (totalToday > 0) (completedToday * 100) / totalToday else 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Progress Tracker",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Today's Target Progress",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (totalToday > 0) {
                            Text(
                                text = "$completedToday of $totalToday targets completed today",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "No targets set for today. Plan some below! 🎯",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { if (totalToday > 0) completedToday.toFloat() / totalToday.toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (todayPercent == 100) CosmicAccentCheck else CosmicSecondary,
                            trackColor = CosmicSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (todayPercent == 100) CosmicAccentCheck.copy(alpha = 0.15f)
                                else CosmicSecondary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$todayPercent%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (todayPercent == 100) CosmicAccentCheck else CosmicSecondary
                            )
                            Text(
                                text = "Completed",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 2. HEADER ---
        item {
            Column {
                Text(
                    text = "Abhyas Target Planner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicPrimary,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Aaj ke Targets & Schedulers",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // --- 3. DYNAMIC PRE-FILL SUBJECT PRESETS (THE TEMPLATE ROW) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { scrollToPrescheduleForm() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = "Presets",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "QUICK STUDY PRESETS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicSecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "+ Add Preset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CosmicAccentCheck,
                            modifier = Modifier
                                .clickable { showAddPresetDialog = true }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (viewModel.targetTemplates.isEmpty()) {
                        Text(
                            text = "No presets configured. Click '+ Add Preset' to create custom subjects.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(viewModel.targetTemplates, key = { it.id }) { template ->
                                val subColor = when (template.subject) {
                                    "Physics" -> ColorPhysics
                                    "Chemistry" -> ColorChemistry
                                    "Maths" -> ColorMaths
                                    "Biology" -> ColorBiology
                                    "General" -> ColorGeneral
                                    else -> ColorOther
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CosmicSurfaceVariant)
                                        .border(BorderStroke(1.dp, subColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                                        .clickable { onSelectTemplate(template) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(subColor)
                                    )
                                    Column {
                                        Text(
                                            text = "${template.subject} (${template.type})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = template.chapter,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(2.dp))

                                    // Quick delete button on preset
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete preset",
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.deleteTemplate(template.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. DAILY ASPIRATIONS PANEL ---
        item {
            AspirationsCard(
                aspirations = aspirations,
                onAdd = { viewModel.addAspiration(it) },
                onToggle = { viewModel.toggleAspiration(it) },
                onDelete = { viewModel.deleteAspiration(it) }
            )
        }

        // --- 5. FILTER PILLS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "today" to "Aaj ke Targets",
                    "upcoming" to "Upcoming",
                    "all" to "All Timeline"
                ).forEach { (key, label) ->
                    val selected = activeFilter == key
                    val bgColors = if (selected) {
                        Brush.linearGradient(listOf(CosmicPrimary, CosmicSecondary))
                    } else {
                        Brush.linearGradient(listOf(CosmicSurfaceVariant, CosmicSurfaceVariant))
                    }
                    val borderStroke = if (selected) {
                        BorderStroke(1.dp, CosmicPrimary)
                    } else {
                        BorderStroke(1.dp, CosmicBorder)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(bgColors)
                            .border(borderStroke, RoundedCornerShape(24.dp))
                            .clickable { viewModel.setTargetFilter(key) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 6. PRE-SCHEDULER COLLAPSIBLE WIDGET ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preschedule_collapsible_card"),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.isTargetFormExpanded = !viewModel.isTargetFormExpanded }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                            .testTag("preschedule_header_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pre-Schedule Custom Target",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (viewModel.isTargetFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color.Gray
                        )
                    }

                    AnimatedVisibility(
                        visible = viewModel.isTargetFormExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Task/Lecture Title", color = Color.Gray) },
                                placeholder = { Text("E.g., Complete Lecture 12 Electrostatics", color = Color.DarkGray) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_target_title_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedIndicatorColor = CosmicPrimary,
                                    unfocusedIndicatorColor = CosmicBorder
                                )
                            )

                            // Subject select and Category select drop downs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                            text = "Sub: $selectedSubject",
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
                            }

                            // EXTRA DETAILS FIELDS as requested by the user
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = chapterText,
                                    onValueChange = { chapterText = it },
                                    label = { Text("Chapter", color = Color.Gray, fontSize = 11.sp) },
                                    placeholder = { Text("Electrostatics", color = Color.DarkGray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                OutlinedTextField(
                                    value = lectureNumberText,
                                    onValueChange = { lectureNumberText = it },
                                    label = { Text("Lecture/DPP Suffix", color = Color.Gray, fontSize = 11.sp) },
                                    placeholder = { Text("Lec 04 or DPP 03", color = Color.DarkGray, fontSize = 11.sp) },
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = durationMinutes,
                                    onValueChange = { durationMinutes = it },
                                    label = { Text("Proposed (Min)", color = Color.Gray, fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                OutlinedTextField(
                                    value = batchText,
                                    onValueChange = { batchText = it },
                                    label = { Text("Batch / Source", color = Color.Gray, fontSize = 11.sp) },
                                    placeholder = { Text(viewModel.userBatch, color = Color.DarkGray, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = targetDateText,
                                    onValueChange = {
                                        targetDateText = it
                                        dateError = null
                                    },
                                    label = { Text("Target Date (YYYY-MM-DD)", color = Color.Gray) },
                                    placeholder = { Text(todayStr, color = Color.DarkGray) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    isError = dateError != null,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            if (dateError != null) {
                                Text(
                                    text = dateError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }

                             if (selectedType == "DPP") {
                                OutlinedTextField(
                                    value = questionsCount,
                                    onValueChange = { questionsCount = it },
                                    label = { Text("Questions Count", color = Color.Gray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Auto-add Next Targets",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Generate daily follow-up lectures automatically",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoAddEnabled,
                                    onCheckedChange = { enabled ->
                                        autoAddEnabled = enabled
                                        if (!enabled) {
                                            autoAddMaxLecture = ""
                                            autoAddEndDate = ""
                                            autoAddError = null
                                        }
                                    }
                                )
                            }

                            if (autoAddEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = autoAddMaxLecture,
                                        onValueChange = {
                                            autoAddMaxLecture = it
                                            autoAddError = null
                                        },
                                        label = { Text("Max Lecture No.", color = Color.Gray, fontSize = 11.sp) },
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
                                    OutlinedTextField(
                                        value = autoAddEndDate,
                                        onValueChange = {
                                            autoAddEndDate = it
                                            autoAddError = null
                                        },
                                        label = { Text("End Date (YYYY-MM-DD)", color = Color.Gray, fontSize = 11.sp) },
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

                                Text(
                                    text = "Set a max lecture number or end date to stop auto-add.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (autoAddError != null) {
                                    Text(
                                        text = autoAddError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    dateError = null
                                    autoAddError = null
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                        isLenient = false
                                    }
                                    val normalizedDate = if (targetDateText.isBlank()) {
                                        todayStr
                                    } else {
                                        try {
                                            val parsed = dateFormat.parse(targetDateText.trim()) ?: throw IllegalArgumentException()
                                            dateFormat.format(parsed)
                                        } catch (e: Exception) {
                                            dateError = "Enter a valid date (YYYY-MM-DD)."
                                            return@Button
                                        }
                                    }

                                    var autoConfig: AutoAddConfig? = null
                                    if (autoAddEnabled) {
                                        val maxLecture = autoAddMaxLecture.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                                        val normalizedEndDate = if (autoAddEndDate.isBlank()) {
                                            null
                                        } else {
                                            try {
                                                val parsed = dateFormat.parse(autoAddEndDate.trim()) ?: throw IllegalArgumentException()
                                                dateFormat.format(parsed)
                                            } catch (e: Exception) {
                                                autoAddError = "Enter a valid end date (YYYY-MM-DD)."
                                                return@Button
                                            }
                                        }

                                        if (maxLecture == null && normalizedEndDate == null) {
                                            autoAddError = "Set a max lecture number or an end date."
                                            return@Button
                                        }

                                        val lectureDigits = "\\d+".toRegex().find(lectureNumberText)?.value?.toIntOrNull()
                                        if (lectureDigits == null) {
                                            autoAddError = "Provide a lecture number to auto-add."
                                            return@Button
                                        }

                                        if (maxLecture != null && lectureDigits > maxLecture) {
                                            autoAddError = "Max lecture must be >= current lecture."
                                            return@Button
                                        }

                                        if (normalizedEndDate != null) {
                                            val baseDate = dateFormat.parse(normalizedDate) ?: throw IllegalArgumentException()
                                            val endDate = dateFormat.parse(normalizedEndDate) ?: throw IllegalArgumentException()
                                            if (endDate.before(baseDate)) {
                                                autoAddError = "End date must be on/after target date."
                                                return@Button
                                            }
                                        }

                                        autoConfig = AutoAddConfig(
                                            maxLectureNumber = maxLecture,
                                            endDate = normalizedEndDate
                                        )
                                    }

                                    val duration = durationMinutes.toIntOrNull() ?: 45
                                    val qCount = if (selectedType == "DPP") questionsCount.toIntOrNull() else null
                                    
                                    val finalBatch = batchText.ifEmpty { viewModel.userBatch }
                                    val finalTitle = title.ifEmpty {
                                        if (chapterText.isNotEmpty()) {
                                            "$chapterText ${lectureNumberText.ifEmpty { selectedType }}"
                                        } else {
                                            "$selectedSubject Study Session"
                                        }
                                    }

                                    if (finalTitle.trim().isNotEmpty()) {
                                        viewModel.addDailyTarget(
                                            title = finalTitle,
                                            subject = selectedSubject,
                                            type = selectedType,
                                            targetDate = normalizedDate,
                                            durationProposed = duration,
                                            questionsCount = qCount,
                                            chapter = chapterText.ifEmpty { null },
                                            lectureNumber = lectureNumberText.ifEmpty { null },
                                            batch = finalBatch,
                                            autoAddConfig = autoConfig
                                        )

                                        // Reset fields
                                        title = ""
                                        questionsCount = ""
                                        chapterText = ""
                                        lectureNumberText = ""
                                        batchText = ""
                                        autoAddEnabled = false
                                        autoAddMaxLecture = ""
                                        autoAddEndDate = ""
                                        autoAddError = null
                                        viewModel.isTargetFormExpanded = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_target_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                            ) {
                                Text("Schedule Target", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- EMPTY STATE IF NO TARGETS ---
        if (filteredTargets.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = "No Targets",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Koi targets scheduled nahi hain.",
                        color = Color(0xFF64748B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Preset template select karein ya naya target pre-schedule karein!",
                        color = Color(0xFF475569),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // --- RECORDED TARGETS LIST ---
            items(filteredTargets, key = { it.id }) { target ->
                TargetCard(
                    target = target,
                    onToggleComplete = { viewModel.toggleTargetCompletion(target) },
                    onDelete = { viewModel.deleteTarget(target.id) },
                    onPlay = {
                        viewModel.selectBoundTarget(target.id)
                        viewModel.selectTimerMode("Stopwatch")
                        viewModel.switchTab("timer")
                    },
                    onLogMinutes = { mins ->
                        viewModel.logTargetTimeManually(target.id, mins)
                    }
                )
            }
        }
    }

    // Dialog - Edit Profile Section
    if (showProfileEditDialog) {
        var editNameText by remember { mutableStateOf(viewModel.userName) }
        var editPlatformText by remember { mutableStateOf(viewModel.userPlatform) }
        var editBatchText by remember { mutableStateOf(viewModel.userBatch) }
        var editPrepText by remember { mutableStateOf(viewModel.userPreparation) }

        AlertDialog(
            onDismissRequest = { showProfileEditDialog = false },
            title = { Text("Update Your Profile details", color = Color.White) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Customize your platform, batch and goal details.", color = Color.LightGray, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text("Aspirant Name", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = editPrepText,
                        onValueChange = { editPrepText = it },
                        label = { Text("Exam Preparing For", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = editPlatformText,
                        onValueChange = { editPlatformText = it },
                        label = { Text("Online Platform (e.g. PW)", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = editBatchText,
                        onValueChange = { editBatchText = it },
                        label = { Text("Batch Name (e.g. Lakshya)", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = editNameText,
                            platform = editPlatformText,
                            batch = editBatchText,
                            preparation = editPrepText
                        )
                        showProfileEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentCheck)
                ) {
                    Text("Save Changes", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CosmicSurface,
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }

    // Dialog - Add Preset Template
    if (showAddPresetDialog) {
        var presetSubject by remember { mutableStateOf("Physics") }
        var presetType by remember { mutableStateOf("Lecture") }
        var presetBatch by remember { mutableStateOf(viewModel.userBatch) }
        var presetChapter by remember { mutableStateOf("") }
        var presetDuration by remember { mutableStateOf("90") }

        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            title = { Text("Add Quick Study Preset", color = Color.White) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Define templates for subjects you study regularly to avoid repeating details later.", color = Color.LightGray, fontSize = 13.sp)

                    // Subject option dropdown
                    var subDialogExp by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { subDialogExp = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, CosmicBorder)
                        ) {
                            Text("Subject: $presetSubject")
                        }
                        DropdownMenu(
                            expanded = subDialogExp,
                            onDismissRequest = { subDialogExp = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            subjectsList.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, color = Color.White) },
                                    onClick = {
                                        presetSubject = s
                                        subDialogExp = false
                                    }
                                )
                            }
                        }
                    }

                    // Type option dropdown
                    var typeDialogExp by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { typeDialogExp = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, CosmicBorder)
                        ) {
                            Text("Type: $presetType")
                        }
                        DropdownMenu(
                            expanded = typeDialogExp,
                            onDismissRequest = { typeDialogExp = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            typesList.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t, color = Color.White) },
                                    onClick = {
                                        presetType = t
                                        typeDialogExp = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = presetChapter,
                        onValueChange = { presetChapter = it },
                        label = { Text("Default Chapter (E.g. integration)", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = presetBatch,
                        onValueChange = { presetBatch = it },
                        label = { Text("Batch / Teacher (E.g. Lakshya)", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = presetDuration,
                        onValueChange = { presetDuration = it },
                        label = { Text("Default Session Time (Mins)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val duration = presetDuration.toIntOrNull() ?: 90
                        val finalChapter = presetChapter.ifEmpty { "General study" }
                        viewModel.addTemplate(
                            subject = presetSubject,
                            batch = presetBatch.ifEmpty { "Self" },
                            type = presetType,
                            chapter = finalChapter,
                            duration = duration
                        )
                        showAddPresetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondary)
                ) {
                    Text("Create Preset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPresetDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CosmicSurface,
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }
}

// --- SUBCOMPONENT: ASPIRATIONS CARD ---
@Composable
fun AspirationsCard(
    aspirations: List<DailyAspiration>,
    onAdd: (String) -> Unit,
    onToggle: (DailyAspiration) -> Unit,
    onDelete: (String) -> Unit
) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var newAspText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AAJ SE AKANSHA (Aspirations)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Your Focus Declarations",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Decoration",
                    tint = CosmicAccentAlert,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newAspText,
                    onValueChange = { newAspText = it },
                    placeholder = { Text("E.g. Study Electrostatics DPP today...", fontSize = 12.sp, color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CosmicSurfaceVariant,
                        unfocusedContainerColor = CosmicSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = CosmicPrimary,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Button(
                    onClick = {
                        if (newAspText.trim().isNotEmpty()) {
                            onAdd(newAspText)
                            newAspText = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                aspirations.forEach { aspiration ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggle(aspiration) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (aspiration.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = "Toggle status",
                                tint = if (aspiration.isCompleted) CosmicAccentCheck else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = aspiration.text,
                            fontSize = 13.sp,
                            color = if (aspiration.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (aspiration.isCompleted) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(
                            onClick = { onDelete(aspiration.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete aspiration",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SUBCOMPONENT: TARGET PROGRESS CARD ---
@Composable
fun TargetCard(
    target: DailyTarget,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onLogMinutes: (Int) -> Unit
) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val subjectColor = when (target.subject) {
        "Physics" -> ColorPhysics
        "Chemistry" -> ColorChemistry
        "Maths" -> ColorMaths
        "Biology" -> ColorBiology
        "General" -> ColorGeneral
        else -> ColorOther
    }

    var showManualMinsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showUncheckConfirmDialog by remember { mutableStateOf(false) }
    var inputMinsText by remember { mutableStateOf("") }

    val totalLoggedMin = target.durationLogged / 60
    val totalLoggedSecondsLeft = target.durationLogged % 60
    val completedPercentage = if (target.durationProposed > 0) {
        ((totalLoggedMin.toFloat() / target.durationProposed) * 100).toInt().coerceIn(0, 100)
    } else 0

    val isFullComplete = target.status == "completed"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_item_card_${target.id}"),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, if (isFullComplete) CosmicAccentCheck else CosmicBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main Top Title Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Header Tags
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(subjectColor)
                        )
                        Text(
                            text = "${target.subject} • ${target.type}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = subjectColor,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        // Batch Tag Display
                        if (!target.batch.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CosmicSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = target.batch,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Main Title
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFullComplete) Color.Gray else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isFullComplete) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Detailed Sub-items: Chapter & Lecture tags
                    if (!target.chapter.isNullOrEmpty() || !target.lectureNumber.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!target.chapter.isNullOrEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CosmicPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = "Chapter icon",
                                        tint = CosmicSecondary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = target.chapter,
                                        fontSize = 10.sp,
                                        color = CosmicSecondary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!target.lectureNumber.isNullOrEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CosmicAccentAlert.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleOutline,
                                        contentDescription = "Lecture Tag",
                                        tint = CosmicAccentAlert,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = target.lectureNumber,
                                        fontSize = 10.sp,
                                        color = CosmicAccentAlert,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (isFullComplete) {
                            showUncheckConfirmDialog = true
                        } else {
                            onToggleComplete()
                        }
                    },
                    modifier = Modifier.testTag("check_target_status_btn_${target.id}")
                ) {
                    Icon(
                        imageVector = if (isFullComplete) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Status check",
                        tint = if (isFullComplete) CosmicAccentCheck else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar and numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Logged: ${totalLoggedMin}m ${totalLoggedSecondsLeft}s / ${target.durationProposed}m",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "$completedPercentage%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFullComplete) CosmicAccentCheck else CosmicSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { completedPercentage.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isFullComplete) CosmicAccentCheck else CosmicPrimary,
                trackColor = CosmicSurfaceVariant
            )

            // DPP questions indicators if available
            if (target.dppQuestionsCount != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CosmicSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DPP Sets: ${target.dppQuestionsCount} Qs",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Complete your DPP scores in the DPP tab to sync accuracy analytics",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions bottom toolbar on card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Manual study override
                    IconButton(
                        onClick = { showManualMinsDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CosmicSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Manual study override",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Delete target
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CosmicSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete target",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Play Focus Timer shortcut
                if (!isFullComplete) {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondary),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play focus", fontSize = 11.sp, color = Color.White)
                    }
                } else {
                    Text(
                        text = "RESOLVED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CosmicAccentCheck,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    // Manual logging dialogue pop-up
    if (showManualMinsDialog) {
        AlertDialog(
            onDismissRequest = { showManualMinsDialog = false },
            title = { Text("Log Study Time Manually", color = Color.White) },
            text = {
                Column {
                    Text("Overriding study seconds directly is convenient. In many minutes did you study?", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputMinsText,
                        onValueChange = { inputMinsText = it },
                        label = { Text("Minutes Done") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurface,
                            unfocusedContainerColor = CosmicSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = inputMinsText.toIntOrNull() ?: 0
                        if (mins > 0) {
                            onLogMinutes(mins)
                            showManualMinsDialog = false
                            inputMinsText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentCheck)
                ) {
                    Text("Override Done", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualMinsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CosmicSurface,
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete target?", color = Color.White) },
            text = { Text("This action will permanently delete the target.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CosmicSurface,
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }

    if (showUncheckConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUncheckConfirmDialog = false },
            title = { Text("Uncheck completed target?", color = Color.White) },
            text = { Text("This will move the target back to pending status.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleComplete()
                        showUncheckConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentAlert)
                ) {
                    Text("Uncheck", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUncheckConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CosmicSurface,
            textContentColor = Color.LightGray,
            titleContentColor = Color.White
        )
    }
}
