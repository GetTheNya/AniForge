package moe.GetTheNya.AniForge.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.entity.WidgetConfigEntity
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.bento.*
import moe.GetTheNya.AniForge.ui.dashboard.BentoStatsCard
import moe.GetTheNya.AniForge.ui.dashboard.FeaturedBentoCard
import moe.GetTheNya.AniForge.ui.localization.LocaleStrings
import moe.GetTheNya.AniForge.ui.theme.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlin.math.roundToInt

fun Modifier.jiggle(enabled: Boolean): Modifier = composed {
    if (!enabled) return@composed this
    
    val transition = rememberInfiniteTransition(label = "jiggle")
    
    val rotation by transition.animateFloat(
        initialValue = -1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleRotation"
    )
    
    val translationX by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleX"
    )
    
    val translationY by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleY"
    )
    
    this.graphicsLayer {
        rotationZ = rotation
        this.translationX = translationX
        this.translationY = translationY
    }
}

class DragGridState(
    val lazyGridState: LazyGridState,
    val onMove: (String, String) -> Unit,
    val onDragEnd: () -> Unit,
    val onDelete: (String) -> Unit
) {
    var draggedWidgetId by mutableStateOf<String?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)
    var gridCoordinates by mutableStateOf<LayoutCoordinates?>(null)
    var hapticFeedback: HapticFeedback? = null
    var dragPointerWindow by mutableStateOf(Offset.Zero)
    var draggedWidth by mutableStateOf(0f)
    var draggedHeight by mutableStateOf(0f)
    var expectedDraggedIndex by mutableStateOf<Int?>(null)

    var initialItemWindowPos by mutableStateOf(Offset.Zero)
    var initialPointerWindowPos by mutableStateOf(Offset.Zero)

    fun onDragStart(
        widgetId: String,
        initialItemWindowPos: Offset,
        initialPointerWindowPos: Offset,
        startIndex: Int
    ) {
        draggedWidgetId = widgetId
        dragOffset = Offset.Zero
        this.initialItemWindowPos = initialItemWindowPos
        this.initialPointerWindowPos = initialPointerWindowPos
        dragPointerWindow = initialPointerWindowPos
        expectedDraggedIndex = startIndex
        
        val item = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == widgetId }
        if (item != null) {
            draggedWidth = item.size.width.toFloat()
            draggedHeight = item.size.height.toFloat()
        } else {
            draggedWidth = 0f
            draggedHeight = 0f
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onDrag(dragAmount: Offset) {
        dragOffset += dragAmount
        dragPointerWindow = initialPointerWindowPos + dragOffset

        val gridCoords = gridCoordinates ?: return
        if (!gridCoords.isAttached) return
        val pointerInGrid = gridCoords.windowToLocal(dragPointerWindow)

        val draggedId = draggedWidgetId ?: return
        val visibleItems = lazyGridState.layoutInfo.visibleItemsInfo
        
        val draggedItem = visibleItems.firstOrNull { it.key == draggedId } ?: return
        val expectedIdx = expectedDraggedIndex
         
        if (expectedIdx != null && draggedItem.index != expectedIdx) {
            return
        }

        val targetItem = visibleItems.firstOrNull { item ->
            if (item.key == draggedId) return@firstOrNull false
            val rect = Rect(
                item.offset.x.toFloat(),
                item.offset.y.toFloat(),
                (item.offset.x + item.size.width).toFloat(),
                (item.offset.y + item.size.height).toFloat()
            )
            if (!rect.contains(pointerInGrid)) return@firstOrNull false
            
            val isVerticallyOffset = kotlin.math.abs(item.offset.y - draggedItem.offset.y) > 5
            val shouldSwap = if (isVerticallyOffset) {
                if (item.offset.y > draggedItem.offset.y) {
                    pointerInGrid.y > item.offset.y + item.size.height / 2f
                } else {
                    pointerInGrid.y < item.offset.y + item.size.height / 2f
                }
            } else {
                if (item.offset.x > draggedItem.offset.x) {
                    pointerInGrid.x > item.offset.x + item.size.width / 2f
                } else {
                    pointerInGrid.x < item.offset.x + item.size.width / 2f
                }
            }
            shouldSwap
        }
        
        if (targetItem != null) {
            val targetId = targetItem.key as String
            onMove(draggedId, targetId)
        }
    }

    fun onDragEnd(isOverDeleteZone: Boolean) {
        val draggedId = draggedWidgetId
        if (draggedId != null) {
            if (isOverDeleteZone) {
                onDelete(draggedId)
            }
        }
        draggedWidgetId = null
        dragOffset = Offset.Zero
        dragPointerWindow = Offset.Zero
        expectedDraggedIndex = null
        initialItemWindowPos = Offset.Zero
        initialPointerWindowPos = Offset.Zero
        onDragEnd()
    }
}

@Composable
fun rememberDragGridState(
    lazyGridState: LazyGridState,
    onMove: (String, String) -> Unit,
    onDragEnd: () -> Unit,
    onDelete: (String) -> Unit
): DragGridState {
    val currentOnMove = rememberUpdatedState(onMove)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    val currentOnDelete = rememberUpdatedState(onDelete)
    
    return remember(lazyGridState) {
        DragGridState(
            lazyGridState = lazyGridState,
            onMove = { from, to -> currentOnMove.value(from, to) },
            onDragEnd = { currentOnDragEnd.value() },
            onDelete = { id -> currentOnDelete.value(id) }
        )
    }
}

fun getWidgetName(widgetId: String, strings: LocaleStrings): String {
    return when (widgetId) {
        "watch_time" -> strings.bentoWidgets.watchTimeTitle
        "chaos_meter" -> strings.bentoWidgets.chaosMeter
        "personal_collections" -> strings.bentoWidgets.activeCollections
        "top_studios" -> strings.dashboardScreen.studios
        "top_genres" -> strings.dashboardScreen.genres
        "franchise_giant" -> strings.bentoWidgets.mostWatchedUniverse
        "watch_status_chart" -> strings.bentoWidgets.watchStatusTitle
        else -> widgetId
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (Long) -> Unit,
    onGenreClick: (String) -> Unit,
    onStudioClick: (Long) -> Unit,
    onCollectionClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val uiState by viewModel.homeUiState.collectAsState()
    val randomSubtitle by viewModel.randomWelcomeSubtitle.collectAsState()

    // Animation presets
    val currentTitleStyle by viewModel.titleAnimStyle.collectAsState()
    val currentSubtitleStyle by viewModel.subtitleAnimStyle.collectAsState()
    val currentContentStyle by viewModel.contentAnimStyle.collectAsState()

    // Capture the session-wide played flag on first composition to prevent race condition during database load
    val isAnimationPlayed = remember { viewModel.isHomeAnimationPlayed }

    // Mark as played in a session-wide LaunchedEffect once composed
    LaunchedEffect(Unit) {
        viewModel.isHomeAnimationPlayed = true
    }

    // A. Dynamic Title Animation Logic
    val titleText = strings.homeScreen.appName
    val cipherPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@&?%*"
    var decodedTitle by remember(titleText, currentTitleStyle) {
        mutableStateOf(
            if (isAnimationPlayed || currentTitleStyle != TitleAnimStyle.DECODING) {
                titleText
            } else {
                // Initialize with fully random characters of same length to prevent layout shift
                CharArray(titleText.length) { cipherPool.random() }.concatToString()
            }
        )
    }

    LaunchedEffect(titleText, currentTitleStyle) {
        if (!isAnimationPlayed && currentTitleStyle == TitleAnimStyle.DECODING) {
            try {
                delay(200L)
                val duration = 380L
                val charsCount = titleText.length
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= duration) {
                        break
                    }
                    val percent = elapsed.toFloat() / duration
                    val lockedCount = (percent * charsCount).toInt().coerceIn(0, charsCount)
                    val randomChars = CharArray(charsCount) { i ->
                        if (i < lockedCount) {
                            titleText[i]
                        } else {
                            cipherPool.random()
                        }
                    }
                    decodedTitle = randomChars.concatToString()
                    delay(35)
                }
            } finally {
                // Guaranteed resolution: always resolve to the correct targetText upon completion/cancellation
                decodedTitle = titleText
            }
        } else {
            decodedTitle = titleText
        }
    }

    var titleGlitchAlpha by remember { mutableStateOf(1f) }
    var titleGlitchTranslationX by remember { mutableStateOf(0f) }

    LaunchedEffect(titleText, currentTitleStyle) {
        if (!isAnimationPlayed && currentTitleStyle == TitleAnimStyle.GLITCH) {
            val duration = 300L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= duration) break
                titleGlitchAlpha = kotlin.random.Random.nextFloat() * 0.6f + 0.4f
                titleGlitchTranslationX = kotlin.random.Random.nextInt(-6, 7).toFloat()
                delay(30L)
            }
            titleGlitchAlpha = 1f
            titleGlitchTranslationX = 0f
        } else {
            titleGlitchAlpha = 1f
            titleGlitchTranslationX = 0f
        }
    }

    val titleProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!isAnimationPlayed) {
            delay(200L)
            titleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    // B. Greeting & Subtext Smooth Unblur & Alpha Fade progress
    val cleanSubtitle = remember(randomSubtitle) {
        randomSubtitle?.let {
            it.replace(Regex("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        } ?: ""
    }

    var typedGreeting by remember(strings.homeScreen.welcomeBack, currentSubtitleStyle) {
        mutableStateOf(
            if (isAnimationPlayed || currentSubtitleStyle != SubtitleAnimStyle.TYPEWRITER) {
                strings.homeScreen.welcomeBack.replace("!", "")
            } else {
                ""
            }
        )
    }

    var typedSubtitle by remember(cleanSubtitle, currentSubtitleStyle) {
        mutableStateOf(
            if (isAnimationPlayed || currentSubtitleStyle != SubtitleAnimStyle.TYPEWRITER) {
                cleanSubtitle
            } else {
                ""
            }
        )
    }

    var cursorPosition by remember { mutableStateOf(0) }
    var cursorBlink by remember { mutableStateOf(false) }

    LaunchedEffect(cursorPosition) {
        if (cursorPosition != 0) {
            while (true) {
                cursorBlink = !cursorBlink
                delay(300L)
            }
        } else {
            cursorBlink = false
        }
    }

    LaunchedEffect(strings.homeScreen.welcomeBack, cleanSubtitle, currentSubtitleStyle) {
        if (!isAnimationPlayed && currentSubtitleStyle == SubtitleAnimStyle.TYPEWRITER) {
            val fullGreeting = strings.homeScreen.welcomeBack.replace("!", "")
            typedGreeting = ""
            typedSubtitle = ""
            cursorPosition = 1
            delay(200L)
            for (i in 1..fullGreeting.length) {
                typedGreeting = fullGreeting.substring(0, i)
                delay(25L)
            }
            if (cleanSubtitle.isNotEmpty()) {
                delay(100L)
                cursorPosition = 2
                for (i in 1..cleanSubtitle.length) {
                    typedSubtitle = cleanSubtitle.substring(0, i)
                    delay(25L)
                }
            }
            delay(800L)
            cursorPosition = 0
        } else {
            typedGreeting = strings.homeScreen.welcomeBack.replace("!", "")
            typedSubtitle = cleanSubtitle
            cursorPosition = 0
        }
    }

    val headerProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(currentSubtitleStyle) {
        if (!isAnimationPlayed && (currentSubtitleStyle == SubtitleAnimStyle.BLUR_FADE || currentSubtitleStyle == SubtitleAnimStyle.WORD_BY_WORD)) {
            delay(200L) // breathing room delay (synchronized with title decoding)
            headerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    // C. Digital Power-Up for Bento widgets
    val contentProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(currentContentStyle) {
        if (!isAnimationPlayed && currentContentStyle != ContentAnimStyle.NONE) {
            delay(180L) // 180ms delay from launch
            val duration = if (currentContentStyle == ContentAnimStyle.FLIP_3D) 450 else 400
            contentProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    val isEditMode by viewModel.isEditMode.collectAsState()
    BackHandler(enabled = isEditMode) {
        viewModel.exitEditMode()
    }
    var showRestorationSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = remember(context) { context as? android.app.Activity }

    var rootBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var topBarBottomInWindow by remember { mutableStateOf(0f) }

    var dismissingWidgetId by remember { mutableStateOf<String?>(null) }
    var dismissingOffset by remember { mutableStateOf(Offset.Zero) }
    var dismissingWidth by remember { mutableStateOf(0f) }
    var dismissingHeight by remember { mutableStateOf(0f) }
    val dismissProgress = remember { Animatable(1f) }

    var placingWidgetId by remember { mutableStateOf<String?>(null) }
    var placingStartOffset by remember { mutableStateOf(Offset.Zero) }
    var placingWidth by remember { mutableStateOf(0f) }
    var placingHeight by remember { mutableStateOf(0f) }
    val placingProgress = remember { Animatable(0f) }
    val widgetCoordinates = remember { mutableStateMapOf<String, LayoutCoordinates>() }

    val dragGridStateHolder = remember { mutableStateOf<DragGridState?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val lazyGridState = rememberLazyGridState()

    val density = LocalDensity.current
    var expandedHeaderHeightPx by remember { mutableStateOf(0f) }
    val collapsedHeaderHeightPx = remember { with(density) { 64.dp.toPx() } }

    val scrollOffset = remember {
        derivedStateOf {
            if (lazyGridState.firstVisibleItemIndex == 0) {
                lazyGridState.firstVisibleItemScrollOffset
            } else {
                100000
            }
        }
    }

    val maxScrollPx = remember {
        derivedStateOf {
            (expandedHeaderHeightPx - collapsedHeaderHeightPx).coerceAtLeast(1f)
        }
    }

    val collapseFraction = remember {
        derivedStateOf {
            if (expandedHeaderHeightPx > 0f) {
                (scrollOffset.value.toFloat() / maxScrollPx.value).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val currentHeaderHeightDp = remember {
        derivedStateOf {
            if (isEditMode) {
                64.dp
            } else if (expandedHeaderHeightPx == 0f) {
                Dp.Unspecified
            } else {
                val expandedDp = with(density) { expandedHeaderHeightPx.toDp() }
                val scrolledDp = with(density) { scrollOffset.value.toDp() }
                (expandedDp - scrolledDp).coerceIn(64.dp, expandedDp)
            }
        }
    }

    val hapticFeedback = LocalHapticFeedback.current
    val dragGridState = rememberDragGridState(
        lazyGridState = lazyGridState,
        onMove = { fromId, toId ->
            val firstVisibleIdx = lazyGridState.firstVisibleItemIndex
            val firstVisibleScrollOffset = lazyGridState.firstVisibleItemScrollOffset
            
            val successState = uiState as? HomeUiState.Success
            if (successState != null) {
                val visibleConfigs = if (isEditMode) {
                    viewModel.editableWidgetConfigs.filter { it.isVisible }
                } else {
                    successState.widgetConfigs.filter { it.isVisible }
                }
                val fromIdx = visibleConfigs.indexOfFirst { it.widgetId == fromId }
                val toIdx = visibleConfigs.indexOfFirst { it.widgetId == toId }
                
                if (fromIdx != -1 && toIdx != -1) {
                    dragGridStateHolder.value?.expectedDraggedIndex = toIdx + 1
                }
            }
            
            // Instantly request the scroll position to be retained in the next remeasure pass to bypass scroll anchoring jumps
            lazyGridState.requestScrollToItem(firstVisibleIdx, firstVisibleScrollOffset)
            viewModel.moveWidget(fromId, toId)
        },
        onDragEnd = { viewModel.saveWidgetOrder() },
        onDelete = { id ->
            val gridState = dragGridStateHolder.value
            if (gridState != null && rootBoxCoordinates != null && rootBoxCoordinates!!.isAttached) {
                val item = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
                if (item != null) {
                    dismissingWidth = item.size.width.toFloat()
                    dismissingHeight = item.size.height.toFloat()
                    dismissingOffset = rootBoxCoordinates!!.windowToLocal(gridState.initialItemWindowPos + gridState.dragOffset)
                    dismissingWidgetId = id
                } else {
                    viewModel.deleteWidget(id)
                }
            } else {
                viewModel.deleteWidget(id)
            }
        }
    ).apply {
        this.hapticFeedback = hapticFeedback
    }
    dragGridStateHolder.value = dragGridState

    val isOverDeleteZone = dragGridState.draggedWidgetId != null && dragGridState.dragPointerWindow.y <= topBarBottomInWindow

    val animatedStatusBarColor by animateColorAsState(
        targetValue = if (isOverDeleteZone) Color(0xFFD32F2F) else BackgroundDark,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "statusBarColor"
    )

    LaunchedEffect(animatedStatusBarColor, activity) {
        activity?.window?.let { window ->
            window.statusBarColor = animatedStatusBarColor.toArgb()
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { window ->
                window.statusBarColor = BackgroundDark.toArgb()
            }
        }
    }

    LaunchedEffect(dismissingWidgetId) {
        val targetId = dismissingWidgetId
        if (targetId != null) {
            dismissProgress.snapTo(1f)
            dismissProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutLinearInEasing
                )
            )
            viewModel.deleteWidget(targetId)
            dismissingWidgetId = null
            dismissingOffset = Offset.Zero
        }
    }

    LaunchedEffect(placingWidgetId) {
        val targetId = placingWidgetId
        if (targetId != null) {
            placingProgress.snapTo(0f)
            placingProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            placingProgress.snapTo(0f)
            placingWidgetId = null
        }
    }

    val stateType = when (uiState) {
        is HomeUiState.Loading -> "loading"
        is HomeUiState.Error -> "error"
        is HomeUiState.Success -> "success"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .onGloballyPositioned { rootBoxCoordinates = it }
    ) {
        Crossfade(
            targetState = stateType,
            modifier = Modifier.fillMaxSize(),
            label = "homeCrossfade"
        ) { type ->
            when (type) {
                "loading" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCoral)
                    }
                }
                "error" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val errorState = uiState as? HomeUiState.Error
                        val errorMessage = when (errorState?.message) {
                            "Unknown error" -> strings.dashboardScreen.unknownError
                            else -> errorState?.message ?: ""
                        }
                        Text(text = "${strings.misc.error}: $errorMessage", color = NeonCoral)
                    }
                }
                "success" -> {
                    val successState = uiState as? HomeUiState.Success
                    if (successState != null) {
                        HomeScreenGrid(
                            state = successState,
                            viewModel = viewModel,
                            isEditMode = isEditMode,
                            dragGridState = dragGridState,
                            topBarBottomInWindow = topBarBottomInWindow,
                            dismissingWidgetId = dismissingWidgetId,
                            placingWidgetId = placingWidgetId,
                            widgetCoordinates = widgetCoordinates,
                            lazyGridState = lazyGridState,
                            expandedHeaderHeightPx = expandedHeaderHeightPx,
                            onAnimeClick = onAnimeClick,
                            onGenreClick = onGenreClick,
                            onStudioClick = onStudioClick,
                            onCollectionClick = onCollectionClick,
                            onStatusClick = onStatusClick,
                            onAddWidgetsClick = {
                                viewModel.enterEditMode()
                                showRestorationSheet = true
                            },
                            onDragRelease = { widgetId, initialItemWindowPos, dragOffset, width, height, isOverDeleteZone ->
                                if (!isOverDeleteZone && rootBoxCoordinates != null && rootBoxCoordinates!!.isAttached) {
                                    coroutineScope.launch {
                                        placingProgress.snapTo(0f)
                                    }
                                    placingWidgetId = widgetId
                                    placingStartOffset = rootBoxCoordinates!!.windowToLocal(initialItemWindowPos + dragOffset)
                                    placingWidth = width
                                    placingHeight = height
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val progress = contentProgress.value
                                    alpha = progress
                                    when (currentContentStyle) {
                                        ContentAnimStyle.NONE -> {
                                            alpha = 1f
                                            scaleX = 1f
                                            scaleY = 1f
                                            translationY = 0f
                                            rotationX = 0f
                                        }
                                        ContentAnimStyle.POWER_UP -> {
                                            val scale = 0.95f + 0.05f * progress
                                            scaleX = scale
                                            scaleY = scale
                                            translationY = 0f
                                            rotationX = 0f
                                        }
                                        ContentAnimStyle.SLIDE_UP -> {
                                            scaleX = 1f
                                            scaleY = 1f
                                            translationY = 80.dp.toPx() * (1f - progress)
                                            rotationX = 0f
                                        }
                                        ContentAnimStyle.FLIP_3D -> {
                                            scaleX = 1f
                                            scaleY = 1f
                                            translationY = 0f
                                            rotationX = 90f * (1f - progress)
                                            cameraDistance = 12f * this.density
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(5f)
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(animatedStatusBarColor)
            )

            val heightModifier = if (currentHeaderHeightDp.value != Dp.Unspecified) {
                Modifier.height(currentHeaderHeightDp.value)
            } else {
                Modifier.wrapContentHeight()
            }

            val headerBgColor = if (isEditMode) {
                BackgroundDark
            } else {
                androidx.compose.ui.graphics.lerp(Color.Transparent, BackgroundDark, collapseFraction.value)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(heightModifier)
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached && scrollOffset.value == 0 && !isEditMode) {
                            expandedHeaderHeightPx = coords.size.height.toFloat()
                        }
                        if (coords.isAttached) {
                            topBarBottomInWindow = coords.localToWindow(Offset.Zero).y + coords.size.height
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                animateColorAsState(
                                    targetValue = if (isOverDeleteZone) Color(0xFFD32F2F).copy(alpha = 0.9f) else headerBgColor,
                                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                                    label = "topBarBg"
                                ).value,
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val contentAlpha by animateFloatAsState(
                    targetValue = if (isOverDeleteZone) 0f else 1f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "contentAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha }
                ) {
                    if (!isEditMode) {
                        val fraction = collapseFraction.value
                        val titleSize = (56 - (56 - 20) * fraction).sp
                        val metadataAlpha = (1f - fraction * 3f).coerceIn(0f, 1f)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = lerp(24.dp, 18.dp, fraction),
                                    bottom = lerp(32.dp, 18.dp, fraction)
                                )
                        ) {
                            Text(
                                text = decodedTitle,
                                fontSize = titleSize,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.5).sp,
                                color = Color.White,
                                modifier = Modifier.graphicsLayer {
                                    if (!isAnimationPlayed) {
                                        when (currentTitleStyle) {
                                            TitleAnimStyle.NONE -> { alpha = 1f; translationX = 0f; rotationY = 0f }
                                            TitleAnimStyle.SLIDE_SIDE -> {
                                                val progress = titleProgress.value
                                                alpha = progress
                                                translationX = -100f * (1f - progress)
                                                rotationY = 0f
                                            }
                                            TitleAnimStyle.TURNSTILE_3D -> {
                                                val progress = titleProgress.value
                                                alpha = progress
                                                rotationY = 25f * (1f - progress)
                                                translationX = 0f
                                            }
                                            TitleAnimStyle.DECODING -> { alpha = 1f; translationX = 0f; rotationY = 0f }
                                            TitleAnimStyle.GLITCH -> {
                                                alpha = titleGlitchAlpha
                                                translationX = titleGlitchTranslationX
                                                rotationY = 0f
                                            }
                                        }
                                    } else {
                                        alpha = 1f; translationX = 0f; rotationY = 0f
                                    }
                                }
                            )

                            if (metadataAlpha > 0f) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { alpha = metadataAlpha }
                                ) {
                                    Spacer(modifier = Modifier.height(0.dp))
                                    when (currentSubtitleStyle) {
                                        SubtitleAnimStyle.NONE -> {
                                            Column {
                                                Text(
                                                    text = strings.homeScreen.welcomeBack.replace("!", ""),
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Light,
                                                    color = TextSecondary
                                                )
                                                if (cleanSubtitle.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = cleanSubtitle,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = TextSecondary.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                        SubtitleAnimStyle.BLUR_FADE -> {
                                            Column(
                                                modifier = Modifier.graphicsLayer {
                                                    val progress = headerProgress.value
                                                    alpha = progress
                                                    val blurRadius = 15f * (1f - progress)
                                                    if (blurRadius > 0.1f) {
                                                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                                            blurRadius,
                                                            blurRadius,
                                                            android.graphics.Shader.TileMode.CLAMP
                                                        ).asComposeRenderEffect()
                                                    } else {
                                                        renderEffect = null
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = strings.homeScreen.welcomeBack.replace("!", ""),
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Light,
                                                    color = TextSecondary
                                                )
                                                if (cleanSubtitle.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        text = cleanSubtitle,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = TextSecondary.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                        SubtitleAnimStyle.WORD_BY_WORD -> {
                                            Column {
                                                WordByWordText(
                                                    text = strings.homeScreen.welcomeBack.replace("!", ""),
                                                    animProgress = { headerProgress.value },
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Light,
                                                    color = TextSecondary
                                                )
                                                if (cleanSubtitle.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    WordByWordText(
                                                        text = cleanSubtitle,
                                                        animProgress = { headerProgress.value },
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = TextSecondary.copy(alpha = 0.5f)
                                                    )
                                                }
                                            }
                                        }
                                        SubtitleAnimStyle.TYPEWRITER -> {
                                            val fullGreeting = strings.homeScreen.welcomeBack.replace("!", "")
                                            Column {
                                                Box(contentAlignment = Alignment.TopStart) {
                                                    Text(
                                                        text = fullGreeting,
                                                        fontSize = 28.sp,
                                                        fontWeight = FontWeight.Light,
                                                        color = Color.Transparent,
                                                        modifier = Modifier.alpha(0f)
                                                    )
                                                    Text(
                                                        text = if (cursorPosition == 1 && cursorBlink) typedGreeting + "▎" else typedGreeting,
                                                        fontSize = 28.sp,
                                                        fontWeight = FontWeight.Light,
                                                        color = TextSecondary
                                                    )
                                                }
                                                if (cleanSubtitle.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Box(contentAlignment = Alignment.TopStart) {
                                                        Text(
                                                            text = cleanSubtitle,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = Color.Transparent,
                                                            modifier = Modifier.alpha(0f)
                                                        )
                                                        Text(
                                                            text = if (cursorPosition == 2 && cursorBlink) typedSubtitle + "▎" else typedSubtitle,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            color = TextSecondary.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.homeScreen.editWorkspace,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { showRestorationSheet = true },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = SurfaceDark),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = strings.homeScreen.addWidgets,
                                        tint = ElectricViolet
                                    )
                                }
                                Button(
                                    onClick = { viewModel.exitEditMode() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = strings.libraryScreen.done,
                                        color = BackgroundDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                val deleteAlpha by animateFloatAsState(
                    targetValue = if (isOverDeleteZone) 1f else 0f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "deleteAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = deleteAlpha },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = strings.homeScreen.releaseToDelete,
                            tint = NeonCoral,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.homeScreen.releaseToDelete,
                            color = NeonCoral,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }    // Restoration Bottom Sheet
        if (showRestorationSheet) {
            val sheetLazyListState = rememberLazyListState()
            val sheetScope = rememberCoroutineScope()
            var sheetGestureStartedAtTop by remember { mutableStateOf(true) }
            
            LaunchedEffect(sheetLazyListState.isScrollInProgress) {
                if (sheetLazyListState.isScrollInProgress) {
                    sheetGestureStartedAtTop = sheetLazyListState.firstVisibleItemIndex == 0 &&
                            sheetLazyListState.firstVisibleItemScrollOffset == 0
                }
            }
            
            val sheetScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        return Velocity.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        var consumedY = 0f
                        if (available.y < 0f) {
                            consumedY = available.y
                            if (source == NestedScrollSource.SideEffect) {
                                sheetScope.launch {
                                    try {
                                        sheetLazyListState.scrollToItem(
                                            sheetLazyListState.firstVisibleItemIndex,
                                            sheetLazyListState.firstVisibleItemScrollOffset
                                        )
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                            }
                        } else if (available.y > 0f && !sheetGestureStartedAtTop) {
                            consumedY = available.y
                            if (source == NestedScrollSource.SideEffect) {
                                sheetScope.launch {
                                    try {
                                        sheetLazyListState.scrollToItem(
                                            sheetLazyListState.firstVisibleItemIndex,
                                            sheetLazyListState.firstVisibleItemScrollOffset
                                        )
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                            }
                        }
                        return Offset(x = 0f, y = consumedY)
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        var consumedY = 0f
                        if (available.y < 0f) {
                            consumedY = available.y
                        } else if (available.y > 0f && !sheetGestureStartedAtTop) {
                            consumedY = available.y
                        }
                        return Velocity(x = 0f, y = consumedY)
                    }
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showRestorationSheet = false },
                containerColor = BackgroundDark,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0.dp) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp)
                        .nestedScroll(sheetScrollConnection)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.homeScreen.addWidgets,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { showRestorationSheet = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.libraryScreen.done,
                                tint = TextSecondary
                            )
                        }
                    }
                    
                    HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    
                    val hiddenWidgets = viewModel.editableWidgetConfigs.filter { !it.isVisible }
                    
                    if (hiddenWidgets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.homeScreen.noHiddenWidgets,
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = sheetLazyListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(hiddenWidgets) { widgetConfig ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .clickable {
                                            viewModel.restoreWidget(widgetConfig.widgetId)
                                            showRestorationSheet = false
                                        }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = getWidgetName(widgetConfig.widgetId, strings),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = strings.homeScreen.addWidgets,
                                        tint = ElectricViolet
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        
        // Root-level overlay for dragged card or dismissing card
        val successState = uiState as? HomeUiState.Success
        if (successState != null) {
            val activeDraggedId = dragGridState.draggedWidgetId
            val activeDismissingId = dismissingWidgetId
            
            if (activeDraggedId != null && rootBoxCoordinates != null && rootBoxCoordinates!!.isAttached) {
                val currentOffsetInRoot = rootBoxCoordinates!!.windowToLocal(dragGridState.initialItemWindowPos + dragGridState.dragOffset)
                    val widthDp = with(LocalDensity.current) {
                        dragGridState.draggedWidth.toDp()
                    }
                    val heightDp = with(LocalDensity.current) {
                        dragGridState.draggedHeight.toDp()
                    }
                    val currentDragAlpha = if (isOverDeleteZone) 0.5f else 0.85f
                    
                    Box(
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(currentOffsetInRoot.x.roundToInt(), currentOffsetInRoot.y.roundToInt()) }
                            .width(widthDp)
                            .height(heightDp)
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides LocalContentColor.current.copy(alpha = currentDragAlpha)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        alpha = currentDragAlpha
                                        shadowElevation = 12.dp.toPx()
                                        clip = true
                                        shape = BentoCardShape
                                    }
                            ) {
                                HomeScreenWidget(
                                    widgetId = activeDraggedId,
                                    state = successState,
                                    onGenreClick = onGenreClick,
                                    onStudioClick = onStudioClick,
                                    onCollectionClick = onCollectionClick,
                                    onStatusClick = onStatusClick,
                                    onLongClick = null,
                                    isEditMode = isEditMode
                                )
                            }
                        }
                    }
            } else if (placingWidgetId != null && rootBoxCoordinates != null && rootBoxCoordinates!!.isAttached) {
                val targetCoords = widgetCoordinates[placingWidgetId]
                val targetRootPos = if (targetCoords != null && targetCoords.isAttached) {
                    rootBoxCoordinates!!.localPositionOf(targetCoords, Offset.Zero)
                } else {
                    placingStartOffset
                }
                
                val progress = placingProgress.value
                val currentOffsetInRoot = Offset(
                    x = placingStartOffset.x + (targetRootPos.x - placingStartOffset.x) * progress,
                    y = placingStartOffset.y + (targetRootPos.y - placingStartOffset.y) * progress
                )
                
                val widthDp = with(LocalDensity.current) { placingWidth.toDp() }
                val heightDp = with(LocalDensity.current) { placingHeight.toDp() }
                
                val currentScale = 1.05f - 0.05f * progress
                val currentAlpha = 0.85f + 0.15f * progress
                val currentShadowElevation = with(LocalDensity.current) {
                    (12.dp.toPx() * (1f - progress))
                }
                
                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(currentOffsetInRoot.x.roundToInt(), currentOffsetInRoot.y.roundToInt()) }
                        .width(widthDp)
                        .height(heightDp)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides LocalContentColor.current.copy(alpha = currentAlpha)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = currentScale
                                    scaleY = currentScale
                                    alpha = currentAlpha
                                    shadowElevation = currentShadowElevation
                                    clip = true
                                    shape = BentoCardShape
                                }
                        ) {
                            HomeScreenWidget(
                                widgetId = placingWidgetId!!,
                                state = successState,
                                onGenreClick = onGenreClick,
                                onStudioClick = onStudioClick,
                                onCollectionClick = onCollectionClick,
                                onStatusClick = onStatusClick,
                                onLongClick = null,
                                isEditMode = isEditMode
                            )
                        }
                    }
                }
            } else if (activeDismissingId != null) {
                val widthDp = with(LocalDensity.current) {
                    dismissingWidth.toDp()
                }
                val heightDp = with(LocalDensity.current) {
                    dismissingHeight.toDp()
                }
                val currentOffsetInRoot = dismissingOffset
                val progress = dismissProgress.value
                val currentScale = 1.05f * progress
                val currentAlpha = 0.85f * progress
                
                Box(
                    modifier = Modifier
                        .offset { androidx.compose.ui.unit.IntOffset(currentOffsetInRoot.x.roundToInt(), currentOffsetInRoot.y.roundToInt()) }
                        .width(widthDp)
                        .height(heightDp)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides LocalContentColor.current.copy(alpha = currentAlpha)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = currentScale
                                    scaleY = currentScale
                                    alpha = currentAlpha
                                    shadowElevation = 12.dp.toPx()
                                    clip = true
                                    shape = BentoCardShape
                                }
                        ) {
                            HomeScreenWidget(
                                widgetId = activeDismissingId,
                                state = successState,
                                onGenreClick = onGenreClick,
                                onStudioClick = onStudioClick,
                                onCollectionClick = onCollectionClick,
                                onStatusClick = onStatusClick,
                                onLongClick = null,
                                isEditMode = isEditMode
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreenWidget(
    widgetId: String,
    state: HomeUiState.Success,
    onGenreClick: (String) -> Unit,
    onStudioClick: (Long) -> Unit,
    onCollectionClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    onLongClick: (() -> Unit)?,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    when (widgetId) {
        "watch_time" -> {
            WatchTimeWidget(
                totalMinutes = state.userStats.totalWatchTimeMinutes,
                onLongClick = onLongClick,
                isEditMode = isEditMode,
                modifier = modifier
            )
        }
        "watch_status_chart" -> {
            BentoWatchStatusPieChart(
                stats = state.trackingStats,
                onStatusClick = onStatusClick,
                onLongClick = onLongClick,
                isEditMode = isEditMode,
                modifier = modifier
            )
        }
        "chaos_meter" -> {
            ChaosMeterWidget(
                count = state.userStats.chaosMeterCount,
                onLongClick = onLongClick,
                isEditMode = isEditMode,
                modifier = modifier
            )
        }
        "personal_collections" -> {
            CollectionsBridgeWidget(
                activeCollections = state.bentoStats.activeCollectionsCount,
                covers = state.bentoStats.collectionCovers,
                onClick = onCollectionClick,
                onLongClick = onLongClick,
                isEditMode = isEditMode,
                modifier = modifier
            )
        }
        "top_studios" -> {
            TopStudiosWidget(
                studios = state.bentoStats.studioDistributions,
                onStudioClick = onStudioClick,
                onLongClick = onLongClick,
                isEditMode = isEditMode,
                modifier = modifier
            )
        }
        "top_genres" -> {
            TopGenresWidget(
                genres = state.bentoStats.genreDistributions,
                onGenreClick = onGenreClick,
                onLongClick = onLongClick,
                isEditMode = isEditMode,
                modifier = modifier
            )
        }
        "franchise_giant" -> {
            FranchiseGiantWidget(
                info = state.bentoStats.franchiseGiant,
                onLongClick = onLongClick,
                isEditMode = isEditMode,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenGrid(
    state: HomeUiState.Success,
    viewModel: HomeViewModel,
    isEditMode: Boolean,
    dragGridState: DragGridState,
    topBarBottomInWindow: Float,
    dismissingWidgetId: String?,
    placingWidgetId: String?,
    widgetCoordinates: MutableMap<String, LayoutCoordinates>,
    lazyGridState: LazyGridState,
    expandedHeaderHeightPx: Float,
    onAnimeClick: (Long) -> Unit,
    onGenreClick: (String) -> Unit,
    onStudioClick: (Long) -> Unit,
    onCollectionClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    onAddWidgetsClick: () -> Unit,
    onDragRelease: (String, Offset, Offset, Float, Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val hapticFeedback = LocalHapticFeedback.current

    val visibleConfigs = if (isEditMode) {
        viewModel.editableWidgetConfigs.filter { it.isVisible }
    } else {
        state.widgetConfigs.filter { it.isVisible }
    }

    var gridScrollEnabled by remember { mutableStateOf(true) }

    val density = LocalDensity.current
    val topSpacerHeight = remember(isEditMode, expandedHeaderHeightPx) {
        derivedStateOf {
            if (isEditMode) {
                64.dp
            } else if (expandedHeaderHeightPx == 0f) {
                220.dp
            } else {
                with(density) { expandedHeaderHeightPx.toDp() }
            }
        }
    }

    LaunchedEffect(dragGridState.draggedWidgetId) {
        if (dragGridState.draggedWidgetId != null) {
            while (true) {
                val gridCoords = dragGridState.gridCoordinates
                if (gridCoords != null && gridCoords.isAttached) {
                    val viewportHeight = gridCoords.size.height
                    val pointerInGrid = gridCoords.windowToLocal(dragGridState.dragPointerWindow)
                    val pointerY = pointerInGrid.y
                    
                    val threshold = 150f // pixels
                    val maxScrollSpeed = 32f // pixels per frame (increased from 20f)
                    
                    var scrollAmount = 0f
                    if (pointerY < threshold && pointerY > 0) {
                        val ratio = (threshold - pointerY) / threshold
                        scrollAmount = -maxScrollSpeed * ratio
                    } else if (pointerY > viewportHeight - threshold && pointerY < viewportHeight) {
                        val ratio = (pointerY - (viewportHeight - threshold)) / threshold
                        scrollAmount = maxScrollSpeed * ratio
                    }
                    
                    if (scrollAmount != 0f) {
                        lazyGridState.scrollBy(scrollAmount)
                    }
                }
                delay(16L)
            }
        }
    }

    LaunchedEffect(dragGridState.draggedWidgetId) {
        if (dragGridState.draggedWidgetId != null) {
            snapshotFlow { lazyGridState.layoutInfo }
                .collect {
                    dragGridState.onDrag(Offset.Zero)
                }
        }
    }

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(2),
        modifier = modifier
            .onGloballyPositioned { coords ->
                dragGridState.gridCoordinates = coords
            }
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val visibleItems = lazyGridState.layoutInfo.visibleItemsInfo
                                val clickedItem = visibleItems.firstOrNull { item ->
                                    val rect = Rect(
                                        item.offset.x.toFloat(),
                                        item.offset.y.toFloat(),
                                        (item.offset.x + item.size.width).toFloat(),
                                        (item.offset.y + item.size.height).toFloat()
                                    )
                                    rect.contains(offset) && item.key != "header_spacer" && item.key != "spotlight_header" && item.key != "spotlight_card" && item.key != "spotlight_spacer" && item.key != "workspace_header" && item.key != "empty_workspace_placeholder"
                                }
                                if (clickedItem != null) {
                                    val widgetId = clickedItem.key as? String
                                    if (widgetId != null) {
                                        gridScrollEnabled = false
                                        val gridCoords = dragGridState.gridCoordinates
                                        if (gridCoords != null && gridCoords.isAttached) {
                                            val gridWindowPos = gridCoords.localToWindow(Offset.Zero)
                                            val initialItemWindowPos = gridWindowPos + Offset(clickedItem.offset.x.toFloat(), clickedItem.offset.y.toFloat())
                                            val initialPointerWindowPos = gridWindowPos + offset
                                            dragGridState.onDragStart(
                                                widgetId = widgetId,
                                                initialItemWindowPos = initialItemWindowPos,
                                                initialPointerWindowPos = initialPointerWindowPos,
                                                startIndex = clickedItem.index
                                            )
                                        }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragGridState.onDrag(dragAmount)
                            },
                            onDragEnd = {
                                gridScrollEnabled = true
                                val draggedId = dragGridState.draggedWidgetId
                                if (draggedId != null) {
                                    val finalIsOverDeleteZone = dragGridState.dragPointerWindow.y <= topBarBottomInWindow
                                    onDragRelease(
                                        draggedId,
                                        dragGridState.initialItemWindowPos,
                                        dragGridState.dragOffset,
                                        dragGridState.draggedWidth,
                                        dragGridState.draggedHeight,
                                        finalIsOverDeleteZone
                                    )
                                    dragGridState.onDragEnd(finalIsOverDeleteZone)
                                }
                            },
                            onDragCancel = {
                                gridScrollEnabled = true
                                val draggedId = dragGridState.draggedWidgetId
                                if (draggedId != null) {
                                    onDragRelease(
                                        draggedId,
                                        dragGridState.initialItemWindowPos,
                                        dragGridState.dragOffset,
                                        dragGridState.draggedWidth,
                                        dragGridState.draggedHeight,
                                        false
                                    )
                                    dragGridState.onDragEnd(false)
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        userScrollEnabled = gridScrollEnabled,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(2) }, key = "header_spacer") {
            Column {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(topSpacerHeight.value))
            }
        }
        // Spotlight Section (only visible when NOT in Edit Mode)
        if (!isEditMode && state.featuredAnime != null) {
            item(span = { GridItemSpan(2) }, key = "spotlight_header") {
                Text(
                    text = strings.homeScreen.spotlight,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item(span = { GridItemSpan(2) }, key = "spotlight_card") {
                HomeFeaturedCard(
                    anime = state.featuredAnime,
                    preferUk = state.preferUk,
                    onClick = { onAnimeClick(state.featuredAnime.anilistId) }
                )
            }
            item(span = { GridItemSpan(2) }, key = "spotlight_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Section Title: Quick Stats (only visible when NOT in edit mode)
        if (!isEditMode && visibleConfigs.isNotEmpty()) {
            item(span = { GridItemSpan(2) }, key = "workspace_header") {
                Text(
                    text = strings.homeScreen.trackingProgress,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (visibleConfigs.isEmpty()) {
            item(span = { GridItemSpan(2) }, key = "empty_workspace_placeholder") {
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.2f),
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.homeScreen.emptyWorkspace,
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onAddWidgetsClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = BackgroundDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.homeScreen.addWidgets,
                                color = BackgroundDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        } else {
            // The dynamic Bento widgets
            itemsIndexed(
                items = visibleConfigs,
                key = { _, config -> config.widgetId },
                span = { _, config ->
                    GridItemSpan(
                        when (config.widgetId) {
                            "chaos_meter", "personal_collections" -> 1
                            else -> 2
                        }
                    )
                }
            ) { index, config ->
                val isDragged = dragGridState.draggedWidgetId == config.widgetId
                val isDismissing = dismissingWidgetId == config.widgetId
                val isPlacing = placingWidgetId == config.widgetId
                
                val scale = 1.0f
                val alpha = if (isDragged || isDismissing || isPlacing) 0f else 1.0f

                val tapModifier = if (!isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                viewModel.enterEditMode()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = tween(350, easing = FastOutSlowInEasing),
                            fadeOutSpec = tween(350, easing = FastOutSlowInEasing),
                            placementSpec = tween(350, easing = FastOutSlowInEasing)
                        )
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                widgetCoordinates[config.widgetId] = coords
                            }
                        }
                        .graphicsLayer {
                            this.translationX = 0f
                            this.translationY = 0f
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                            this.shadowElevation = 0f
                            this.clip = true
                            this.shape = BentoCardShape
                        }
                        .zIndex(if (isDragged) 10f else 1f)
                        .jiggle(isEditMode && !isDragged)
                        .then(tapModifier)
                ) {
                    HomeScreenWidget(
                        widgetId = config.widgetId,
                        state = state,
                        onGenreClick = onGenreClick,
                        onStudioClick = onStudioClick,
                        onCollectionClick = onCollectionClick,
                        onStatusClick = onStatusClick,
                        onLongClick = if (isEditMode) null else { { viewModel.enterEditMode() } },
                        isEditMode = isEditMode
                    )
                }
            }
        }
    }
}

@Composable
fun HomeFeaturedCard(
    anime: Anime,
    preferUk: Boolean,
    onClick: () -> Unit
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(anime.bannerImage ?: anime.coverLarge)
                .precision(coil.size.Precision.EXACT)
                .allowHardware(true)
                .crossfade(true)
                .build(),
            contentDescription = anime.titleRomaji,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x99000000), Color(0xEE000000))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = NeonCoral,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${strings.misc.featured}  •  ${anime.scoreMal ?: 8.5}",
                    color = NeonCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = anime.getDisplayTitle(preferUk = preferUk),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = anime.descriptionUk ?: anime.descriptionEn ?: "",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordByWordText(
    text: String,
    animProgress: () -> Float,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified
) {
    val words = remember(text) { text.split(" ") }
    val wordCount = words.size

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        words.forEachIndexed { index, word ->
            val wordAlpha = remember(index, wordCount) {
                derivedStateOf {
                    val progress = animProgress()
                    val start = index.toFloat() / wordCount
                    val end = (index + 1).toFloat() / wordCount
                    ((progress - start) / (end - start)).coerceIn(0f, 1f)
                }
            }
            Text(
                text = word,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha.value
                }
            )
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is android.app.Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
