package moe.GetTheNya.AniForge

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import moe.GetTheNya.AniForge.ui.profile.LogViewerScreen
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

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val detailViewModel: DetailViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentDetailId by remember { mutableStateOf<Long?>(null) }
            var showLogViewer by remember { mutableStateOf(false) }

            // Check for catalog database updates asynchronously on application startup
            LaunchedEffect(Unit) {
                databaseManager.updateCatalogIfAvailable()
            }

            AniForgeTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    if (showLogViewer) {
                        LogViewerScreen(
                            viewModel = profileViewModel,
                            onBackClick = { showLogViewer = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        val detailId = currentDetailId
                        if (detailId != null) {
                            DetailScreen(
                                anilistId = detailId,
                                viewModel = detailViewModel,
                                onBackClick = {
                                    currentDetailId = null
                                },
                                onAnimeClick = { newId ->
                                    currentDetailId = newId
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            val pagerState = rememberPagerState(initialPage = 2) { TabScreen.entries.size }
                            val coroutineScope = rememberCoroutineScope()
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
                                                onAnimeClick = { id -> currentDetailId = id }
                                            )
                                            1 -> AnimeScreen()
                                            2 -> DashboardScreen(
                                                viewModel = dashboardViewModel,
                                                preferUk = preferUk,
                                                onAnimeClick = { id -> currentDetailId = id }
                                            )
                                            3 -> ProfileScreen(
                                                viewModel = profileViewModel,
                                                onViewLogsClick = { showLogViewer = true }
                                            )
                                        }
                                    }
                                }

                                // Floating Bottom Navigation Bar
                                val selectedTab = when (pagerState.currentPage) {
                                    0 -> TabScreen.Home
                                    1 -> TabScreen.Anime
                                    2 -> TabScreen.Seasons
                                    3 -> TabScreen.Profile
                                    else -> TabScreen.Seasons
                                }

                                val bottomBarOffset by animateDpAsState(
                                    targetValue = if (isBottomBarVisible) 0.dp else 120.dp,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "bottomBarOffset"
                                )

                                FloatingBottomNavigation(
                                    selectedTab = selectedTab,
                                    onTabSelected = { tab ->
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(tab.ordinal)
                                        }
                                    },
                                    contentLayer = contentLayer,
                                    pagerWindowPositionState = pagerWindowPosition,
                                    bottomBarWindowPositionState = bottomBarWindowPosition,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset {
                                            IntOffset(0, bottomBarOffset.roundToPx())
                                        }
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
fun FloatingBottomNavigation(
    selectedTab: TabScreen,
    onTabSelected: (TabScreen) -> Unit,
    contentLayer: GraphicsLayer,
    pagerWindowPositionState: State<Offset>,
    bottomBarWindowPositionState: MutableState<Offset>,
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
                    val baseDeltaX = barPos.x - pagerPos.x
                    val baseDeltaY = barPos.y - pagerPos.y

                    translate(left = -baseDeltaX, top = -baseDeltaY) {
                        drawLayer(contentLayer)
                    }
                    drawRect(Color.Black.copy(alpha = 0.50f))
                }
        )

        // 2. Foreground navigation items (perfectly sharp on top)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                TabScreen.Home to (Icons.Default.Home to "Home"),
                TabScreen.Anime to (Icons.Default.PlayArrow to "Anime"),
                TabScreen.Seasons to (Icons.Default.Search to "Seasons"),
                TabScreen.Profile to (Icons.Default.Person to "Profile")
            )

            tabs.forEach { (tab, pair) ->
                val (icon, label) = pair
                val isSelected = selectedTab == tab
                val color = if (isSelected) NeonCoral else TextSecondary
                
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
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}