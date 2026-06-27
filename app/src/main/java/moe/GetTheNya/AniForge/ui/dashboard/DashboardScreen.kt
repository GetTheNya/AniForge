package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.theme.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.gestures.stopScroll
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import moe.GetTheNya.AniForge.core.model.AnimeWithTracking
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onAnimeClick: (Long) -> Unit,
    viewModel: DashboardViewModel,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible) {
            focusManager.clearFocus()
        }
    }
    val lazyPagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
    val filteredCount by viewModel.filteredCount.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()
    val allTracking by viewModel.allTracking.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    val gestureCenter by viewModel.gestureCenter.collectAsState()
    val gestureUp by viewModel.gestureUp.collectAsState()
    val gestureDown by viewModel.gestureDown.collectAsState()
    val gestureLeft by viewModel.gestureLeft.collectAsState()
    val gestureRight by viewModel.gestureRight.collectAsState()

    val hasActiveFilters = remember(searchFilter) {
        searchFilter.genres.isNotEmpty() ||
        searchFilter.excludedGenres.isNotEmpty() ||
        searchFilter.studios.isNotEmpty() ||
        searchFilter.excludedStudios.isNotEmpty() ||
        searchFilter.tags.isNotEmpty() ||
        searchFilter.excludedTags.isNotEmpty() ||
        searchFilter.staff.isNotEmpty() ||
        searchFilter.excludedStaff.isNotEmpty() ||
        searchFilter.minScore != null ||
        searchFilter.maxScore != null ||
        searchFilter.episodeGroups.isNotEmpty() ||
        searchFilter.excludedEpisodeGroups.isNotEmpty() ||
        searchFilter.formats.isNotEmpty() ||
        searchFilter.excludedFormats.isNotEmpty() ||
        searchFilter.hasUkTranslation == true ||
        searchFilter.trackingStatuses.isNotEmpty() ||
        searchFilter.excludedTrackingStatuses.isNotEmpty() ||
        searchFilter.mediaStatuses.isNotEmpty() ||
        searchFilter.excludedMediaStatuses.isNotEmpty() ||
        searchFilter.mediaSources.isNotEmpty() ||
        searchFilter.excludedMediaSources.isNotEmpty()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Sticky Header & Search Bar with Filter Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchBar(
                query = searchFilter.textQuery,
                onQueryChange = viewModel::updateSearchQuery,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
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
        }

        // Main Bento list container
        val refreshState = lazyPagingItems.loadState.refresh
        if (refreshState is LoadState.Loading && lazyPagingItems.itemCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonCoral)
            }
        } else if (refreshState is LoadState.Error && lazyPagingItems.itemCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val errorMessage = refreshState.error.message ?: strings.dashboardScreen.unknownError
                Text(text = "${strings.misc.error}: $errorMessage", color = NeonCoral)
            }
        } else {
            DashboardContent(
                lazyPagingItems = lazyPagingItems,
                filteredCount = filteredCount,
                allTracking = allTracking,
                gestureCenter = gestureCenter,
                gestureUp = gestureUp,
                gestureDown = gestureDown,
                gestureLeft = gestureLeft,
                gestureRight = gestureRight,
                preferUk = preferUk,
                onAnimeClick = onAnimeClick,
                onStatusChange = { anilistId, status -> viewModel.updateWatchStatus(anilistId, status) },
                onScoreChange = { anilistId, score -> viewModel.updateScore(anilistId, score) },
                onEpisodeChange = { anilistId, progress -> viewModel.updateEpisodeProgress(anilistId, progress) }
            )
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            viewModel = viewModel,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(strings.dashboardScreen.searchPlaceholder, color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = strings.misc.search, tint = TextSecondary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.misc.clear,
                        tint = TextSecondary
                    )
                }
            }
        },
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
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    )
}

@Composable
fun DashboardContent(
    lazyPagingItems: LazyPagingItems<AnimeWithTracking>,
    filteredCount: Int,
    allTracking: Map<Long, UserTrackingEntity>?,
    gestureCenter: QuickGestureAction,
    gestureUp: QuickGestureAction,
    gestureDown: QuickGestureAction,
    gestureLeft: QuickGestureAction,
    gestureRight: QuickGestureAction,
    preferUk: Boolean,
    onAnimeClick: (Long) -> Unit,
    onStatusChange: (Long, String) -> Unit,
    onScoreChange: (Long, Double) -> Unit,
    onEpisodeChange: (Long, Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    var activeMenuAnimeId by remember { mutableStateOf<Long?>(null) }
    var gridScrollEnabled by remember { mutableStateOf(true) }
    var isSliderActive by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .filter { it }
            .collect {
                activeMenuAnimeId = null
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                activeMenuAnimeId = null
            }
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isSliderActive) 16.dp else 0.dp),
            userScrollEnabled = gridScrollEnabled
        ) {
            // Header for Catalog listing
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.dashboardScreen.discoverCatalog,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.dashboardScreen.foundCount.replace("{count}", filteredCount.toString()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Grid Catalog items
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.anime.anilistId },
                contentType = lazyPagingItems.itemContentType { "anime_card" }
            ) { index ->
                val item = lazyPagingItems[index]
                if (item != null) {
                    val anime = item.anime
                    val currentStatus = if (allTracking != null) {
                        allTracking[anime.anilistId]?.watchStatus
                    } else {
                        item.watchStatus
                    }
                    val currentScore = if (allTracking != null) {
                        allTracking[anime.anilistId]?.score
                    } else {
                        item.score
                    }
                    val currentEpisode = if (allTracking != null) {
                        allTracking[anime.anilistId]?.episodeProgress ?: 0
                    } else {
                        item.episodeProgress
                    }

                    AnimeBentoCard(
                        anime = anime,
                        status = currentStatus,
                        preferUk = preferUk,
                        onGestureActionTriggered = { action, value ->
                            handleQuickGestureAction(
                                context = context,
                                anime = anime,
                                action = action,
                                value = value,
                                onOpenDetails = {
                                    if (gridState.isScrollInProgress) {
                                        coroutineScope.launch { gridState.stopScroll() }
                                    } else {
                                        onAnimeClick(anime.anilistId)
                                    }
                                },
                                onOpenWatchStatusPicker = { activeMenuAnimeId = anime.anilistId },
                                onScoreChange = { newScore -> onScoreChange(anime.anilistId, newScore) },
                                onEpisodeChange = { newEp -> onEpisodeChange(anime.anilistId, newEp) }
                            )
                        },
                        onStatusChange = { newStatus -> onStatusChange(anime.anilistId, newStatus) },
                        isMenuVisible = activeMenuAnimeId == anime.anilistId,
                        onMenuDismiss = { activeMenuAnimeId = null },
                        initialScore = currentScore,
                        initialEpisode = currentEpisode,
                        onDragStateChanged = { isDragging -> gridScrollEnabled = !isDragging },
                        onSliderStateChanged = { isActive -> isSliderActive = isActive },
                        gestureCenter = gestureCenter,
                        gestureUp = gestureUp,
                        gestureDown = gestureDown,
                        gestureLeft = gestureLeft,
                        gestureRight = gestureRight
                    )
                }
            }

            // Append state loader
            val appendState = lazyPagingItems.loadState.append
            if (appendState is LoadState.Loading) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonCoral,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else if (appendState is LoadState.Error) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.misc.error,
                            color = NeonCoral,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BentoStatsCard(stats: UserStats) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${stats.episodesWatched}", color = NeonCoral, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(text = strings.misc.episodes, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = CardBorder)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${stats.titlesCompleted}", color = CyberTeal, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(text = strings.misc.completed, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp), color = CardBorder)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${stats.titlesWatching}", color = ElectricViolet, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(text = strings.misc.watching, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun FeaturedBentoCard(
    anime: Anime,
    onClick: () -> Unit,
    preferUk: Boolean = true
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
                text = anime.getDisplayDescription(preferUk = preferUk) ?: "",
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GenreChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) NeonCoral else SurfaceDark)
            .border(1.dp, if (selected) NeonCoral else CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = if (selected) BackgroundDark else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
