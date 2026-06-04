package moe.GetTheNya.AniForge

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.sync.DatabaseManager
import moe.GetTheNya.AniForge.ui.anime.AnimeScreen
import moe.GetTheNya.AniForge.ui.dashboard.DashboardScreen
import moe.GetTheNya.AniForge.ui.dashboard.DashboardViewModel
import moe.GetTheNya.AniForge.ui.detail.DetailScreen
import moe.GetTheNya.AniForge.ui.detail.DetailViewModel
import moe.GetTheNya.AniForge.ui.home.HomeScreen
import moe.GetTheNya.AniForge.ui.home.HomeViewModel
import moe.GetTheNya.AniForge.ui.navigation.rememberNavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.profile.LogViewerScreen
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.spring
import moe.GetTheNya.AniForge.ui.navigation.BackStackEntry
import moe.GetTheNya.AniForge.ui.profile.ProfileScreen
import moe.GetTheNya.AniForge.ui.profile.ProfileViewModel
import moe.GetTheNya.AniForge.ui.theme.*
import javax.inject.Inject

enum class TabScreen {
    Home,
    Anime,
    Seasons,
    Profile
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var databaseManager: DatabaseManager

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var localizationService: moe.GetTheNya.AniForge.ui.localization.LocalizationService

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val pagerState = rememberPagerState(initialPage = 0) { TabScreen.entries.size }
            val coroutineScope = rememberCoroutineScope()
            val context = androidx.compose.ui.platform.LocalContext.current
            val localeStrings by localizationService.activeLocaleStrings.collectAsState()

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
                                    2 -> TabScreen.Seasons
                                    3 -> TabScreen.Profile
                                    else -> TabScreen.Seasons
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
                                                val topOffset = if (topEntry.isDragging) {
                                                    topEntry.dragOffset
                                                } else if (topEntry.screen != Screen.Tabs && !topEntry.isEntranceStarted) {
                                                    screenWidthPx
                                                } else {
                                                    topEntry.animatableOffset.value
                                                }
                                                val progress = (topOffset / screenWidthPx).coerceIn(0f, 1f)
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
                                                        1 -> AnimeScreen()
                                                        2 -> DashboardScreen(
                                                            viewModel = dashboardViewModel,
                                                            preferUk = preferUk,
                                                            onAnimeClick = { id -> navController.navigate(Screen.Detail(id)) }
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
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(tab.ordinal)
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
                                                    if (isTopMost) {
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
                                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                                        val dragEvent = event.changes.firstOrNull { it.id == dragId }
                                                                        if (dragEvent == null || !dragEvent.pressed) {
                                                                            break
                                                                        }
                                                                        
                                                                        val position = dragEvent.position
                                                                        val prevPosition = dragEvent.previousPosition
                                                                        val deltaX = position.x - prevPosition.x
                                                                        val deltaY = position.y - prevPosition.y
                                                                        
                                                                        if (!hasLocked) {
                                                                            dragX += deltaX
                                                                            dragY += kotlin.math.abs(deltaY)
                                                                            if (dragX > touchSlop && dragX > dragY * 1.5f) {
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
                                                            anilistId = screen.animeId,
                                                            navController = navController,
                                                            viewModel = scopedViewModel,
                                                            preferUk = preferUk,
                                                            modifier = Modifier.padding(innerPadding),
                                                            onBack = { triggerDismissAnimation(entry) }
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
            Color(0xFFA855F7), // Seasons (Index 2)
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
                TabScreen.Anime to (Icons.Default.PlayArrow to strings.animeScreen.name),
                TabScreen.Seasons to (Icons.Default.Search to strings.dashboardScreen.name),
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