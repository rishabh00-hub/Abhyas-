package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DPPHistoryLog
import com.example.ui.DppPreset
import com.example.ui.StudyViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DPPScreen(viewModel: StudyViewModel) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val dppLogs by viewModel.allDPPLogs.collectAsState()

    var showAddForm by remember { mutableStateOf(false) }
    var showAddPresetDialog by remember { mutableStateOf(false) }

    // Intake Form States
    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("Physics") }
    var totalQuestions by remember { mutableStateOf("10") }
    var attempted by remember { mutableStateOf("8") }
    var correct by remember { mutableStateOf("6") }
    var durationMinutes by remember { mutableStateOf("20") }
    var notes by remember { mutableStateOf("") }

    val subjectsList = listOf("Physics", "Chemistry", "Maths", "Biology", "General")

    val onSelectPreset = { preset: DppPreset ->
        title = preset.title
        selectedSubject = preset.subject
        totalQuestions = preset.totalQuestions.toString()
        attempted = preset.attempted.toString()
        correct = preset.correct.toString()
        durationMinutes = preset.durationMinutes.toString()
        showAddForm = true
    }

    // Sort chronologically (earliest to latest for drawing graph lines)
    val chronologicalLogs = remember(dppLogs) {
        dppLogs.reversed() // Reverse so earliest is first
    }

    // Filtered list based on chart selector
    val filteredLogsForChart = remember(chronologicalLogs, viewModel.dppSubjectFilter) {
        if (viewModel.dppSubjectFilter == "All Subjects") {
            chronologicalLogs
        } else {
            chronologicalLogs.filter { it.subject == viewModel.dppSubjectFilter }
        }
    }

    // Filtered lists for historical lists display
    val filteredLogsForList = remember(dppLogs, viewModel.dppSubjectFilter) {
        if (viewModel.dppSubjectFilter == "All Subjects") {
            dppLogs
        } else {
            dppLogs.filter { it.subject == viewModel.dppSubjectFilter }
        }
    }

    // --- ACCUMULATIVE TOP METRICS CALCULATIONS ---
    val cumulativeMetrics = remember(dppLogs) {
        var totalSets = dppLogs.size
        var totalAttempted = 0
        var totalCorrect = 0
        var totalQs = 0

        dppLogs.forEach { log ->
            totalAttempted += log.attempted
            totalCorrect += log.correct
            totalQs += log.totalQuestions
        }

        val accuracyPercentage = if (totalAttempted > 0) {
            ((totalCorrect.toFloat() / totalAttempted) * 100).toInt()
        } else 0

        Triple(totalSets, totalQs, accuracyPercentage)
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
                    text = "Abhyas Core Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicPrimary,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "DPP Practice Score Tracking",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // --- DPP PRESETS ---
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Analytics,
                                contentDescription = "DPP Presets",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DPP PRESETS",
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

                    if (viewModel.dppPresets.isEmpty()) {
                        Text(
                            text = "No DPP presets configured. Click '+ Add Preset' to create one.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(viewModel.dppPresets, key = { it.id }) { preset ->
                                val subColor = when (preset.subject) {
                                    "Physics" -> ColorPhysics
                                    "Chemistry" -> ColorChemistry
                                    "Maths" -> ColorMaths
                                    "Biology" -> ColorBiology
                                    else -> ColorGeneral
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CosmicSurfaceVariant)
                                        .border(BorderStroke(1.dp, subColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                                        .clickable { onSelectPreset(preset) }
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
                                            text = preset.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${preset.totalQuestions} Qs • ${preset.durationMinutes}m",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(2.dp))

                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete preset",
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.deleteDppPreset(preset.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- INTAKE CARD MODULE ---
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
                            .clickable { showAddForm = !showAddForm }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Add DPP",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Intake Module: Record DPP Marks",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand form",
                            tint = Color.Gray
                        )
                    }

                    AnimatedVisibility(
                        visible = showAddForm,
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
                                label = { Text("DPP Set Title/Sheet No.", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("add_dpp_title_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedIndicatorColor = CosmicPrimary,
                                    unfocusedIndicatorColor = CosmicBorder
                                )
                            )

                            // Subject choice
                            var subExp by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { subExp = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                                    border = BorderStroke(1.dp, CosmicBorder)
                                ) {
                                    Text("Select DPP Subject: $selectedSubject", fontSize = 13.sp)
                                }
                                DropdownMenu(
                                    expanded = subExp,
                                    onDismissRequest = { subExp = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(CosmicSurfaceVariant)
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = totalQuestions,
                                    onValueChange = { totalQuestions = it },
                                    label = { Text("Total Qs", color = Color.Gray) },
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
                                    value = attempted,
                                    onValueChange = { attempted = it },
                                    label = { Text("Attempted", color = Color.Gray) },
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
                                    value = correct,
                                    onValueChange = { correct = it },
                                    label = { Text("Correct", color = Color.Gray) },
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
                                value = durationMinutes,
                                onValueChange = { durationMinutes = it },
                                label = { Text("Time spent (Minutes)", color = Color.Gray) },
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

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Optional observations/difficulty details", color = Color.Gray) },
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
                                    val totalVal = totalQuestions.toIntOrNull() ?: 10
                                    val attVal = attempted.toIntOrNull() ?: 0
                                    val corrVal = correct.toIntOrNull() ?: 0
                                    val mins = durationMinutes.toIntOrNull() ?: 20

                                    if (title.trim().isNotEmpty() && attVal >= corrVal && totalVal >= attVal) {
                                        viewModel.addDPPLog(
                                            title = title,
                                            subject = selectedSubject,
                                            totalQuestions = totalVal,
                                            attempted = attVal,
                                            correct = corrVal,
                                            minutes = mins,
                                            notes = notes.ifEmpty { null }
                                        )
                                        // Reset fields
                                        title = ""
                                        notes = ""
                                        showAddForm = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_dpp_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                            ) {
                                Text("Log DPP sheet done", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // --- TOP LEVEL ACCUMULATED SUMMARY METRIC CARDS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Solved Sets
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.CloudQueue, contentDescription = "", tint = CosmicSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sets Solved", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${cumulativeMetrics.first} Sets",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Total Questions Bank
                Card(
                    modifier = Modifier.weight(1.1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.AllInbox, contentDescription = "", tint = CosmicAccentAlert, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Qs Tackled", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${cumulativeMetrics.second} Qs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Cumulative Accuracy Percentage
                Card(
                    modifier = Modifier.weight(1.1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.PieChart, contentDescription = "", tint = CosmicAccentCheck, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Avg Accuracy", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${cumulativeMetrics.third}%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CosmicAccentCheck,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- CUSTOM DOUBLE LINE CHART (ACCURACY vs SCORE) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // UPPER SELECTOR HEADER IN CHART CARD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CHRONOLOGICAL ACCURACY GRAPH",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Analytics Over Time",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Subject Dropdown Filter box inside graph
                        var filterExp by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { filterExp = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                border = BorderStroke(1.dp, CosmicBorder)
                            ) {
                                Text(viewModel.dppSubjectFilter, fontSize = 11.sp, color = Color.LightGray)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = filterExp,
                                onDismissRequest = { filterExp = false },
                                modifier = Modifier.background(CosmicSurfaceVariant)
                            ) {
                                (listOf("All Subjects") + subjectsList).forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub, color = Color.White) },
                                        onClick = {
                                            viewModel.setDppFilter(sub)
                                            filterExp = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredLogsForChart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Analytics ko chart karne ke liye DPP solve karein.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // CUSTOM DRAWN DOUBLE-LINE CHART
                        var selectedCoordinateIndex by remember { mutableStateOf<Int?>(null) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(filteredLogsForChart) {
                                        detectTapGestures { offset ->
                                            // Handle tapping index mapping
                                            val spaceWidth = size.width
                                            val segmentWidth = spaceWidth / (filteredLogsForChart.size + 1)
                                            var nearestIndex = -1
                                            var minDistance = Float.MAX_VALUE

                                            for (i in filteredLogsForChart.indices) {
                                                val x = segmentWidth * (i + 1)
                                                val distance = kotlin.math.abs(offset.x - x)
                                                if (distance < minDistance) {
                                                    minDistance = distance
                                                    nearestIndex = i
                                                }
                                            }

                                            if (nearestIndex != -1 && minDistance < segmentWidth * 0.6f) {
                                                selectedCoordinateIndex = if (selectedCoordinateIndex == nearestIndex) null else nearestIndex
                                            } else {
                                                selectedCoordinateIndex = null
                                            }
                                        }
                                    }
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Draw vertical grid coordinate lines & horizontal scales (25%, 50%, 75%, 100%)
                                val stepHeight = canvasHeight / 4
                                val fontMinSpaceColor = CosmicBorder

                                for (gridLine in 1..3) {
                                    val y = stepHeight * gridLine
                                    drawLine(
                                        color = fontMinSpaceColor,
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 1f
                                    )
                                }

                                val sizeLog = filteredLogsForChart.size
                                val spacingSegmentX = canvasWidth / (sizeLog + 1)

                                val dppPointsAccuracy = mutableListOf<Offset>()
                                val dppPointsScored = mutableListOf<Offset>()

                                filteredLogsForChart.forEachIndexed { idx, log ->
                                    val actX = spacingSegmentX * (idx + 1)

                                    // Metric 1: Accuracy = Correct / Attempted * 100
                                    val accVal = if (log.attempted > 0) {
                                        (log.correct.toFloat() / log.attempted) * 100f
                                    } else 0f

                                    // Metric 2: Score = Correct / Total * 100
                                    val scoreVal = if (log.totalQuestions > 0) {
                                        (log.correct.toFloat() / log.totalQuestions) * 100f
                                    } else 0f

                                    // Normalize Y (0% is bottom canvasHeight, 100% is top 0f, padded slightly)
                                    val actYAcc = canvasHeight - ((accVal / 100f) * (canvasHeight - 20f) + 10f)
                                    val actYScore = canvasHeight - ((scoreVal / 100f) * (canvasHeight - 20f) + 10f)

                                    dppPointsAccuracy.add(Offset(actX, actYAcc))
                                    dppPointsScored.add(Offset(actX, actYScore))
                                }

                                // 1. Draw Percentage Scored Line (Indigo)
                                for (pIdx in 0 until dppPointsScored.size - 1) {
                                    drawLine(
                                        color = CosmicSecondary,
                                        start = dppPointsScored[pIdx],
                                        end = dppPointsScored[pIdx + 1],
                                        strokeWidth = 3f
                                    )
                                }

                                // 2. Draw Accuracy Line (Emerald)
                                for (pIdx in 0 until dppPointsAccuracy.size - 1) {
                                    drawLine(
                                        color = CosmicAccentCheck,
                                        start = dppPointsAccuracy[pIdx],
                                        end = dppPointsAccuracy[pIdx + 1],
                                        strokeWidth = 3f
                                    )
                                }

                                // Draw little coordinates circles dots
                                dppPointsScored.forEachIndexed { dIdx, point ->
                                    drawCircle(
                                        color = if (selectedCoordinateIndex == dIdx) Color.White else CosmicSecondary,
                                        radius = if (selectedCoordinateIndex == dIdx) 6f else 4f,
                                        center = point
                                    )
                                    if (selectedCoordinateIndex == dIdx) {
                                        drawCircle(
                                            color = CosmicSecondary,
                                            radius = 12f,
                                            center = point,
                                            style = Stroke(2f)
                                        )
                                    }
                                }

                                dppPointsAccuracy.forEachIndexed { dIdx, point ->
                                    drawCircle(
                                        color = if (selectedCoordinateIndex == dIdx) Color.White else CosmicAccentCheck,
                                        radius = if (selectedCoordinateIndex == dIdx) 6f else 4f,
                                        center = point
                                    )
                                    if (selectedCoordinateIndex == dIdx) {
                                        drawCircle(
                                            color = CosmicAccentCheck,
                                            radius = 12f,
                                            center = point,
                                            style = Stroke(2f)
                                        )
                                    }
                                }
                            }
                        }

                        // OVERLAY DETAILS TOOLTIP ELEMENT RENDER
                        AnimatedVisibility(
                            visible = selectedCoordinateIndex != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            val activeIdx = selectedCoordinateIndex ?: 0
                            val targetLog = filteredLogsForChart.getOrNull(activeIdx)

                            if (targetLog != null) {
                                val acc = if (targetLog.attempted > 0) {
                                    (targetLog.correct.toFloat() / targetLog.attempted) * 100
                                } else 0.0
                                val score = if (targetLog.totalQuestions > 0) {
                                    (targetLog.correct.toFloat() / targetLog.totalQuestions) * 100
                                } else 0.0

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant),
                                    border = BorderStroke(1.dp, CosmicPrimary)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = targetLog.title,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = targetLog.date,
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(
                                                text = "Accuracy: ${String.format(Locale.getDefault(), "%.1f", acc)}%",
                                                color = CosmicAccentCheck,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Percentage Scored: ${String.format(Locale.getDefault(), "%.1f", score)}%",
                                                color = CosmicSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${targetLog.correct}/${targetLog.attempted}/${targetLog.totalQuestions} Qs",
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // BOTTOM LEGENDS MATHEMATICAL DEFINITIONS CARD
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CosmicSurfaceVariant,
                        border = BorderStroke(1.dp, CosmicBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.Analytics, contentDescription = null, tint = CosmicSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mathematical Metrics Definitions", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CosmicAccentCheck))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Accuracy %", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                        Text("(Correct / Attempted) * 100", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Row(
                                    modifier = Modifier.weight(1.1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CosmicSecondary))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Scored Score %", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                        Text("(Correct / Total Questions) * 100", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- EMPTY STATE IF NO LIST ITEMS IN DPP ---
        if (filteredLogsForList.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Is subject me koi DPP records nahi hain.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // --- DPP HISTORY LIST ---
            items(filteredLogsForList, key = { it.id }) { log ->
                DPPItemRow(
                    log = log,
                    onDelete = { viewModel.deleteDPPLog(log.id) }
                )
            }
        }
    }

    if (showAddPresetDialog) {
        var presetTitle by remember { mutableStateOf("") }
        var presetSubject by remember { mutableStateOf(selectedSubject) }
        var presetTotal by remember { mutableStateOf(totalQuestions) }
        var presetAttempted by remember { mutableStateOf(attempted) }
        var presetCorrect by remember { mutableStateOf(correct) }
        var presetDuration by remember { mutableStateOf(durationMinutes) }
        var subjectExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            title = { Text("Create DPP Preset", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = presetTitle,
                        onValueChange = { presetTitle = it },
                        label = { Text("Preset Title", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Box {
                        OutlinedButton(
                            onClick = { subjectExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, CosmicBorder)
                        ) {
                            Text("Subject: $presetSubject", fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = subjectExpanded,
                            onDismissRequest = { subjectExpanded = false },
                            modifier = Modifier.background(CosmicSurfaceVariant)
                        ) {
                            subjectsList.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub, color = Color.White) },
                                    onClick = {
                                        presetSubject = sub
                                        subjectExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = presetTotal,
                            onValueChange = { presetTotal = it },
                            label = { Text("Total Qs", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CosmicSurfaceVariant,
                                unfocusedContainerColor = CosmicSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        OutlinedTextField(
                            value = presetAttempted,
                            onValueChange = { presetAttempted = it },
                            label = { Text("Attempted", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CosmicSurfaceVariant,
                                unfocusedContainerColor = CosmicSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = presetCorrect,
                            onValueChange = { presetCorrect = it },
                            label = { Text("Correct", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
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
                            label = { Text("Duration (Min)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CosmicSurfaceVariant,
                                unfocusedContainerColor = CosmicSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val totalVal = presetTotal.toIntOrNull() ?: 10
                        val attemptVal = presetAttempted.toIntOrNull() ?: 0
                        val correctVal = presetCorrect.toIntOrNull() ?: 0
                        val durationVal = presetDuration.toIntOrNull() ?: 20

                        if (presetTitle.trim().isNotEmpty()) {
                            viewModel.addDppPreset(
                                title = presetTitle,
                                subject = presetSubject,
                                totalQuestions = totalVal,
                                attempted = attemptVal.coerceAtMost(totalVal),
                                correct = correctVal.coerceAtMost(attemptVal.coerceAtMost(totalVal)),
                                durationMinutes = durationVal
                            )
                            showAddPresetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentCheck)
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

// --- SUBCOMPONENT: DPP ITEM CARD IN LIST ---
@Composable
fun DPPItemRow(
    log: DPPHistoryLog,
    onDelete: () -> Unit
) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val subjectBadgeColor = when (log.subject) {
        "Physics" -> ColorPhysics
        "Chemistry" -> ColorChemistry
        "Maths" -> ColorMaths
        "Biology" -> ColorBiology
        else -> ColorOther
    }

    val acc = if (log.attempted > 0) {
        ((log.correct.toFloat() / log.attempted) * 100).toInt()
    } else 0

    val score = if (log.totalQuestions > 0) {
        ((log.correct.toFloat() / log.totalQuestions) * 100).toInt()
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(subjectBadgeColor))
                    Text(
                        text = log.subject,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "· ${log.date}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Correct: ${log.correct} | Attempted: ${log.attempted} | Total: ${log.totalQuestions} Qs",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Accuracy vs Score Display Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Acc: $acc%", color = CosmicAccentCheck, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "Score: $score%", color = CosmicSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_dpp_log_btn_${log.id}")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
