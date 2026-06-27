package moe.GetTheNya.AniForge.ui.profile

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import moe.GetTheNya.AniForge.ui.dashboard.FilterBottomSheet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.ui.dashboard.AnimeBentoCard
import moe.GetTheNya.AniForge.ui.dashboard.QuickGestureAction
import moe.GetTheNya.AniForge.ui.dashboard.handleQuickGestureAction
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.disableSplitTouch
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.core.model.Anime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackedListScreen(
    initialStatusId: String,
    viewModel: TrackedListViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredAnime by viewModel.filteredAnime.collectAsState()
    val preferUk by viewModel.preferUk.collectAsState()
    val trackingMap by viewModel.trackingMap.collectAsState()
    val trackingEntitiesMap by viewModel.trackingEntitiesMap.collectAsState()
    val listFilterState by viewModel.listFilterState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    val hasActiveFilters = remember(listFilterState) {
        listFilterState.genres.isNotEmpty() ||
        listFilterState.excludedGenres.isNotEmpty() ||
        listFilterState.formats.isNotEmpty() ||
        listFilterState.excludedFormats.isNotEmpty()
    }

    val gestureCenter by viewModel.gestureCenter.collectAsState()
    val gestureUp by viewModel.gestureUp.collectAsState()
    val gestureDown by viewModel.gestureDown.collectAsState()
    val gestureLeft by viewModel.gestureLeft.collectAsState()
    val gestureRight by viewModel.gestureRight.collectAsState()

    var scrollEnabled by remember { mutableStateOf(true) }
    var isSliderActive by remember { mutableStateOf(false) }

    val isTopMost = remember(navController.backStack.lastOrNull()) {
        navController.backStack.lastOrNull()?.screen is Screen.TrackedList
    }

    LaunchedEffect(isTopMost) {
        if (isTopMost) {
            viewModel.refreshSnapshot()
        }
    }

    val initialPage = remember(initialStatusId) {
        val idx = statusConfigs.indexOfFirst { it.id == initialStatusId }
        if (idx != -1) idx else 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage) {
        statusConfigs.size
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in statusConfigs.indices) {
            viewModel.setActiveTab(statusConfigs[pagerState.currentPage].id)
        }
    }

    // Dynamic color interpolation for active tab elements (text, icon, indicator)
    val activeColor = remember(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        val getColor = { idx: Int ->
            if (idx in statusConfigs.indices) statusConfigs[idx].color else Color.Gray
        }
        val baseColor = getColor(pagerState.currentPage)
        val offset = pagerState.currentPageOffsetFraction
        if (offset != 0f) {
            val targetPage = if (offset > 0) pagerState.currentPage + 1 else pagerState.currentPage - 1
            if (targetPage in statusConfigs.indices) {
                val targetColor = getColor(targetPage)
                lerp(baseColor, targetColor, kotlin.math.abs(offset))
            } else {
                baseColor
            }
        } else {
            baseColor
        }
    }

    val currentEntry = remember(navController) {
        navController.backStack.lastOrNull { it.screen is Screen.TrackedList }
    }

    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = remember(density, screenWidth) { with(density) { screenWidth.toPx() } }

    var accumulatedDragX by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember(screenWidthPx, currentEntry) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (pagerState.currentPage == 0 && (available.x > 0f || accumulatedDragX > 0f)) {
                    accumulatedDragX = (accumulatedDragX + available.x).coerceAtLeast(0f)
                    currentEntry?.let { entry ->
                        if (accumulatedDragX > 0f) {
                            entry.isDragging = true
                            entry.dragOffset = accumulatedDragX
                        } else {
                            entry.isDragging = false
                            entry.dragOffset = 0f
                        }
                    }
                    return Offset(available.x, 0f)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && accumulatedDragX > 0f) {
            val releaseOffset = accumulatedDragX
            accumulatedDragX = 0f
            currentEntry?.let { entry ->
                entry.isDragging = false
                coroutineScope.launch {
                    entry.animatableOffset.snapTo(releaseOffset)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Sticky Header / Top bar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.misc.back,
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = strings.homeScreen.trackingProgress,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Sleek Actions bar: Search Bar + Filter Button + Random Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text(strings.trackedListScreen.searchPlaceholder, color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = strings.misc.search, tint = TextSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            )

            Box {
                IconButton(
                    onClick = { showFilterSheet = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (hasActiveFilters) ElectricViolet.copy(alpha = 0.2f) else SurfaceDark
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, if (hasActiveFilters) ElectricViolet else CardBorder, RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = strings.dashboardScreen.filterTooltip,
                        tint = if (hasActiveFilters) ElectricViolet else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (hasActiveFilters) {
                    Badge(
                        containerColor = NeonCoral,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(10.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    val activeTabId = statusConfigs[pagerState.currentPage].id
                    val randomAnimeId = viewModel.getRandomAnimeIdForCurrentTab()
                    if (randomAnimeId != null) {
                        navController.navigate(
                            Screen.Detail(
                                anilistId = randomAnimeId,
                                sourceStatusId = activeTabId,
                                rouletteCount = 1,
                                visitedIds = randomAnimeId.toString()
                            )
                        )
                    } else {
                        Toast.makeText(context, strings.trackedListScreen.randomEmpty, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Random Anime",
                    tint = TextPrimary
                )
            }
        }

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    val currentPage = pagerState.currentPage
                    val fraction = pagerState.currentPageOffsetFraction
                    val currentTab = tabPositions[currentPage]
                    val targetTab = if (fraction > 0f && currentPage < tabPositions.lastIndex) {
                        tabPositions[currentPage + 1]
                    } else if (fraction < 0f && currentPage > 0) {
                        tabPositions[currentPage - 1]
                    } else {
                        currentTab
                    }
                    val absFraction = kotlin.math.abs(fraction)
                    val left = androidx.compose.ui.unit.lerp(currentTab.left, targetTab.left, absFraction)
                    val width = androidx.compose.ui.unit.lerp(currentTab.width, targetTab.width, absFraction)

                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = left)
                            .width(width),
                        color = activeColor
                    )
                }
            },
            divider = {}
        ) {
            statusConfigs.forEachIndexed { index, config ->
                val isSelected = pagerState.currentPage == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) config.activeIcon else config.inactiveIcon,
                                contentDescription = null,
                                tint = if (isSelected) activeColor else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = config.getLabel(strings),
                                color = if (isSelected) activeColor else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Pager with list grid
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .blur(if (isSliderActive) 16.dp else 0.dp)
                .nestedScroll(nestedScrollConnection),
            userScrollEnabled = scrollEnabled
        ) { pageIndex ->
            // Watch status pages
            val statusId = statusConfigs[pageIndex].id
            val allTrackedList by viewModel.allTrackedAnime.collectAsState()
            val trackingSnapshotMap by viewModel.trackingSnapshotMap.collectAsState()

            val items = remember(statusId, filteredAnime, searchQuery, allTrackedList, trackingSnapshotMap) {
                if (statusId == viewModel.activeTab.value) {
                    filteredAnime
                } else {
                    val byStatus = allTrackedList.filter { trackingSnapshotMap[it.anilistId] == statusId }
                    if (searchQuery.isBlank()) {
                        byStatus
                    } else {
                        byStatus.filter { anime ->
                            anime.titleRomaji.contains(searchQuery, ignoreCase = true) ||
                            (anime.titleEn?.contains(searchQuery, ignoreCase = true) == true) ||
                            (anime.titleUk?.contains(searchQuery, ignoreCase = true) == true)
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) strings.trackedListScreen.randomEmpty else strings.trackedListScreen.emptyState,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                var activeMenuAnimeId by remember { mutableStateOf<Long?>(null) }
                val lazyGridState = rememberLazyGridState()

                if (lazyGridState.isScrollInProgress) {
                    LaunchedEffect(lazyGridState.isScrollInProgress) {
                        activeMenuAnimeId = null
                    }
                }

                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .disableSplitTouch(),
                    userScrollEnabled = scrollEnabled
                ) {
                    items(
                        items = items,
                        key = { it.anilistId }
                    ) { anime ->
                        val trackingEntity = trackingEntitiesMap[anime.anilistId]
                        AnimeBentoCard(
                            anime = anime,
                            status = trackingMap[anime.anilistId],
                            preferUk = preferUk,
                            onGestureActionTriggered = { action, value ->
                                handleQuickGestureAction(
                                    context = context,
                                    anime = anime,
                                    action = action,
                                    value = value,
                                    onOpenDetails = { navController.navigate(Screen.Detail(anime.anilistId, sourceStatusId = null)) },
                                    onOpenWatchStatusPicker = { activeMenuAnimeId = anime.anilistId },
                                    onScoreChange = { newScore -> viewModel.updateScore(anime.anilistId, newScore) },
                                    onEpisodeChange = { newEp -> viewModel.updateEpisodeProgress(anime.anilistId, newEp) }
                                )
                            },
                            onStatusChange = { newStatus -> viewModel.updateWatchStatus(anime.anilistId, newStatus) },
                            isMenuVisible = activeMenuAnimeId == anime.anilistId,
                            onMenuDismiss = { activeMenuAnimeId = null },
                            initialScore = trackingEntity?.score,
                            initialEpisode = trackingEntity?.episodeProgress ?: 0,
                            onDragStateChanged = { isDragging -> scrollEnabled = !isDragging },
                            onSliderStateChanged = { isActive -> isSliderActive = isActive },
                            gestureCenter = gestureCenter,
                            gestureUp = gestureUp,
                            gestureDown = gestureDown,
                            gestureLeft = gestureLeft,
                            gestureRight = gestureRight
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        val genres by viewModel.allGenres.collectAsState()
        val tags by viewModel.allTags.collectAsState()
        val studios by viewModel.allStudios.collectAsState()
        val staff by viewModel.allStaff.collectAsState()

        FilterBottomSheet(
            isCatalog = false,
            onDismiss = { showFilterSheet = false },
            allGenres = genres,
            allTags = tags,
            allStudios = studios,
            allStaff = staff,
            preferUkTitles = preferUk,
            trackedListFilter = listFilterState,
            filteredCount = filteredAnime.size,
            onClearAllFilters = { viewModel.clearAllFilters() },
            onListSortOptionSelected = { viewModel.updateSortOrder(it) },
            onFormatToggled = { viewModel.toggleFormatType(it) },
            onGenreFilterToggled = { viewModel.toggleGenreFilterState(it) },
            onClearGenreFilters = { viewModel.clearGenreFilters() }
        )
    }
}
