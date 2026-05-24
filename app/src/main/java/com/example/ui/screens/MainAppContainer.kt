package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.ui.StudyViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainAppContainer(viewModel: StudyViewModel) {
    var isFloatingMode by remember { mutableStateOf(false) }
    var swipeOffsetX by remember { mutableStateOf(0f) }

    // Normalized drag progress: 0f = no drag, 1f = threshold reached (200px)
    val dragProgress = (swipeOffsetX / 200f).coerceIn(0f, 1f)

    // Coordinates of the drag floating button (relative to default bottom-right)
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()
    var snapJobX by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var snapJobY by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }
            val paddingEndPx = with(density) { 32.dp.toPx() }
            val paddingBottomPx = with(density) { 32.dp.toPx() }
            val fabSizePx = with(density) { 56.dp.toPx() }
            val marginPx = with(density) { 16.dp.toPx() }

            // Safe coordinate offsets relative to BottomEnd default position
            val minOffsetX = marginPx - (containerWidthPx - paddingEndPx - fabSizePx)
            val maxOffsetX = paddingEndPx - marginPx
            val minOffsetY = marginPx - (containerHeightPx - paddingBottomPx - fabSizePx)
            val maxOffsetY = paddingBottomPx - marginPx

            Column(modifier = Modifier.fillMaxSize()) {
                // Main Page Router switcher
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = viewModel.activeTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "PageTransitions"
                    ) { activeTab ->
                        when (activeTab) {
                            "targets" -> TargetsScreen(viewModel)
                            "timer" -> TimerScreen(viewModel)
                            "backlog" -> BacklogScreen(viewModel)
                            "dpp" -> DPPScreen(viewModel)
                            "history" -> HistoryScreen(viewModel)
                            else -> TargetsScreen(viewModel)
                        }
                    }
                }
            }

            if (isFloatingMode) {
                // DRAGGABLE FLOATING BUTTON
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset {
                            IntOffset(
                                fabOffsetX.roundToInt(),
                                fabOffsetY.roundToInt()
                            )
                        }
                        .padding(bottom = 32.dp, end = 32.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .size(56.dp)
                            .pointerInput(containerWidthPx, containerHeightPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        snapJobX?.cancel()
                                        snapJobY?.cancel()
                                    },
                                    onDragEnd = {
                                        val absoluteLeft = containerWidthPx - paddingEndPx - fabSizePx + fabOffsetX
                                        val absoluteCenter = absoluteLeft + (fabSizePx / 2f)
                                        val screenCenter = containerWidthPx / 2f
                                        val targetX = if (absoluteCenter < screenCenter) minOffsetX else 0f
                                        val targetY = fabOffsetY.coerceIn(minOffsetY, maxOffsetY)

                                        snapJobX?.cancel()
                                        snapJobX = coroutineScope.launch {
                                            androidx.compose.animation.core.animate(
                                                initialValue = fabOffsetX,
                                                targetValue = targetX,
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                )
                                            ) { value, _ ->
                                                fabOffsetX = value
                                            }
                                        }

                                        snapJobY?.cancel()
                                        snapJobY = coroutineScope.launch {
                                            androidx.compose.animation.core.animate(
                                                initialValue = fabOffsetY,
                                                targetValue = targetY,
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                )
                                            ) { value, _ ->
                                                fabOffsetY = value
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        val absoluteLeft = containerWidthPx - paddingEndPx - fabSizePx + fabOffsetX
                                        val absoluteCenter = absoluteLeft + (fabSizePx / 2f)
                                        val screenCenter = containerWidthPx / 2f
                                        val targetX = if (absoluteCenter < screenCenter) minOffsetX else 0f
                                        val targetY = fabOffsetY.coerceIn(minOffsetY, maxOffsetY)

                                        snapJobX?.cancel()
                                        snapJobX = coroutineScope.launch {
                                            androidx.compose.animation.core.animate(
                                                initialValue = fabOffsetX,
                                                targetValue = targetX,
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                )
                                            ) { value, _ ->
                                                fabOffsetX = value
                                            }
                                        }

                                        snapJobY?.cancel()
                                        snapJobY = coroutineScope.launch {
                                            androidx.compose.animation.core.animate(
                                                initialValue = fabOffsetY,
                                                targetValue = targetY,
                                                animationSpec = androidx.compose.animation.core.spring(
                                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                                )
                                            ) { value, _ ->
                                                fabOffsetY = value
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        fabOffsetX = (fabOffsetX + dragAmount.x).coerceIn(minOffsetX, maxOffsetX)
                                        fabOffsetY = (fabOffsetY + dragAmount.y).coerceIn(minOffsetY, maxOffsetY)
                                    }
                                )
                            }
                            .border(BorderStroke(1.5.dp, CosmicPrimary), CircleShape)
                            .clickable {
                                isFloatingMode = false
                                swipeOffsetX = 0f
                                fabOffsetX = 0f
                                fabOffsetY = 0f
                            }
                            .testTag("floating_restore_nav_fab"),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Restore page switching buttons",
                                tint = CosmicSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            } else {
                val animatedProgress by animateFloatAsState(
                    targetValue = dragProgress,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                    ),
                    label = "MorphProgress"
                )
                val navItemsAlpha = 1f - animatedProgress
                val fabIconAlpha = animatedProgress
                val glowIntensity = if (animatedProgress <= 0f || animatedProgress >= 1f) {
                    0f
                } else {
                    sin(animatedProgress * PI).toFloat()
                }

                val navWidth = maxWidth - 32.dp
                val morphWidth = lerp(navWidth, 56.dp, animatedProgress)
                val morphHeight = lerp(64.dp, 56.dp, animatedProgress)
                val cornerRadius = lerp(32.dp, 28.dp, animatedProgress)
                val morphShape: Shape = if (animatedProgress >= 1f) CircleShape else RoundedCornerShape(cornerRadius)

                // Floating Bottom Navigation Capsule with MORPHING GESTURE
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { swipeOffsetX = 0f },
                                onDragEnd = {
                                    if (swipeOffsetX > 200f) {
                                        isFloatingMode = true
                                    } else {
                                        // Snap back organically
                                        swipeOffsetX = 0f
                                    }
                                },
                                onDragCancel = { swipeOffsetX = 0f },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    swipeOffsetX += dragAmount
                                }
                            )
                        }
                ) {
                    FloatingBottomBar(
                        modifier = Modifier
                            .width(morphWidth)
                            .height(morphHeight)
                            .drawBehind {
                                if (glowIntensity > 0f) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val glowRadius = size.minDimension * (1.2f + 0.8f * animatedProgress)
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                CosmicPrimary.copy(alpha = 0.6f * glowIntensity),
                                                CosmicSecondary.copy(alpha = 0.3f * glowIntensity),
                                                Color.Transparent
                                            ),
                                            center = center,
                                            radius = glowRadius
                                        ),
                                        radius = glowRadius,
                                        center = center
                                    )
                                }
                            },
                        shape = morphShape,
                        navItemsAlpha = navItemsAlpha,
                        fabIconAlpha = fabIconAlpha,
                        activeTab = viewModel.activeTab,
                        onTabSelected = { viewModel.switchTab(it) }
                    )
                }
            }
        }
    }
}

// --- SUBCOMPONENT: FLOATING DEEP-GLOW BOTTOM NAVIGATION CAPSULE ---
@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    navItemsAlpha: Float = 1f,
    fabIconAlpha: Float = 0f,
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Card(
        modifier = modifier
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .alpha(navItemsAlpha),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    NavigationItem("targets", "Targets", Icons.Default.TrackChanges, Icons.Outlined.TrackChanges),
                    NavigationItem("timer", "Focus", Icons.Default.Timer, Icons.Outlined.Timer),
                    NavigationItem("backlog", "Debt", Icons.Default.Warning, Icons.Outlined.Warning),
                    NavigationItem("dpp", "DPPs", Icons.Default.Assessment, Icons.Outlined.Assessment),
                    NavigationItem("history", "History", Icons.Default.History, Icons.Outlined.History)
                ).forEach { navItem ->
                    val selected = activeTab == navItem.id

                    val itemColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    val glowBrush = if (selected) {
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        )
                    } else null

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(0.85f)
                            .clip(CircleShape)
                            .then(if (glowBrush != null) Modifier.background(glowBrush) else Modifier)
                            .clickable(enabled = navItemsAlpha > 0f) { onTabSelected(navItem.id) }
                            .testTag("nav_tab_${navItem.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (selected) navItem.activeIcon else navItem.inactiveIcon,
                                contentDescription = navItem.label,
                                tint = itemColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = navItem.label,
                                color = itemColor,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Restore page switching buttons",
                tint = CosmicSecondary.copy(alpha = fabIconAlpha),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(26.dp)
                    .alpha(fabIconAlpha)
            )
        }
    }
}

data class NavigationItem(
    val id: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

// Helper to handle max dimensions elegantly in dynamic compositions
fun Modifier.maxWidthIn(maxWidth: androidx.compose.ui.unit.Dp) = this.then(Modifier.widthIn(max = maxWidth))
fun Modifier.maxHeightIn(maxHeight: androidx.compose.ui.unit.Dp) = this.then(Modifier.heightIn(max = maxHeight))
