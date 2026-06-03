package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudyViewModel
import com.example.ui.theme.ColorBiology
import com.example.ui.theme.ColorChemistry
import com.example.ui.theme.ColorGeneral
import com.example.ui.theme.ColorMaths
import com.example.ui.theme.ColorOther
import com.example.ui.theme.ColorPhysics
import com.example.ui.theme.CosmicAccentCheck
import com.example.ui.theme.CosmicSurfaceVariant

@Composable
fun InsightsScreen(viewModel: StudyViewModel) {
    val heatmapDays by viewModel.insightsHeatmapDays.collectAsState()
    val subjectDistribution by viewModel.insightsSubjectDistribution.collectAsState()
    val maxSubjectSeconds = subjectDistribution.maxOfOrNull { it.totalSeconds }?.coerceAtLeast(1) ?: 1

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Insights & Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Study consistency for the last 60 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Consistency Heatmap",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    heatmapDays.chunked(10).forEach { rowDays ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowDays.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(heatmapColor(day.totalSeconds))
                                )
                            }
                        }
                    }
                    if (heatmapDays.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = heatmapDays.first().date,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = heatmapDays.last().date,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Subject Time Distribution",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subjectDistribution.isEmpty()) {
                        Text(
                            text = "No study sessions logged yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        subjectDistribution.forEach { entry ->
                            val progress = entry.totalSeconds.toFloat() / maxSubjectSeconds.toFloat()
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.subject,
                                        color = subjectInsightColor(entry.subject),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatHours(entry.totalSeconds),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = subjectInsightColor(entry.subject),
                                    trackColor = CosmicSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun heatmapColor(totalSeconds: Int): Color {
    if (totalSeconds <= 0) return CosmicSurfaceVariant
    val hours = totalSeconds / 3600f
    val alpha = when {
        hours < 2f -> 0.28f
        hours < 4f -> 0.48f
        hours < 6f -> 0.72f
        else -> 0.96f
    }
    return CosmicAccentCheck.copy(alpha = alpha)
}

private fun subjectInsightColor(subject: String): Color = when (subject) {
    "Physics" -> ColorPhysics
    "Chemistry" -> ColorChemistry
    "Maths" -> ColorMaths
    "Biology" -> ColorBiology
    "General" -> ColorGeneral
    else -> ColorOther
}

private fun formatHours(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return "${hours}h ${minutes}m"
}
