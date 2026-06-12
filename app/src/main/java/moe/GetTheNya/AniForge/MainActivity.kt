package moe.GetTheNya.AniForge

import android.os.Bundle
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.os.Build
import moe.GetTheNya.AniForge.ui.settings.DevSettingsScreen
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.sync.DatabaseManager
import moe.GetTheNya.AniForge.ui.franchises.LibraryScreen
import moe.GetTheNya.AniForge.ui.franchises.LibraryViewModel
import moe.GetTheNya.AniForge.ui.franchises.FranchiseTreeScreen
import moe.GetTheNya.AniForge.ui.franchises.FranchiseTreeViewModel
import moe.GetTheNya.AniForge.ui.dashboard.DashboardScreen
import moe.GetTheNya.AniForge.ui.dashboard.DashboardViewModel
import moe.GetTheNya.AniForge.ui.detail.DetailScreen
import moe.GetTheNya.AniForge.ui.detail.DetailViewModel
import moe.GetTheNya.AniForge.ui.detail.ImageViewerScreen
import moe.GetTheNya.AniForge.ui.home.HomeScreen
import moe.GetTheNya.AniForge.ui.home.HomeViewModel
import moe.GetTheNya.AniForge.ui.navigation.BackStackEntry
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.navigation.rememberNavController
import moe.GetTheNya.AniForge.ui.profile.LogViewerScreen
import moe.GetTheNya.AniForge.ui.profile.ProfileScreen
import moe.GetTheNya.AniForge.ui.profile.ProfileViewModel
import moe.GetTheNya.AniForge.ui.profile.TrackedListScreen
import moe.GetTheNya.AniForge.ui.profile.TrackedListViewModel
import moe.GetTheNya.AniForge.ui.theme.AniForgeTheme
import moe.GetTheNya.AniForge.ui.theme.CardBorder
import javax.inject.Inject

enum class TabScreen {
    Home,
    Anime,
    Library,
    Profile
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var sensorListener: SensorEventListener? = null
    private var onShake: (() -> Unit)? = null

    private var shakeCount = 0
    private var firstShakeTime = 0L
    private var lastShakeTime = 0L

    @Inject
    lateinit var databaseManager: DatabaseManager

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var localizationService: moe.GetTheNya.AniForge.ui.localization.LocalizationService

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                val gForce = kotlin.math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                if (gForce > 2.5f) {
                    val now = System.currentTimeMillis()
                    // Ignore micro-vibrations in the same physical stroke
                    if (now - lastShakeTime < 100) return

                    // If the window expired, treat this as a brand new shake sequence
                    if (now - firstShakeTime > 1200) {
                        shakeCount = 1
                        firstShakeTime = now
                    } else {
                        shakeCount++
                    }
                    lastShakeTime = now

                    // Require 4 distinct back-and-forth strokes to confirm a deliberate shake
                    if (shakeCount >= 4) {
                        shakeCount = 0
                        firstShakeTime = 0L
                        onShake?.invoke()
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val pagerState = rememberPagerState(initialPage = 0) { TabScreen.entries.size }
            val coroutineScope = rememberCoroutineScope()
            
            navController.onSelectTab = { tab ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(tab.ordinal)
                }
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            val localeStrings by localizationService.activeLocaleStrings.collectAsState()

            LaunchedEffect(navController) {
                onShake = {
                    val topScreen = navController.backStack.lastOrNull()?.screen
                    if (topScreen != Screen.DevSettings) {
                        try {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                vibratorManager.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(150L)
                            }
                        } catch (e: Exception) {
                            // Suppress vibration exceptions in case device has no hardware support or permissions
                        }
                        navController.navigate(Screen.DevSettings)
                    }
                }
            }

            // Check for catalog database updates asynchronously on application startup
            LaunchedEffect(Unit) {
                databaseManager.updateCatalogIfAvailable()
            }

            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }

            // Dynamic Relative Parallax triggerDismissAnimation
            val triggerDismissAnimation: (BackStackEntry) -> Unit = { entry ->
                if (navController.backStack.contains(entry) && entry.animatableOffset.value < screenWidthPx) {
                    coroutineScope.launch {
                        entry.isDragging = false
                        entry.animatableOffset.snapTo(0f)
                        entry.animatableOffset.animateTo(
                            targetValue = screenWidthPx,
                            animationSpec = tween(durationMillis = 300)
                        )
                        navController.popBackStack()
                    }
                }
            }

            // Zero-Overhead Double-Tap Exit BackHandler
            var lastBackPressTime by remember { mutableLongStateOf(0L) }
            BackHandler(enabled = true) {
                val topEntry = navController.backStack.lastOrNull()
                if (topEntry != null && topEntry.screen != Screen.Tabs) {
                    triggerDismissAnimation(topEntry)
                } else if (pagerState.currentPage != 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime > 2000) {
                        Toast.makeText(context, localeStrings.misc.exitToast, Toast.LENGTH_SHORT).show()
                        lastBackPressTime = currentTime
                    } else {
                        finish()
                    }
                }
            }

            CompositionLocalProvider(
                moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings provides localeStrings
            ) {
                AniForgeTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main tabs layout (always kept alive in background)
                        val density = LocalDensity.current
                        val preferUk by settingsProvider.preferUkTitles.collectAsState()

                        // Scroll-to-hide gesture logic using NestedScroll
                        var isBottomBarVisible by remember { mutableStateOf(true) }
                        
                        val nestedScrollConnection = remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    val delta = available.y
                                    if (delta < -15f) {
                                        isBottomBarVisible = false
                                    } else if (delta > 15f) {
                                        isBottomBarVisible = true
                                    }
                                    return Offset.Zero
                                }
                            }
                        }

                        // Reset bottom bar visibility when switching tabs
                        LaunchedEffect(pagerState.currentPage) {
                            isBottomBarVisible = true
                        }

                        val contentLayer = rememberGraphicsLayer()
                        val pagerWindowPosition = remember { mutableStateOf(Offset.Zero) }
                        val bottomBarWindowPosition = remember { mutableStateOf(Offset.Zero) }
                        val tabsScaleState = remember {
                            derivedStateOf {
                                val lastIndex = navController.backStack.lastIndex
                                if (lastIndex == 0) {
                                    1.0f
                                } else if (lastIndex == 1) {
                                    val topEntry = navController.backStack[1]
                                    val topOffset = if (topEntry.isDragging) {
                                        topEntry.dragOffset
                                    } else if (topEntry.screen != Screen.Tabs && !topEntry.isEntranceStarted) {
                                        screenWidthPx
                                    } else {
                                        topEntry.animatableOffset.value
                                    }
                                    val progress = (topOffset / screenWidthPx).coerceIn(0f, 1f)
                                    0.95f + 0.05f * progress
                                } else {
                                    0.95f
                                }
                            }
                        }

                        // Optimized selection state to avoid root recomposition
                        val selectedTabState = remember {
                            derivedStateOf {
                                when (pagerState.currentPage) {
                                    0 -> TabScreen.Home
                                    1 -> TabScreen.Anime
                                    2 -> TabScreen.Library
                                    3 -> TabScreen.Profile
                                    else -> TabScreen.Home
                                }
                            }
                        }

                        val bottomBarOffset by animateDpAsState(
                            targetValue = if (isBottomBarVisible) 0.dp else 120.dp,
                            animationSpec = tween(durationMillis = 300),
                            label = "bottomBarOffset"
                        )

                        navController.backStack.forEachIndexed { index, entry ->
                            key(entry.id) {
                                val isTopMost = index == navController.backStack.lastIndex
                                
                                LaunchedEffect(entry.id) {
                                    if (entry.screen != Screen.Tabs && !entry.isEntranceStarted) {
                                        entry.animatableOffset.snapTo(screenWidthPx)
                                        entry.isEntranceStarted = true
                                        entry.animatableOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val exitMax = navController.rouletteExitMaxCount
                                            if (exitMax != null) {
                                                var firstDetailIndex = -1
                                                for (i in navController.backStack.indices) {
                                                    if (navController.backStack[i].screen is Screen.Detail) {
                                                        firstDetailIndex = i
                                                        break
                                                    }
                                                }
                                                if (firstDetailIndex != -1 && index >= firstDetailIndex - 1) {
                                                    scaleX = 1.0f
                                                    scaleY = 1.0f
                                                    alpha = 1.0f
                                                    translationX = 0f
                                                } else {
                                                    scaleX = 0.95f
                                                    scaleY = 0.95f
                                                    alpha = 0.0f
                                                    translationX = 0f
                                                }
                                            } else {
                                                val lastIndex = navController.backStack.lastIndex
                                                if (isTopMost) {
                                                    val offsetVal = if (entry.isDragging) {
                                                        entry.dragOffset
                                                    } else if (entry.screen != Screen.Tabs && !entry.isEntranceStarted) {
                                                        screenWidthPx
                                                    } else {
                                                        entry.animatableOffset.value
                                                    }
                                                    scaleX = 1.0f
                                                    scaleY = 1.0f
                                                    alpha = 1.0f
                                                    translationX = offsetVal
                                                } else if (index == lastIndex - 1) {
                                                    val topEntry = navController.backStack[lastIndex]
                                                    val progress = if (topEntry.screen is Screen.ImageViewer) {
                                                        if (topEntry.animatableOffset.value > 0f || !topEntry.isEntranceStarted) {
                                                            val topOffset = if (!topEntry.isEntranceStarted) {
                                                                screenWidthPx
                                                            } else {
                                                                topEntry.animatableOffset.value
                                                            }
                                                            (topOffset / screenWidthPx).coerceIn(0f, 1f)
                                                        } else {
                                                            topEntry.swipeDismissProgress
                                                        }
                                                    } else {
                                                        val topOffset = if (topEntry.isDragging) {
                                                            topEntry.dragOffset
                                                        } else if (topEntry.screen != Screen.Tabs && !topEntry.isEntranceStarted) {
                                                            screenWidthPx
                                                        } else {
                                                            topEntry.animatableOffset.value
                                                        }
                                                        (topOffset / screenWidthPx).coerceIn(0f, 1f)
                                                    }
                                                    val s = 0.95f + 0.05f * progress
                                                    val a = 0.6f + 0.4f * progress
                                                    scaleX = s
                                                    scaleY = s
                                                    alpha = a
                                                    translationX = 0f
                                                } else {
                                                    scaleX = 0.95f
                                                    scaleY = 0.95f
                                                    alpha = 0.0f
                                                    translationX = 0f
                                                }
                                            }
                                        }
                                ) {
                                    if (entry.screen == Screen.Tabs) {
                                        // Main tabs layout
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .nestedScroll(nestedScrollConnection)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .onGloballyPositioned { coordinates ->
                                                        pagerWindowPosition.value = coordinates.positionInWindow()
                                                    }
                                                    .drawWithContent {
                                                        contentLayer.record {
                                                            this@drawWithContent.drawContent()
                                                        }
                                                        drawLayer(contentLayer)
                                                    }
                                            ) {
                                                HorizontalPager(
                                                    state = pagerState,
                                                    modifier = Modifier.fillMaxSize()
                                                ) { page ->
                                                    when (page) {
                                                        0 -> HomeScreen(
                                                            viewModel = homeViewModel,
                                                            onAnimeClick = { id -> navController.navigate(Screen.Detail(id)) }
                                                        )
                                                        1 -> DashboardScreen(
                                                            viewModel = dashboardViewModel,
                                                            preferUk = preferUk,
                                                            onAnimeClick = { id -> navController.navigate(Screen.Detail(id)) }
                                                        )
                                                        2 -> LibraryScreen(
                                                            viewModel = libraryViewModel,
                                                            navController = navController,
                                                            preferUk = preferUk
                                                        )
                                                        3 -> ProfileScreen(
                                                            viewModel = profileViewModel,
                                                            navController = navController
                                                        )
                                                    }
                                                }
                                            }

                                            // Floating Bottom Navigation Bar (optimized with selectedTabState lambda)
                                            FloatingBottomNavigation(
                                                selectedTab = { selectedTabState.value },
                                                onTabSelected = { tab ->
                                                    if (tab == TabScreen.Library && pagerState.currentPage == TabScreen.Library.ordinal) {
                                                        navController.onLibraryClick?.invoke()
                                                    } else {
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(tab.ordinal)
                                                        }
                                                    }
                                                },
                                                contentLayer = contentLayer,
                                                pagerWindowPositionState = pagerWindowPosition,
                                                bottomBarWindowPositionState = bottomBarWindowPosition,
                                                currentScale = { tabsScaleState.value },
                                                scrollPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .graphicsLayer {
                                                        translationY = bottomBarOffset.toPx()
                                                    }
                                            )
                                        }
                                    } else {
                                        // Sub-screen overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {}
                                                .then(
                                                    if (isTopMost && entry.screen !is Screen.ImageViewer) {
                                                        Modifier.pointerInput(screenWidthPx) {
                                                            val touchSlop = viewConfiguration.touchSlop
                                                            awaitPointerEventScope {
                                                                while (true) {
                                                                    val down = awaitFirstDown(requireUnconsumed = false)
                                                                    var dragX = 0f
                                                                    var dragY = 0f
                                                                    var hasLocked = false
                                                                    val dragId = down.id
                                                                    
                                                                    while (true) {
                                                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                                                        val dragEvent = event.changes.firstOrNull { it.id == dragId }
                                                                        if (dragEvent == null || !dragEvent.pressed) {
                                                                            break
                                                                        }
                                                                        if (!hasLocked && dragEvent.isConsumed) {
                                                                            break
                                                                        }
                                                                        
                                                                        val position = dragEvent.position
                                                                        val prevPosition = dragEvent.previousPosition
                                                                        val deltaX = position.x - prevPosition.x
                                                                        val deltaY = position.y - prevPosition.y
                                                                        
                                                                        if (!hasLocked) {
                                                                            dragX += deltaX
                                                                            dragY += kotlin.math.abs(deltaY)
                                                                            if (dragX > touchSlop && dragX > dragY * 1.5f && !dragEvent.isConsumed) {
                                                                                hasLocked = true
                                                                                entry.isDragging = true
                                                                                entry.dragOffset = dragX
                                                                                dragEvent.consume()
                                                                            } else if (kotlin.math.abs(dragX) > touchSlop || dragY > touchSlop) {
                                                                                break
                                                                            }
                                                                        } else {
                                                                            dragEvent.consume()
                                                                            entry.dragOffset = (entry.dragOffset + deltaX).coerceAtLeast(0f)
                                                                        }
                                                                    }
                                                                    
                                                                    if (hasLocked) {
                                                                        coroutineScope.launch {
                                                                            val releaseOffset = entry.dragOffset
                                                                            entry.animatableOffset.snapTo(releaseOffset)
                                                                            entry.isDragging = false
                                                                            if (releaseOffset > screenWidthPx * 0.4f) {
                                                                                entry.animatableOffset.animateTo(
                                                                                    screenWidthPx,
                                                                                    tween(durationMillis = 300)
                                                                                )
                                                                                navController.popBackStack()
                                                                            } else {
                                                                                entry.animatableOffset.animateTo(0f, spring())
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else Modifier
                                                )
                                        ) {
                                            CompositionLocalProvider(LocalViewModelStoreOwner provides entry) {
                                                when (val screen = entry.screen) {
                                                    is Screen.Detail -> {
                                                        val scopedViewModel = remember(entry) {
                                                            ViewModelProvider(entry)[DetailViewModel::class.java]
                                                        }
                                                        DetailScreen(
                                                            anilistId = screen.anilistId,
                                                            sourceStatusId = screen.sourceStatusId,
                                                            rouletteCount = screen.rouletteCount,
                                                            visitedIds = screen.visitedIds,
                                                            navController = navController,
                                                            viewModel = scopedViewModel,
                                                            preferUk = preferUk,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) },
                                                            onGenreClick = { genreSlug ->
                                                                 dashboardViewModel.selectGenreOnly(genreSlug)
                                                                 navController.onSelectTab?.invoke(TabScreen.Anime)
                                                                 coroutineScope.launch {
                                                                     val stack = navController.backStack
                                                                     if (stack.size > 2) {
                                                                         val top = stack.last()
                                                                         val toRemove = stack.filter { it != top && it.screen != Screen.Tabs }
                                                                         stack.removeAll(toRemove)
                                                                         toRemove.forEach { it.clear() }
                                                                     }
                                                                     entry.isDragging = false
                                                                     entry.animatableOffset.snapTo(0f)
                                                                     entry.animatableOffset.animateTo(
                                                                         targetValue = screenWidthPx,
                                                                         animationSpec = tween(durationMillis = 300)
                                                                     )
                                                                     navController.popBackStack()
                                                                 }
                                                             },
                                                             onTagClick = { tagId ->
                                                                 dashboardViewModel.selectTagOnly(tagId)
                                                                 navController.onSelectTab?.invoke(TabScreen.Anime)
                                                                 coroutineScope.launch {
                                                                     val stack = navController.backStack
                                                                     if (stack.size > 2) {
                                                                         val top = stack.last()
                                                                         val toRemove = stack.filter { it != top && it.screen != Screen.Tabs }
                                                                         stack.removeAll(toRemove)
                                                                         toRemove.forEach { it.clear() }
                                                                     }
                                                                     entry.isDragging = false
                                                                     entry.animatableOffset.snapTo(0f)
                                                                     entry.animatableOffset.animateTo(
                                                                         targetValue = screenWidthPx,
                                                                         animationSpec = tween(durationMillis = 300)
                                                                     )
                                                                     navController.popBackStack()
                                                                 }
                                                             },
                                                             onStudioClick = { studioId ->
                                                                 dashboardViewModel.selectStudioOnly(studioId)
                                                                 navController.onSelectTab?.invoke(TabScreen.Anime)
                                                                 coroutineScope.launch {
                                                                     val stack = navController.backStack
                                                                     if (stack.size > 2) {
                                                                         val top = stack.last()
                                                                         val toRemove = stack.filter { it != top && it.screen != Screen.Tabs }
                                                                         stack.removeAll(toRemove)
                                                                         toRemove.forEach { it.clear() }
                                                                     }
                                                                     entry.isDragging = false
                                                                     entry.animatableOffset.snapTo(0f)
                                                                     entry.animatableOffset.animateTo(
                                                                         targetValue = screenWidthPx,
                                                                         animationSpec = tween(durationMillis = 300)
                                                                     )
                                                                     navController.popBackStack()
                                                                 }
                                                             },
                                                             onStaffClick = { staffId ->
                                                                 dashboardViewModel.selectStaffOnly(staffId)
                                                                 navController.onSelectTab?.invoke(TabScreen.Anime)
                                                                 coroutineScope.launch {
                                                                     val stack = navController.backStack
                                                                     if (stack.size > 2) {
                                                                         val top = stack.last()
                                                                         val toRemove = stack.filter { it != top && it.screen != Screen.Tabs }
                                                                         stack.removeAll(toRemove)
                                                                         toRemove.forEach { it.clear() }
                                                                     }
                                                                     entry.isDragging = false
                                                                     entry.animatableOffset.snapTo(0f)
                                                                     entry.animatableOffset.animateTo(
                                                                         targetValue = screenWidthPx,
                                                                         animationSpec = tween(durationMillis = 300)
                                                                     )
                                                                     navController.popBackStack()
                                                                 }
                                                            }
                                                        )
                                                    }
                                                    is Screen.LogViewer -> {
                                                        LogViewerScreen(
                                                            viewModel = profileViewModel,
                                                            navController = navController,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
                                                        )
                                                    }
                                                    is Screen.FranchiseTree -> {
                                                         val scopedViewModel = remember(entry) {
                                                             ViewModelProvider(entry)[FranchiseTreeViewModel::class.java].apply {
                                                                 entry.savedStateHandle.keys().forEach { key ->
                                                                     this.savedStateHandle.set(key, entry.savedStateHandle.get<Any>(key))
                                                                 }
                                                                 entry.savedStateHandle = this.savedStateHandle
                                                             }
                                                         }
                                                         FranchiseTreeScreen(
                                                            franchiseId = screen.franchiseId,
                                                            viewModel = scopedViewModel,
                                                            navController = navController,
                                                            preferUk = preferUk,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
                                                        )
                                                    }
                                                    is Screen.CollectionDetail -> {
                                                        val scopedViewModel = remember(entry) {
                                                            ViewModelProvider(entry)[moe.GetTheNya.AniForge.ui.profile.CollectionDetailViewModel::class.java]
                                                        }
                                                        moe.GetTheNya.AniForge.ui.profile.CollectionDetailScreen(
                                                            collectionId = screen.collectionId,
                                                            viewModel = scopedViewModel,
                                                            navController = navController,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
                                                        )
                                                    }
                                                    is Screen.Settings -> {
                                                        val scopedViewModel = remember(entry) {
                                                            ViewModelProvider(entry)[moe.GetTheNya.AniForge.ui.settings.SettingsViewModel::class.java]
                                                        }
                                                        moe.GetTheNya.AniForge.ui.settings.SettingsScreen(
                                                            viewModel = scopedViewModel,
                                                            navController = navController,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
                                                        )
                                                    }
                                                    is Screen.DevSettings -> {
                                                         DevSettingsScreen(
                                                            settingsProvider = settingsProvider,
                                                            navController = navController,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
                                                        )
                                                    }
                                                    is Screen.ImageViewer -> {
                                                        ImageViewerScreen(
                                                            urls = screen.urls,
                                                            initialIndex = screen.initialIndex,
                                                            onBack = { triggerDismissAnimation(entry) },
                                                            onSwipeDismiss = { navController.popBackStack() },
                                                            onSwipeProgressChanged = { progress ->
                                                                entry.swipeDismissProgress = progress
                                                            }
                                                        )
                                                    }
                                                    is Screen.TrackedList -> {
                                                        val scopedViewModel = remember(entry) {
                                                            ViewModelProvider(entry)[TrackedListViewModel::class.java]
                                                        }
                                                        TrackedListScreen(
                                                            initialStatusId = screen.initialStatusId,
                                                            viewModel = scopedViewModel,
                                                            navController = navController,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
                                                        )
                                                    }
                                                    is Screen.Tabs -> {} /*
                                                         val scopedViewModel = remember(entry) {
                                                             ViewModelProvider(entry)[FranchiseTreeViewModel::class.java].apply {
                                                                 entry.savedStateHandle.keys().forEach { key ->
                                                                     this.savedStateHandle.set(key, entry.savedStateHandle.get<Any>(key))
                                                                 }
                                                                 entry.savedStateHandle = this.savedStateHandle
                                                             }
                                                         }
                                                         FranchiseTreeScreen(
                                                            franchiseId = screen.franchiseId,
                                                            viewModel = scopedViewModel,
                                                            navController = navController,
                                                            preferUk = preferUk,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
                                                        )
                                                    }*/
                                                    else -> {}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorListener?.let { listener ->
            accelerometer?.let { accel ->
                sensorManager?.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorListener?.let { listener ->
            sensorManager?.unregisterListener(listener)
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    selectedTab: () -> TabScreen,
    onTabSelected: (TabScreen) -> Unit,
    contentLayer: GraphicsLayer,
    pagerWindowPositionState: State<Offset>,
    bottomBarWindowPositionState: MutableState<Offset>,
    currentScale: () -> Float,
    scrollPosition: () -> Float,
    modifier: Modifier = Modifier
) {
    val blurEffect = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.graphics.RenderEffect.createBlurEffect(
                20f, 20f, android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        } else {
            null
        }
    }

    val tabColors = remember {
        listOf(
            Color(0xFFFF4081), // Home (Index 0)
            Color(0xFF00FFCC), // Anime (Index 1)
            Color(0xFFA855F7), // Franchises (Index 2)
            Color(0xFF00FF87)  // Profile (Index 3)
        )
    }
    val restColor = Color(0x66FFFFFF)

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
    ) {
        // 1. Blurred background Box (draws captured sibling layer translated)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    bottomBarWindowPositionState.value = coordinates.positionInWindow()
                }
                .graphicsLayer {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        renderEffect = blurEffect
                    }
                }
                .drawBehind {
                    val pagerPos = pagerWindowPositionState.value
                    val barPos = bottomBarWindowPositionState.value
                    val s = currentScale().coerceAtLeast(0.01f)

                    val unscaledDeltaX = (barPos.x - pagerPos.x) / s
                    val unscaledDeltaY = (barPos.y - pagerPos.y) / s

                    translate(left = -unscaledDeltaX, top = -unscaledDeltaY) {
                        drawLayer(contentLayer)
                    }
                    drawRect(Color.Black.copy(alpha = 0.50f))
                }
        )

        // 2. Foreground navigation items (perfectly sharp on top)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    // Draw base static layer (opaque white base mask)
                    drawContent()
                    
                    val scrollPos = scrollPosition().coerceIn(0f, 3f)
                    val tabWidth = size.width / 4f
                    val sliderX = scrollPos * tabWidth
                    val w = size.width.coerceAtLeast(1f)
                    
                    val baseIndex = scrollPos.toInt()
                    val fraction = scrollPos - baseIndex
                    
                    val boundaryX = (baseIndex + 1) * tabWidth
                    
                    val f1 = (sliderX / w).coerceIn(0f, 1f)
                    val f2 = (boundaryX / w).coerceIn(0f, 1f)
                    val f3 = ((sliderX + tabWidth) / w).coerceIn(0f, 1f)
                    
                    val activeColor1 = tabColors[baseIndex]
                    val activeColor2 = tabColors[(baseIndex + 1).coerceAtMost(3)]
                    val restColorVal = Color(0x66FFFFFF)
                    
                    // Create color stops with duplicate coordinate stops for razor-sharp borders
                    val colorStops = arrayOf(
                        0.0f to restColorVal,
                        f1 to restColorVal,
                        f1 to activeColor1,
                        f2 to activeColor1,
                        f2 to activeColor2,
                        f3 to activeColor2,
                        f3 to restColorVal,
                        1.0f to restColorVal
                    )
                    
                    val brush = Brush.linearGradient(
                        colorStops = colorStops,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f)
                    )
                    
                    drawRect(
                        brush = brush,
                        blendMode = BlendMode.SrcIn
                    )
                },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
            val tabs = listOf(
                TabScreen.Home to (Icons.Default.Home to strings.homeScreen.name),
                TabScreen.Anime to (Icons.Default.DateRange to strings.dashboardScreen.name),
                TabScreen.Library to (Icons.Default.Layers to strings.libraryScreen.name),
                TabScreen.Profile to (Icons.Default.Person to strings.profileScreen.name)
            )
 
            tabs.forEachIndexed { index, (tab, pair) ->
                val (icon, label) = pair
                val isSelected = selectedTab() == tab
                val itemColor = Color.White
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = itemColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = itemColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}