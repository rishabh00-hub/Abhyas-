package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyTarget
import com.example.ui.StudyViewModel
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(viewModel: StudyViewModel) {
    val CosmicSurface = MaterialTheme.colorScheme.surface
    val CosmicBorder = MaterialTheme.colorScheme.outline
    val CosmicSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val isCompact = LocalConfiguration.current.screenWidthDp < 360

    val targets by viewModel.allTargets.collectAsState()
    val todayStr = viewModel.todayDate

    // Pending today's targets to bind
    val pendingTodayTargets = remember(targets, todayStr) {
        targets.filter { it.targetDate == todayStr && it.status != "completed" }
    }

    val boundTarget = remember(targets, viewModel.boundTargetId) {
        targets.find { it.id == viewModel.boundTargetId }
    }

    // Concentric pulsating circle animation
    val infiniteTransition = rememberInfiniteTransition(label = "PulseGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlowScale"
    )

    fun formatSecondsLabel(seconds: Int): String {
        return if (seconds < 60) {
            "${seconds}s"
        } else {
            val minsPart = seconds / 60
            val secsPart = seconds % 60
            if (secsPart == 0) "${minsPart}m" else "${minsPart}m ${secsPart}s"
        }
    }

    // Calculate time remaining format (MM:SS) if Pomodoro, or elapsed if Stopwatch
    val totalSeconds = viewModel.secondsElapsedOrRemaining
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    val formattedTimeStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- TICKER / MODE SELECTOR HOVER CAPSULE ---
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CosmicSurface)
                .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Pomodoro", "Stopwatch").forEach { m ->
                val selected = viewModel.timerMode == m
                val bgBrush = if (selected) {
                    Brush.linearGradient(listOf(CosmicPrimary, CosmicSecondary))
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(bgBrush)
                        .clickable { viewModel.selectTimerMode(m) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = m,
                        color = if (selected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- PENDING TARGET BINDING SELECTION ---
        var showTargetDropdown by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicSurface)
                .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(12.dp))
                .clickable { if (viewModel.timerState == "Idle") showTargetDropdown = true }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (boundTarget == null) Icons.Default.LinkOff else Icons.Default.Link,
                        contentDescription = "Binding",
                        tint = if (boundTarget == null) Color.Gray else CosmicAccentCheck,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (viewModel.timerState != "Idle") "Active Study Target (LOCKED)" else "Bind Active Study Target",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicSecondary
                        )
                        Text(
                            text = boundTarget?.title ?: "General Focus Zone (Unlinked)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
                if (viewModel.timerState == "Idle") {
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                }
            }

            DropdownMenu(
                expanded = showTargetDropdown,
                onDismissRequest = { showTargetDropdown = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(CosmicSurfaceVariant)
                    .border(BorderStroke(1.dp, CosmicBorder))
            ) {
                DropdownMenuItem(
                    text = { Text("General Focus Zone (Unlinked)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) },
                    onClick = {
                        viewModel.selectBoundTarget(null)
                        showTargetDropdown = false
                    }
                )
                if (pendingTodayTargets.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No pending today's targets available.", color = Color.Gray, fontSize = 12.sp) },
                        onClick = { showTargetDropdown = false },
                        enabled = false
                    )
                } else {
                    pendingTodayTargets.forEach { t ->
                        val subIndicatorColor = when (t.subject) {
                            "Physics" -> ColorPhysics
                            "Chemistry" -> ColorChemistry
                            "Maths" -> ColorMaths
                            "Biology" -> ColorBiology
                            else -> ColorGeneral
                        }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(subIndicatorColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${t.subject} · ${t.title}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectBoundTarget(t.id)
                                showTargetDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // --- IMMERSIVE GLOWING RADIAL RING ---
        val orbitColor = when (viewModel.timerState) {
            "Running" -> CosmicPrimary
            "Paused" -> CosmicAccentAlert
            "Finished" -> CosmicAccentCheck
            else -> CosmicBorder
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val ringSize = maxWidth * if (isCompact) 0.9f else 0.75f
            val timeFontSize = (ringSize.value * 0.18f).sp
            val stateFontSize = (ringSize.value * 0.045f).sp
            val modeFontSize = (ringSize.value * 0.04f).sp

            Box(
                modifier = Modifier
                    .size(ringSize)
                    .scale(if (viewModel.timerState == "Running") pulseScale else 1f),
                contentAlignment = Alignment.Center
            ) {
                // Neon shadow pulse background circles
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.92f)
                        .clip(CircleShape)
                        .background(orbitColor.copy(alpha = 0.05f))
                        .border(BorderStroke(4.dp, orbitColor.copy(alpha = 0.4f)), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize(0.82f)
                        .clip(CircleShape)
                        .background(CosmicSurface)
                        .border(BorderStroke(2.dp, orbitColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = viewModel.timerState.uppercase(Locale.getDefault()),
                            fontSize = stateFontSize,
                            fontWeight = FontWeight.Bold,
                            color = orbitColor,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedTimeStr,
                            fontSize = timeFontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (viewModel.timerMode) {
                                    "Pomodoro" -> Icons.Default.HourglassBottom
                                    else -> Icons.Default.Timer
                                },
                                contentDescription = "Mode indicator",
                                tint = Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = viewModel.timerMode,
                                fontSize = modeFontSize,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- POMODORO PRESETS AND CUSTOM TIMER CONTROLS ---
        if (viewModel.timerMode == "Pomodoro" && viewModel.timerState == "Idle") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "POMODORO DURATION PRESETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary,
                        letterSpacing = 1.sp
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = if (isCompact) 2 else 4
                    ) {
                        listOf(15, 25, 45, 60).forEach { minsPreset ->
                            val isSelected = viewModel.durationProposedMinutes == minsPreset
                            OutlinedButton(
                                onClick = { viewModel.setCustomPomodoroMinutes(minsPreset) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) CosmicPrimary else CosmicBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) CosmicPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("${minsPreset}m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        maxItemsInEachRow = if (isCompact) 1 else 2
                    ) {
                        Text(
                            text = "ADJUST CUSTOM DURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${viewModel.durationProposedMinutes} min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = viewModel.durationProposedMinutes.toFloat(),
                            onValueChange = { viewModel.setCustomPomodoroMinutes(it.toInt().coerceIn(1, 180)) },
                            valueRange = 1f..180f,
                            steps = 179,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = CosmicPrimary,
                                activeTrackColor = CosmicSecondary,
                                inactiveTrackColor = CosmicBorder
                            )
                        )

                        val inputModifier = if (isCompact) Modifier.weight(0.35f) else Modifier.weight(0.25f)
                        var showTypedInput by remember { mutableStateOf(false) }
                        if (showTypedInput) {
                            OutlinedTextField(
                                value = if (viewModel.durationProposedMinutes == 0) "" else viewModel.durationProposedMinutes.toString(),
                                onValueChange = { newVal ->
                                    val parsed = newVal.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    viewModel.setCustomPomodoroMinutes(parsed.coerceIn(1, 180))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CosmicSurfaceVariant,
                                    unfocusedContainerColor = CosmicSurfaceVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedIndicatorColor = CosmicPrimary,
                                    unfocusedIndicatorColor = CosmicBorder
                                ),
                                modifier = inputModifier.testTag("pomodoro_custom_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            IconButton(
                                onClick = { showTypedInput = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CosmicSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit manually",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- STOPWATCH ALERTS PRESETS AND CUSTOM TIMER CONTROLS ---
        if (viewModel.timerMode == "Stopwatch" && viewModel.timerState == "Idle") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stopwatch_alert_card"),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "STOPWATCH ALERT MODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicSecondary,
                        letterSpacing = 1.sp
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = if (isCompact) 1 else 3
                    ) {
                        listOf(
                            Triple("None", "No Alert", Icons.Default.NotificationsOff),
                            Triple("Single", "Single Target", Icons.Default.Alarm),
                            Triple("Interval", "Periodic Ring", Icons.Default.NotificationsActive)
                        ).forEach { (modeVal, label, icon) ->
                            val isSelected = viewModel.stopwatchAlertType == modeVal
                            OutlinedButton(
                                onClick = { viewModel.stopwatchAlertType = modeVal },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) CosmicPrimary else CosmicBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) CosmicPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .let { if (isCompact) it.fillMaxWidth() else it.weight(1f) }
                                    .testTag("stopwatch_alert_mode_${modeVal.lowercase()}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) CosmicPrimary else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }

                    if (viewModel.stopwatchAlertType != "None") {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (viewModel.stopwatchAlertType == "Interval") {
                                "ALERT INTERVAL PRESETS"
                            } else {
                                "SINGLE ALERT TARGET PRESETS"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicSecondary,
                            letterSpacing = 1.sp
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = if (isCompact) 3 else 4
                        ) {
                            listOf(30, 60, 120, 180, 300, 600, 900).forEach { secondsPreset ->
                                val isSelected = viewModel.stopwatchAlertIntervalSeconds == secondsPreset
                                OutlinedButton(
                                    onClick = { viewModel.stopwatchAlertIntervalSeconds = secondsPreset },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) CosmicPrimary else CosmicBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) CosmicPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stopwatch_preset_${secondsPreset}s")
                                ) {
                                    Text(formatSecondsLabel(secondsPreset), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            maxItemsInEachRow = if (isCompact) 1 else 2
                        ) {
                            Text(
                                text = "ADJUST ALERT TIME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CosmicSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = formatSecondsLabel(viewModel.stopwatchAlertIntervalSeconds),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = viewModel.stopwatchAlertIntervalSeconds.toFloat(),
                                onValueChange = { viewModel.stopwatchAlertIntervalSeconds = it.toInt().coerceIn(1, 3600) },
                                valueRange = 1f..3600f,
                                steps = 3599,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = CosmicPrimary,
                                    activeTrackColor = CosmicSecondary,
                                    inactiveTrackColor = CosmicBorder
                                )
                            )

                            val inputModifier = if (isCompact) Modifier.weight(0.35f) else Modifier.weight(0.25f)
                            var showStopwatchTypedInput by remember { mutableStateOf(false) }
                            if (showStopwatchTypedInput) {
                                OutlinedTextField(
                                    value = if (viewModel.stopwatchAlertIntervalSeconds == 0) "" else viewModel.stopwatchAlertIntervalSeconds.toString(),
                                    onValueChange = { newVal ->
                                        val parsed = newVal.filter { it.isDigit() }.toIntOrNull() ?: 1
                                        viewModel.stopwatchAlertIntervalSeconds = parsed.coerceIn(1, 3600)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = CosmicSurfaceVariant,
                                        unfocusedContainerColor = CosmicSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedIndicatorColor = CosmicPrimary,
                                        unfocusedIndicatorColor = CosmicBorder
                                    ),
                                    modifier = inputModifier.testTag("stopwatch_custom_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else {
                                IconButton(
                                    onClick = { showStopwatchTypedInput = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CosmicSurfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit alerts manually",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (viewModel.stopwatchAlertType == "Interval") {
                                "🔔 Rings briefly every ${formatSecondsLabel(viewModel.stopwatchAlertIntervalSeconds)} to track question paces."
                            } else {
                                "🔔 Rings briefly once when stopwatch reaches ${formatSecondsLabel(viewModel.stopwatchAlertIntervalSeconds)}."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- LAP / CHECKPOINT MARKER BUTTON FOR STOPWATCH ---
        if (viewModel.timerMode == "Stopwatch" && viewModel.timerState == "Running") {
            Button(
                onClick = { viewModel.addStopwatchCheckpoint() },
                colors = ButtonDefaults.buttonColors(containerColor = CosmicSecondary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_checkpoint_main_btn"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Flag Checkpoint",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Question Checkpoint (Q${viewModel.stopwatchCheckpoints.size + 1})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // --- PLAY PAUSE RESET OPERATIONS TOOLBAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            IconButton(
                onClick = { viewModel.resetTimer() },
                enabled = viewModel.timerState != "Idle",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (viewModel.timerState != "Idle") CosmicSurface else CosmicSurfaceVariant)
                    .testTag("reset_timer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = if (viewModel.timerState != "Idle") Color.White else Color.DarkGray
                )
            }

            // Main Play/Pause trigger Action Button
            Button(
                onClick = {
                    if (viewModel.timerState == "Running") {
                        viewModel.pauseTimer()
                    } else {
                        viewModel.startTimer()
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = orbitColor),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("play_pause_timer_button")
            ) {
                Icon(
                    imageVector = if (viewModel.timerState == "Running") Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Trigger timer",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Alarm Toggler controls
            IconButton(
                onClick = { viewModel.alarmAlertsEnabled = !viewModel.alarmAlertsEnabled },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CosmicSurface)
                    .testTag("toggle_sound_bell")
            ) {
                Icon(
                    imageVector = if (viewModel.alarmAlertsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    contentDescription = "Bell status",
                    tint = if (viewModel.alarmAlertsEnabled) CosmicAccentAlert else Color.Gray
                )
            }
        }

        // --- STOPWATCH ACTIVE INTERACTIVE CHECKPOINTS LIST ---
        if (viewModel.timerMode == "Stopwatch" && viewModel.stopwatchCheckpoints.isNotEmpty()) {
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
                        Text(
                            text = "ACTIVE SESSION LAP SECONDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${viewModel.stopwatchCheckpoints.size} Solved",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicAccentCheck
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.stopwatchCheckpoints.reversed().forEach { cp ->
                            var isEditingLabel by remember { mutableStateOf(false) }
                            var editingText by remember { mutableStateOf(cp.label) }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CosmicSurfaceVariant,
                                border = BorderStroke(0.5.dp, CosmicBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(CosmicPrimary.copy(alpha = 0.2f))
                                                .border(BorderStroke(1.dp, CosmicPrimary), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${cp.questionNumber}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        if (isEditingLabel) {
                                            OutlinedTextField(
                                                value = editingText,
                                                onValueChange = { editingText = it },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = CosmicBackground,
                                                    unfocusedContainerColor = CosmicBackground,
                                                    focusedIndicatorColor = CosmicPrimary,
                                                    unfocusedIndicatorColor = CosmicBorder
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("checkpoint_edit_${cp.id}"),
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        viewModel.updateCheckpointLabel(cp.id, editingText)
                                                        isEditingLabel = false
                                                    }) {
                                                        Icon(Icons.Default.Check, contentDescription = "Save edit", tint = CosmicAccentCheck, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.clickable { isEditingLabel = true },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = cp.label,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit label",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Split: ${cp.splitFormatted}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CosmicAccentCheck,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "Total: ${cp.timeFormatted}",
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteStopwatchCheckpoint(cp.id) },
                                            modifier = Modifier.size(32.dp).testTag("delete_checkpoint_${cp.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete checkpoint",
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
            }
        }

        // --- SESSION ANNOTATION LOGGER (SAVE OVERLAYS) ---
        AnimatedVisibility(visible = viewModel.timerState == "Paused" || viewModel.timerState == "Finished") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = "Notes", tint = CosmicSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Focused Session Observations", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    OutlinedTextField(
                        value = viewModel.timerNotes,
                        onValueChange = { viewModel.timerNotes = it },
                        placeholder = { Text("E.g., Cleared half vector questions, needs backlog revision...", fontSize = 12.sp, color = Color.Gray) },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("session_notes_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CosmicSurfaceVariant,
                            unfocusedContainerColor = CosmicSurfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Button(
                        onClick = { viewModel.logCurrentSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicAccentCheck),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_session_log_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Focused Study Period", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
