package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.style.TextAlign
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import kotlinx.coroutines.*
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.theme.*
import kotlin.math.abs

enum class QuickGestureDirection {
    CENTER, UP, DOWN, LEFT, RIGHT
}

sealed interface QuickGestureAction {
    val actionId: String
    val isContinuous: Boolean
    val icon: ImageVector

    enum class Immediate(override val actionId: String, override val icon: ImageVector) : QuickGestureAction {
        OpenDetails("OpenDetails", Icons.Default.Info),
        OpenWatchStatusPicker("OpenWatchStatusPicker", Icons.Default.List),
        ShareLink("ShareLink", Icons.Default.Share),
        None("None", Icons.Default.Close);

        override val isContinuous: Boolean = false
    }

    enum class Continuous(override val actionId: String, override val icon: ImageVector) : QuickGestureAction {
        ScoreSlider("ScoreSlider", Icons.Default.Star),
        EpisodeSlider("EpisodeSlider", Icons.Default.PlayArrow);

        override val isContinuous: Boolean = true
    }

    companion object {
        fun fromString(value: String): QuickGestureAction {
            return when (value) {
                "OpenDetails" -> Immediate.OpenDetails
                "OpenWatchStatusPicker" -> Immediate.OpenWatchStatusPicker
                "ShareLink" -> Immediate.ShareLink
                "ScoreSlider" -> Continuous.ScoreSlider
                "EpisodeSlider" -> Continuous.EpisodeSlider
                else -> Immediate.None
            }
        }
    }
}

@Composable
fun QuickGestureWrapper(
    anime: Anime,
    animeTitle: String,
    initialScore: Double?,
    initialEpisode: Int,
    gestureCenter: QuickGestureAction,
    gestureUp: QuickGestureAction,
    gestureDown: QuickGestureAction,
    gestureLeft: QuickGestureAction,
    gestureRight: QuickGestureAction,
    onGestureActionTriggered: (action: QuickGestureAction, value: Any?) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    onSliderStateChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clickAction: QuickGestureAction = QuickGestureAction.Immediate.OpenDetails,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var isPressed by remember { mutableStateOf(false) }
    var isGestureActive by remember { mutableStateOf(false) }
    var initialTouchAnchor by remember { mutableStateOf(Offset.Zero) }
    var currentTouchPosition by remember { mutableStateOf(Offset.Zero) }
    var activeDirection by remember { mutableStateOf<QuickGestureDirection?>(null) }
    var selectedAction by remember { mutableStateOf<QuickGestureAction?>(null) }
    var isContinuousSliderActive by remember { mutableStateOf(false) }
    
    // Sliders state
    var currentScoreValue by remember(initialScore) { mutableDoubleStateOf(initialScore ?: 0.0) }
    var currentEpisodeValue by remember(initialEpisode) { mutableIntStateOf(initialEpisode) }
    var sliderBaseScore by remember { mutableDoubleStateOf(0.0) }
    var sliderBaseEpisode by remember { mutableIntStateOf(0) }
    var scrollSpeedRate by remember { mutableFloatStateOf(0f) }
    var accumulatedDx by remember { mutableFloatStateOf(0f) }
    var sliderTouchAnchor by remember { mutableStateOf(Offset.Zero) }
    val cardBlurRadius by animateDpAsState(
        targetValue = if (isGestureActive && !isContinuousSliderActive) 8.dp else 0.dp,
        animationSpec = tween(200),
        label = "cardBlurRadius"
    )

    val isReleasing = anime.status?.uppercase() == "RELEASING"
    val airingEpisode = anime.airingEpisode
    val totalPlanned = anime.episodes

    val maxEp = remember(anime.status, anime.airingEpisode, anime.episodes) {
        val limit = anime.getMaxAllowedIncrement()
        if (limit == Int.MAX_VALUE) 9999 else limit
    }

    LaunchedEffect(isContinuousSliderActive, maxEp) {
        val maxEpVal = maxEp
        if (isContinuousSliderActive && maxEpVal >= 50) {
            try {
                while (true) {
                    val dx = currentTouchPosition.x - sliderTouchAnchor.x
                    val dy = currentTouchPosition.y - sliderTouchAnchor.y
                    val dxDp = dx / density.density
                    val dyDp = dy / density.density
                    val netDp = dxDp - dyDp
                    if (abs(netDp) > 100f) {
                        val direction = if (netDp > 0) 1 else -1
                        val excess = abs(netDp) - 100f
                        val rate = (5f + (excess / 10f) * 3f).coerceIn(5f, 45f)
                        scrollSpeedRate = rate
                        val tickDelay = (1000f / rate).toLong()

                        val oldVal = currentEpisodeValue
                        currentEpisodeValue = (currentEpisodeValue + direction).coerceIn(0, maxEpVal.coerceAtLeast(sliderBaseEpisode))
                        if (oldVal != currentEpisodeValue) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        delay(tickDelay)
                    } else {
                        scrollSpeedRate = 0f
                        delay(16L)
                    }
                }
            } finally {
                scrollSpeedRate = 0f
            }
        } else {
            scrollSpeedRate = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.pointerInput(initialScore, initialEpisode, maxEp, gestureCenter, gestureUp, gestureDown, gestureLeft, gestureRight) {
                coroutineScope {
                    var continuousActionJob: Job? = null
                    while (true) {
                        // 1. Wait for touch down
                        val down = awaitPointerEventScope {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                        val touchSlop = viewConfiguration.touchSlop
                        initialTouchAnchor = down.position
                        currentTouchPosition = down.position
                        isPressed = true
                        isGestureActive = false
                        activeDirection = null
                        selectedAction = null
                        isContinuousSliderActive = false
                        accumulatedDx = 0f
                        var wasFastScrolling = false
                        var isDragged = false
                        continuousActionJob?.cancel()
                        continuousActionJob = null

                        // 500ms Hold timer
                        val timerJob = launch {
                            delay(500L)
                            if (isPressed) {
                                isGestureActive = true
                                activeDirection = QuickGestureDirection.CENTER
                                selectedAction = gestureCenter
                                onDragStateChanged(true)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        // 2. Track pointer input
                        awaitPointerEventScope {
                            val dragId = down.id
                            var currentTime = down.uptimeMillis
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val dragEvent = event.changes.firstOrNull { it.id == dragId }
                                if (dragEvent == null || !dragEvent.pressed) {
                                    break
                                }

                                val position = dragEvent.position
                                val prevPosition = dragEvent.previousPosition
                                currentTouchPosition = position
                                val time = dragEvent.uptimeMillis
                                val timeDelta = time - currentTime
                                currentTime = time

                                val vector = position - initialTouchAnchor
                                val distancePx = vector.getDistance()
                                val distanceDp = distancePx / density.density

                                if (!isGestureActive && distanceDp > touchSlop / density.density) {
                                    timerJob.cancel()
                                    isDragged = true
                                }

                                if (isGestureActive) {
                                    dragEvent.consume()

                                     if (!isContinuousSliderActive) {
                                         if (distanceDp >= 40f) {
                                             val dx = vector.x
                                             val dy = vector.y
                                             val newDirection = if (abs(dx) > abs(dy)) {
                                                 if (dx > 0) QuickGestureDirection.RIGHT else QuickGestureDirection.LEFT
                                             } else {
                                                 if (dy > 0) QuickGestureDirection.DOWN else QuickGestureDirection.UP
                                             }
                                             if (activeDirection != newDirection) {
                                                 activeDirection = newDirection
                                                 haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                 continuousActionJob?.cancel()
                                                 continuousActionJob = null
                                             }

                                             val action = when (newDirection) {
                                                 QuickGestureDirection.UP -> gestureUp
                                                 QuickGestureDirection.DOWN -> gestureDown
                                                 QuickGestureDirection.LEFT -> gestureLeft
                                                 QuickGestureDirection.RIGHT -> gestureRight
                                                 else -> gestureCenter
                                             }
                                             selectedAction = action

                                             if (action.isContinuous) {
                                                 if (continuousActionJob == null) {
                                                     continuousActionJob = launch {
                                                         delay(250L) // hover delay before slider activates
                                                         isContinuousSliderActive = true
                                                         onSliderStateChanged(true)
                                                         sliderTouchAnchor = currentTouchPosition
                                                         sliderBaseScore = (Math.round((initialScore ?: 0.0) * 2.0) / 2.0)
                                                         sliderBaseEpisode = initialEpisode
                                                         currentScoreValue = sliderBaseScore
                                                         currentEpisodeValue = sliderBaseEpisode
                                                         accumulatedDx = 0f
                                                         wasFastScrolling = false
                                                     }
                                                 }
                                             } else {
                                                 continuousActionJob?.cancel()
                                                 continuousActionJob = null
                                             }
                                         } else {
                                             if (activeDirection != QuickGestureDirection.CENTER) {
                                                 activeDirection = QuickGestureDirection.CENTER
                                                 selectedAction = gestureCenter
                                                 haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                 continuousActionJob?.cancel()
                                                 continuousActionJob = null
                                             }
                                         }
                                     } else {
                                          // Slider processing
                                          val action = selectedAction
                                          val dx = position.x - sliderTouchAnchor.x
                                          val dy = position.y - sliderTouchAnchor.y
                                          val dxDp = dx / density.density
                                          val dyDp = dy / density.density
                                          val netDp = dxDp - dyDp

                                          if (action == QuickGestureAction.Continuous.ScoreSlider) {
                                              val newScore = (sliderBaseScore + netDp / 20.0).coerceIn(0.0, 10.0)
                                              val roundedScore = (Math.round(newScore * 2.0) / 2.0)
                                              if (currentScoreValue != roundedScore) {
                                                  currentScoreValue = roundedScore
                                                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                              }
                                          } else if (action == QuickGestureAction.Continuous.EpisodeSlider) {
                                               val maxEpVal = maxEp
                                               val isLongSeries = maxEpVal >= 50

                                               if (!isLongSeries) {
                                                   val dpPerEpisode = (300f / maxEpVal).coerceIn(4f, 20f)
                                                   val newEpisode = (sliderBaseEpisode + (netDp / dpPerEpisode).toInt()).coerceIn(0, maxEpVal.coerceAtLeast(sliderBaseEpisode))
                                                   if (currentEpisodeValue != newEpisode) {
                                                       currentEpisodeValue = newEpisode
                                                       haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                   }
                                               } else {
                                                   if (abs(netDp) <= 100f) {
                                                       if (wasFastScrolling) {
                                                           sliderTouchAnchor = position
                                                           sliderBaseEpisode = currentEpisodeValue
                                                           wasFastScrolling = false
                                                       }
                                                       // Re-calculate local displacement after resetting touch anchor
                                                       val localDx = position.x - sliderTouchAnchor.x
                                                       val localDy = position.y - sliderTouchAnchor.y
                                                       val localDxDp = localDx / density.density
                                                       val localDyDp = localDy / density.density
                                                       val localNetDp = localDxDp - localDyDp
                                                       val newEpisode = (sliderBaseEpisode + (localNetDp / 4f).toInt()).coerceIn(0, maxEpVal.coerceAtLeast(sliderBaseEpisode))
                                                       if (currentEpisodeValue != newEpisode) {
                                                           currentEpisodeValue = newEpisode
                                                           haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                       }
                                                   } else {
                                                       wasFastScrolling = true
                                                   }
                                               }
                                          }
                                    }
                                }
                            }
                        }

                        // 3. Release & Commit
                        timerJob.cancel()
                        continuousActionJob?.cancel()
                        continuousActionJob = null
                        isPressed = false

                        if (isGestureActive) {
                            onDragStateChanged(false)
                            isGestureActive = false
                            if (isContinuousSliderActive) {
                                isContinuousSliderActive = false
                                onSliderStateChanged(false)
                            }

                            val action = selectedAction
                            if (action != null) {
                                if (action.isContinuous) {
                                    val value = if (action == QuickGestureAction.Continuous.ScoreSlider) {
                                        currentScoreValue
                                    } else {
                                        currentEpisodeValue
                                    }
                                    onGestureActionTriggered(action, value)
                                } else {
                                    onGestureActionTriggered(action, null)
                                }
                            } else {
                                onGestureActionTriggered(clickAction, null)
                            }
                        } else {
                            if (!isDragged) {
                                onGestureActionTriggered(clickAction, null)
                            }
                        }
                    }
                }
            }
                } else {
                    Modifier
                }
            )
    ) {
        // Base content Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .blur(cardBlurRadius)
        ) {
            content()
        }

        // Direction & Sector Overlays - Refactored into a Unified Radial Menu
        AnimatedVisibility(
            visible = isGestureActive && !isContinuousSliderActive,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                val centerColor by animateColorAsState(
                    targetValue = if (activeDirection == QuickGestureDirection.CENTER) ElectricViolet.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.5f),
                    animationSpec = tween(150)
                )
                val upColor by animateColorAsState(
                    targetValue = if (activeDirection == QuickGestureDirection.UP) ElectricViolet.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.05f),
                    animationSpec = tween(150)
                )
                val downColor by animateColorAsState(
                    targetValue = if (activeDirection == QuickGestureDirection.DOWN) ElectricViolet.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.05f),
                    animationSpec = tween(150)
                )
                val leftColor by animateColorAsState(
                    targetValue = if (activeDirection == QuickGestureDirection.LEFT) ElectricViolet.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.05f),
                    animationSpec = tween(150)
                )
                val rightColor by animateColorAsState(
                    targetValue = if (activeDirection == QuickGestureDirection.RIGHT) ElectricViolet.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.05f),
                    animationSpec = tween(150)
                )

                val strings = LocalLocaleStrings.current

                BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val menuRadius = 86.dp
                    val menuRadiusPx = with(density) { menuRadius.toPx() }
                    val coreRadius = 28.dp
                    val coreRadiusPx = with(density) { coreRadius.toPx() }

                    Canvas(modifier = Modifier.size(menuRadius * 2)) {
                        val canvasCenter = Offset(size.width / 2, size.height / 2)
                        
                        val ringWidthPx = menuRadiusPx - coreRadiusPx
                        val strokeRadius = coreRadiusPx + ringWidthPx / 2
                        val arcSize = Size(strokeRadius * 2, strokeRadius * 2)
                        val arcTopLeft = Offset(canvasCenter.x - strokeRadius, canvasCenter.y - strokeRadius)
                        val wedgeStroke = Stroke(width = ringWidthPx)

                        // Draw UP wedge
                        drawArc(
                            color = upColor,
                            startAngle = -135f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = arcSize,
                            topLeft = arcTopLeft,
                            style = wedgeStroke
                        )
                        // Draw RIGHT wedge
                        drawArc(
                            color = rightColor,
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = arcSize,
                            topLeft = arcTopLeft,
                            style = wedgeStroke
                        )
                        // Draw DOWN wedge
                        drawArc(
                            color = downColor,
                            startAngle = 45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = arcSize,
                            topLeft = arcTopLeft,
                            style = wedgeStroke
                        )
                        // Draw LEFT wedge
                        drawArc(
                            color = leftColor,
                            startAngle = 135f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = arcSize,
                            topLeft = arcTopLeft,
                            style = wedgeStroke
                        )

                        // Draw CENTER core circle
                        drawCircle(
                            color = centerColor,
                            radius = coreRadiusPx,
                            center = canvasCenter
                        )

                        // Outer and inner borders
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = menuRadiusPx,
                            center = canvasCenter,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.25f),
                            radius = coreRadiusPx,
                            center = canvasCenter,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Render icons and localized text on top of the Canvas sectors using offset placement
                    val placementRadius = 54.dp

                    RadialLabel(
                        action = gestureCenter,
                        label = strings.misc.gestureCenterDir,
                        isActive = activeDirection == QuickGestureDirection.CENTER,
                        modifier = Modifier.offset(x = 0.dp, y = 0.dp)
                    )
                    RadialLabel(
                        action = gestureUp,
                        label = strings.misc.gestureUpDir,
                        isActive = activeDirection == QuickGestureDirection.UP,
                        modifier = Modifier.offset(x = 0.dp, y = -placementRadius)
                    )
                    RadialLabel(
                        action = gestureDown,
                        label = strings.misc.gestureDownDir,
                        isActive = activeDirection == QuickGestureDirection.DOWN,
                        modifier = Modifier.offset(x = 0.dp, y = placementRadius)
                    )
                    RadialLabel(
                        action = gestureLeft,
                        label = strings.misc.gestureLeftDir,
                        isActive = activeDirection == QuickGestureDirection.LEFT,
                        modifier = Modifier.offset(x = -placementRadius, y = 0.dp)
                    )
                    RadialLabel(
                        action = gestureRight,
                        label = strings.misc.gestureRightDir,
                        isActive = activeDirection == QuickGestureDirection.RIGHT,
                        modifier = Modifier.offset(x = placementRadius, y = 0.dp)
                    )
                }
            }
        }

        // Full-Screen Immersive Slider Overlay
        if (isGestureActive && isContinuousSliderActive) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                val view = androidx.compose.ui.platform.LocalView.current
                var dialogWindow by remember { mutableStateOf<android.view.Window?>(null) }
                val densityVal = view.resources.displayMetrics.density

                var isTransitionStarted by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    isTransitionStarted = true
                }
                val animatedBlurRadius by animateIntAsState(
                    targetValue = if (isTransitionStarted) 24 else 0,
                    animationSpec = tween(250),
                    label = "windowBlur"
                )

                LaunchedEffect(dialogWindow, animatedBlurRadius) {
                    val window = dialogWindow
                    if (window != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        val blurRadiusPx = (animatedBlurRadius * densityVal).toInt()
                        window.setBackgroundBlurRadius(blurRadiusPx)
                        val lp = window.attributes
                        lp.blurBehindRadius = blurRadiusPx
                        window.attributes = lp
                    }
                }

                DisposableEffect(view) {
                    var parent = view.parent
                    var window: android.view.Window? = null
                    while (parent != null) {
                        if (parent is androidx.compose.ui.window.DialogWindowProvider) {
                            window = parent.window
                            break
                        }
                        parent = parent.parent
                    }
                    dialogWindow = window
                    if (window != null) {
                        window.addFlags(
                            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                        )
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                        window.setLayout(
                            android.view.WindowManager.LayoutParams.MATCH_PARENT,
                            android.view.WindowManager.LayoutParams.MATCH_PARENT
                        )
                    }
                    onDispose {
                        if (window != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            window.setBackgroundBlurRadius(0)
                            val lp = window.attributes
                            lp.blurBehindRadius = 0
                            window.attributes = lp
                        }
                    }
                }

                val transitionState = remember {
                    MutableTransitionState(false).apply {
                        targetState = true
                    }
                }

                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = fadeIn(animationSpec = tween(250)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = animeTitle,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 48.dp)
                                .padding(horizontal = 24.dp)
                        )

                        val innerTransitionState = remember {
                            MutableTransitionState(false).apply {
                                targetState = true
                            }
                        }

                        AnimatedVisibility(
                            visibleState = innerTransitionState,
                            enter = fadeIn(animationSpec = tween(250)) + scaleIn(
                                initialScale = 0.85f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                 val action = selectedAction
                                 val strings = LocalLocaleStrings.current
                                 val title = if (action == QuickGestureAction.Continuous.ScoreSlider) strings.misc.setScore else strings.misc.setEpisodes
                                 val progressColor = if (action == QuickGestureAction.Continuous.ScoreSlider) NeonCoral else CyberTeal
 
                                 Text(
                                     text = title.uppercase(),
                                     color = TextPrimary.copy(alpha = 0.6f),
                                     fontSize = 12.sp,
                                     fontWeight = FontWeight.Bold,
                                     letterSpacing = 2.sp
                                 )
 
                                 Spacer(modifier = Modifier.height(32.dp))
 
                                 // Large Floating Value Bubble (Premium Visual)
                                 val bubbleText = if (action == QuickGestureAction.Continuous.ScoreSlider) {
                                     String.format("%.1f", currentScoreValue)
                                 } else {
                                     val limit = anime.getReleasedEpisodes()
                                     val maxEpDisplay = if (limit != null && limit > 0) limit.toString() else "?"
                                     val prefix = strings.misc.episodeLabel
                                     "$prefix $currentEpisodeValue / $maxEpDisplay"
                                 }

                                // Scrolling indicator if fast scrubbing is active
                                val dx = currentTouchPosition.x - sliderTouchAnchor.x
                                val dy = currentTouchPosition.y - sliderTouchAnchor.y
                                val net = dx - dy
                                val isScrollingFast = action == QuickGestureAction.Continuous.EpisodeSlider && maxEp >= 50 && scrollSpeedRate > 0f

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isScrollingFast && net < 0) {
                                        ChevronIndicator(direction = -1, rate = scrollSpeedRate)
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Text(
                                        text = bubbleText,
                                        color = TextPrimary,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = progressColor.copy(alpha = 0.8f),
                                                offset = Offset(0f, 0f),
                                                blurRadius = 16f
                                            )
                                        )
                                    )

                                    if (isScrollingFast && net > 0) {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        ChevronIndicator(direction = 1, rate = scrollSpeedRate)
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // Immersive Slider track (280dp wide, centered)
                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    val fraction = if (action == QuickGestureAction.Continuous.ScoreSlider) {
                                         (currentScoreValue / 10.0).toFloat().coerceIn(0f, 1f)
                                    } else {
                                        (currentEpisodeValue.toFloat() / maxEp.toFloat()).coerceIn(0f, 1f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(progressColor.copy(alpha = 0.7f), progressColor)
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Help instructions
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = if (action == QuickGestureAction.Continuous.EpisodeSlider && maxEp >= 30) {
                                        strings.settingsScreen.dragToScrollFast
                                    } else {
                                        strings.settingsScreen.dragToAdjust
                                    },
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadialLabel(
    action: QuickGestureAction,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.55f,
        animationSpec = tween(150)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .size(54.dp)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = label,
            tint = if (isActive) ElectricViolet else Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) TextPrimary else TextSecondary.copy(alpha = 0.8f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChevronIndicator(
    direction: Int, // -1 for left, 1 for right
    rate: Float,
    modifier: Modifier = Modifier
) {
    val duration = (600f / (rate / 5f)).toInt().coerceIn(80, 400)
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingChevrons")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevronAlpha"
    )

    val count = when {
        rate > 30f -> 3
        rate > 15f -> 2
        else -> 1
    }
    
    val arrow = if (direction > 0) ">" else "<"
    val text = arrow.repeat(count)
    
    Text(
        text = text,
        color = (if (direction > 0) CyberTeal else NeonCoral).copy(alpha = alpha),
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
    )
}

fun handleQuickGestureAction(
    context: android.content.Context,
    anime: Anime,
    action: QuickGestureAction,
    value: Any?,
    onOpenDetails: () -> Unit,
    onOpenWatchStatusPicker: () -> Unit,
    onScoreChange: (Double) -> Unit,
    onEpisodeChange: (Int) -> Unit
) {
    when (action) {
        QuickGestureAction.Immediate.OpenDetails -> onOpenDetails()
        QuickGestureAction.Immediate.OpenWatchStatusPicker -> onOpenWatchStatusPicker()
        QuickGestureAction.Immediate.ShareLink -> {
            val sendIntent = android.content.Intent().apply {
                setAction(android.content.Intent.ACTION_SEND)
                putExtra(android.content.Intent.EXTRA_TEXT, "Check out ${anime.titleRomaji} on AniForge: https://anilist.co/anime/${anime.anilistId}")
                setType("text/plain")
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        QuickGestureAction.Continuous.ScoreSlider -> {
            if (value is Double) {
                onScoreChange(value)
            }
        }
        QuickGestureAction.Continuous.EpisodeSlider -> {
            if (value is Int) {
                onEpisodeChange(value)
            }
        }
        else -> {}
    }
}

