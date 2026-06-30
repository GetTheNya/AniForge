package moe.GetTheNya.AniForge.ui.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.EnterTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import moe.GetTheNya.AniForge.ui.localization.getPlural
import moe.GetTheNya.AniForge.ui.localization.getSeasonLabel
import moe.GetTheNya.AniForge.ui.localization.getFormatLabel
import moe.GetTheNya.AniForge.ui.localization.getMediaStatusLabel
import moe.GetTheNya.AniForge.ui.localization.getMediaSourceLabel
import moe.GetTheNya.AniForge.core.model.Anime
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.AnimeStaff
import moe.GetTheNya.AniForge.ui.dashboard.AnimeBentoCard
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import androidx.compose.material.icons.filled.Casino
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.franchises.CollectionFormDialog
import moe.GetTheNya.AniForge.core.model.Studio
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import moe.GetTheNya.AniForge.ui.dashboard.QuickGestureAction
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    anilistId: Long,
    sourceStatusId: String? = null,
    sourceCollectionId: Int? = null,
    rouletteCount: Int = 0,
    visitedIds: String = "",
    navController: NavController,
    viewModel: DetailViewModel,
    onGenreClick: (String) -> Unit,
    onTagClick: (Long) -> Unit,
    onStaffClick: (Long) -> Unit,
    onStudioClick: (Long) -> Unit,
    onStatusClick: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    preferUk: Boolean = true,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val uiState by viewModel.uiState.collectAsState()

    var showCollectionSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRecommendationsSheet by remember { mutableStateOf(false) }

    // Trigger load on startup
    LaunchedEffect(anilistId) {
        viewModel.loadAnimeDetail(anilistId, sourceStatusId, sourceCollectionId, rouletteCount, visitedIds)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DetailUiEvent.Navigate -> navController.navigate(event.screen)
                is DetailUiEvent.ShowToast -> {
                    val text = if (event.messageKey == "rouletteExhausted") {
                        strings.libraryScreen.rouletteExhausted
                    } else {
                        event.messageKey
                    }
                    android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var isCollapsing by remember { mutableStateOf(false) }

    LaunchedEffect(isCollapsing) {
        if (isCollapsing && navController.rouletteExitMaxCount == null) {
            kotlinx.coroutines.delay(300L)
            navController.popBackStack()
        }
    }

    val exitMax = navController.rouletteExitMaxCount
    LaunchedEffect(exitMax) {
        if (exitMax != null) {
            val detailEntries = navController.backStack.filter { it.screen is Screen.Detail }
            val myIndex = detailEntries.indexOfFirst { (it.screen as Screen.Detail).rouletteCount == rouletteCount }
            if (myIndex != -1) {
                val totalDetailScreens = detailEntries.size
                val distance = totalDetailScreens - 1 - myIndex
                val stepDelay = 350f / totalDetailScreens
                val finalDelay = (distance * stepDelay).toLong()
                
                kotlinx.coroutines.delay(finalDelay)
                isCollapsing = true
                
                if (myIndex == detailEntries.lastIndex) {
                    kotlinx.coroutines.delay(350L + 300L)
                    navController.finalizeRouletteExit()
                }
            } else {
                isCollapsing = true
            }
        }
    }

    AnimatedVisibility(
        visible = !isCollapsing,
        enter = EnterTransition.None,
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    CircularProgressIndicator(
                        color = NeonCoral,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val errorMessage = when (state.message) {
                            "Anime not found in catalog" -> strings.detailScreen.errorNotFound
                            "Failed to load details" -> strings.detailScreen.errorFailedToLoad
                            else -> state.message
                        }
                        Text(text = "${strings.misc.error}: $errorMessage", color = NeonCoral, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAnimeDetail(anilistId, sourceStatusId, sourceCollectionId, rouletteCount, visitedIds) }, colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)) {
                            Text(strings.misc.retry)
                        }
                    }
                }
                is DetailUiState.Success -> {
                    DetailContent(
                        anime = state.anime,
                        screenshots = state.screenshots,
                        relations = state.relations,
                        tracking = state.tracking,
                        trackingMap = state.trackingMap,
                        franchise = state.franchise,
                        franchiseReleaseCount = state.franchiseReleaseCount,
                        genres = state.genres,
                        tags = state.tags,
                        staffList = state.staff,
                        studios = state.studios,
                        recommendations = state.recommendations,
                        onStatusChange = viewModel::updateWatchStatus,
                        onIncrementProgress = viewModel::incrementEpisodeProgress,
                        onDecrementProgress = viewModel::decrementEpisodeProgress,
                        onSaveNotes = viewModel::saveNotes,
                        onScoreChange = viewModel::updateScore,
                        onAnimeClick = { newId ->
                            navController.navigate(Screen.Detail(newId))
                        },
                        onImageClick = { urls, index ->
                            navController.navigate(Screen.ImageViewer(urls, index))
                        },
                        onFranchiseClick = { franchiseId ->
                            navController.navigate(Screen.FranchiseTree(franchiseId))
                        },
                        onGenreClick = onGenreClick,
                        onTagClick = onTagClick,
                        onStaffClick = onStaffClick,
                        onStudioClick = onStudioClick,
                        onStatusClick = onStatusClick,
                        onSourceClick = onSourceClick,
                        onRecommendationsClick = { showRecommendationsSheet = true },
                        preferUk = preferUk,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x990C0C0E))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.misc.back,
                    tint = TextPrimary
                )
            }



            // Top Add to Collection Button
            IconButton(
                onClick = { showCollectionSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x990C0C0E))
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = strings.libraryScreen.addToCollection,
                    tint = TextPrimary
                )
            }

            if (showCollectionSheet) {
                val collections by viewModel.collections.collectAsState()
                val animeCollectionIds by viewModel.animeCollectionIds.collectAsState()
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val collectionsLazyListState = rememberLazyListState()
                var gestureStartedAtTop by remember { mutableStateOf(true) }
                LaunchedEffect(collectionsLazyListState.isScrollInProgress) {
                    if (collectionsLazyListState.isScrollInProgress) {
                        gestureStartedAtTop = collectionsLazyListState.firstVisibleItemIndex == 0 &&
                                collectionsLazyListState.firstVisibleItemScrollOffset == 0
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                val bottomSheetScrollConnection = remember(collectionsLazyListState, coroutineScope) {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            var consumedY = 0f
                            if (available.y < 0f) {
                                consumedY = available.y
                                if (source == NestedScrollSource.SideEffect) {
                                    coroutineScope.launch {
                                        try {
                                            collectionsLazyListState.scrollToItem(
                                                collectionsLazyListState.firstVisibleItemIndex,
                                                collectionsLazyListState.firstVisibleItemScrollOffset
                                            )
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                }
                            } else if (available.y > 0f && !gestureStartedAtTop) {
                                consumedY = available.y
                                if (source == NestedScrollSource.SideEffect) {
                                    coroutineScope.launch {
                                        try {
                                            collectionsLazyListState.scrollToItem(
                                                collectionsLazyListState.firstVisibleItemIndex,
                                                collectionsLazyListState.firstVisibleItemScrollOffset
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
                            } else if (available.y > 0f && !gestureStartedAtTop) {
                                consumedY = available.y
                            }
                            return Velocity(x = 0f, y = consumedY)
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { showCollectionSheet = false },
                    sheetState = sheetState,
                    containerColor = BackgroundDark,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.5f)) },
                    contentWindowInsets = { WindowInsets(0.dp) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.9f)
                            .navigationBarsPadding()
                            .nestedScroll(bottomSheetScrollConnection)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = strings.libraryScreen.addToCollection,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        // Prominent "New Collection" action button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .clickable { showCreateDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.detailScreen.newCollection,
                                tint = ElectricViolet,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.detailScreen.newCollection,
                                color = ElectricViolet,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (collections.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.libraryScreen.noCollectionsFound,
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                state = collectionsLazyListState,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(collections, key = { it.id }) { collection ->
                                    val isAdded = animeCollectionIds.contains(collection.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(SurfaceDark)
                                            .border(1.dp, if (isAdded) ElectricViolet else CardBorder, RoundedCornerShape(16.dp))
                                            .clickable { viewModel.toggleAnimeInCollection(collection.id) }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = collection.title,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (collection.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = collection.description,
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Checkbox(
                                            checked = isAdded,
                                            onCheckedChange = { viewModel.toggleAnimeInCollection(collection.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = ElectricViolet,
                                                uncheckedColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                CollectionFormDialog(
                    initialTitle = "",
                    initialDescription = "",
                    dialogTitle = strings.libraryScreen.newCollection,
                    confirmButtonText = strings.libraryScreen.create,
                    descriptionLabel = strings.libraryScreen.descriptionOptional,
                    onDismissRequest = { showCreateDialog = false },
                    onConfirm = { title, description ->
                        viewModel.createNewCollectionWithAnime(title, description)
                        showCreateDialog = false
                    }
                )
            }

            if (showRecommendationsSheet && uiState is DetailUiState.Success) {
                val state = uiState as DetailUiState.Success
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val recsGridState = rememberLazyGridState()
                val recsCoroutineScope = rememberCoroutineScope()
                ModalBottomSheet(
                    onDismissRequest = { showRecommendationsSheet = false },
                    sheetState = sheetState,
                    containerColor = BackgroundDark,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.5f)) },
                    contentWindowInsets = { WindowInsets(0.dp) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.9f)
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = strings.detailScreen.recommendations,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        if (state.recommendations.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recommendations found",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                state = recsGridState,
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                gridItemsIndexed(
                                    items = state.recommendations,
                                    key = { _, recAnime -> recAnime.anilistId }
                                ) { index, recAnime ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        AnimeBentoCard(
                                            anime = recAnime,
                                            status = state.trackingMap[recAnime.anilistId],
                                            preferUk = preferUk,
                                            onGestureActionTriggered = { action, _ ->
                                                if (action == QuickGestureAction.Immediate.OpenDetails) {
                                                    if (recsGridState.isScrollInProgress) {
                                                        recsCoroutineScope.launch { recsGridState.stopScroll() }
                                                    } else {
                                                        showRecommendationsSheet = false
                                                        navController.navigate(Screen.Detail(recAnime.anilistId))
                                                    }
                                                }
                                            },
                                            gestureCenter = QuickGestureAction.Immediate.None,
                                            gestureUp = QuickGestureAction.Immediate.None,
                                            gestureDown = QuickGestureAction.Immediate.None,
                                            gestureLeft = QuickGestureAction.Immediate.None,
                                            gestureRight = QuickGestureAction.Immediate.None,
                                            clickAction = QuickGestureAction.Immediate.OpenDetails,
                                            enableGestures = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if ((sourceStatusId != null || sourceCollectionId != null) && uiState is DetailUiState.Success) {
                val listColor = statusConfigs.find { it.id == sourceStatusId }?.color ?: ElectricViolet
                var lastClickTime by remember { mutableLongStateOf(0L) }
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (rouletteCount >= 2) {
                        IconButton(
                            onClick = {
                                navController.rouletteExitMaxCount = rouletteCount
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x4DFF4C4C))
                                .border(1.dp, Color(0xFFFF4C4C), RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Roulette",
                                tint = Color(0xFFFF4C4C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime > 500) {
                                lastClickTime = currentTime
                                viewModel.MapsToNextRandomAnime()
                            }
                        },
                        containerColor = listColor,
                        contentColor = BackgroundDark,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Random Anime",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailContent(
    anime: Anime,
    screenshots: List<String>,
    relations: List<Anime>,
    tracking: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity?,
    trackingMap: Map<Long, String>,
    franchise: moe.GetTheNya.AniForge.core.model.Franchise?,
    franchiseReleaseCount: Int,
    genres: List<Genre>,
    tags: List<Tag>,
    staffList: List<AnimeStaff>,
    studios: List<Studio> = emptyList(),
    recommendations: List<Anime> = emptyList(),
    onStatusChange: (String) -> Unit,
    onIncrementProgress: () -> Unit,
    onDecrementProgress: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onScoreChange: (Double) -> Unit,
    onAnimeClick: (Long) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    onFranchiseClick: (Long) -> Unit,
    onGenreClick: (String) -> Unit,
    onTagClick: (Long) -> Unit,
    onStaffClick: (Long) -> Unit,
    onStudioClick: (Long) -> Unit,
    onStatusClick: (String) -> Unit,
    onSourceClick: (String) -> Unit,
    onRecommendationsClick: () -> Unit,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val scrollState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val headerHeightPx = remember(density) { with(density) { 300.dp.toPx() } }

    val context = androidx.compose.ui.platform.LocalContext.current
    val bannerWidthPx = remember { context.resources.displayMetrics.widthPixels }
    val bannerHeightPx = remember(density) { with(density) { 300.dp.roundToPx() } }
    val posterWidthPx = remember(density) { with(density) { 90.dp.roundToPx() } }
    val posterHeightPx = remember(density) { with(density) { 130.dp.roundToPx() } }
    val screenshotWidthPx = remember(density) { with(density) { 220.dp.roundToPx() } }
    val screenshotHeightPx = remember(density) { with(density) { 130.dp.roundToPx() } }
    val maxOverscrollPx = remember(headerHeightPx) { headerHeightPx * 0.15f }
    val overscrollOffsetState = remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var isTagsExpanded by remember { mutableStateOf(false) }
    var tagsContentHeight by remember { mutableStateOf(0) }
    val isTagsScrollable = remember(tagsContentHeight) {
        with(density) { tagsContentHeight.toDp() > 110.dp }
    }

    val nestedScrollConnection = remember(scrollState, headerHeightPx, maxOverscrollPx, coroutineScope) {
        var springJob: kotlinx.coroutines.Job? = null
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Instant Gesture Interruption
                if (source == NestedScrollSource.UserInput && (springJob?.isActive == true || overscrollOffsetState.value > 0f)) {
                    springJob?.cancel()
                    springJob = null
                }

                val delta = available.y
                val currentOverscroll = overscrollOffsetState.value
                if (delta < 0f && currentOverscroll > 0f) {
                    val newOverscroll = (currentOverscroll + delta).coerceIn(0f, maxOverscrollPx)
                    val consumed = newOverscroll - currentOverscroll
                    overscrollOffsetState.value = newOverscroll
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Instant Gesture Interruption
                if (source == NestedScrollSource.UserInput && springJob?.isActive == true) {
                    springJob?.cancel()
                    springJob = null
                }

                val delta = available.y
                if (delta > 0f && scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0) {
                    val currentOverscroll = overscrollOffsetState.value
                    val newOverscroll = (currentOverscroll + delta).coerceIn(0f, maxOverscrollPx)
                    val consumedDelta = newOverscroll - currentOverscroll
                    overscrollOffsetState.value = newOverscroll
                    return Offset(0f, consumedDelta)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val currentOverscroll = overscrollOffsetState.value
                if (currentOverscroll > 0f) {
                    springJob?.cancel()
                    springJob = coroutineScope.launch {
                        try {
                            val animatable = androidx.compose.animation.core.Animatable(currentOverscroll)
                            animatable.animateTo(
                                targetValue = 0f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                )
                            ) {
                                overscrollOffsetState.value = this.value
                            }
                            animatable.snapTo(0f)
                            overscrollOffsetState.value = 0f
                        } catch (e: Exception) {
                            // handle cancellation silently
                        }
                    }
                }
                // Return available velocity to consume it completely, eliminating ghost double-bounce
                return available
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Background Layer (the Image)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    val currentOverscroll = overscrollOffsetState.value.coerceIn(0f, maxOverscrollPx)
                    val rawProgress = if (headerHeightPx > 0f) currentOverscroll / headerHeightPx else 0f
                    val dampenedScale = 1.0f + (rawProgress * 0.5f)
                    val scale = dampenedScale.coerceIn(1.0f, 1.10f)
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    
                    val scrollOffset = if (scrollState.firstVisibleItemIndex == 0) {
                        scrollState.firstVisibleItemScrollOffset.toFloat()
                    } else {
                        headerHeightPx
                    }
                    translationY = (-scrollOffset * 0.5f).coerceIn(-headerHeightPx, 0f)
                }
        ) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(anime.bannerImage ?: anime.coverLarge)
                    .size(bannerWidthPx, bannerHeightPx)
                    .precision(coil.size.Precision.EXACT)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .allowHardware(true)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }

        // Foreground Layer (the Content)
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = overscrollOffsetState.value
                }
        ) {
            // First item: Spacer + Poster/Title Row
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    // Title & Poster Row overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Dedicated vertical poster image next to title section
                        val posterUrl = anime.coverLarge
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(posterUrl)
                                .size(posterWidthPx, posterHeightPx)
                                .precision(coil.size.Precision.EXACT)
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .allowHardware(true)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(90.dp)
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (posterUrl != null) {
                                        onImageClick(listOf(posterUrl), 0)
                                    }
                                }
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
 
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .wrapContentHeight()
                                .fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${anime.scoreMal ?: "N/A"}",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val format = anime.format
                                val formatLabel = format?.let { strings.formats.getFormatLabel(it) }
                                if (!formatLabel.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "•",
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatLabel,
                                        color = NeonCoral,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val seasonText = anime.season?.let { strings.seasons.getSeasonLabel(it) }
                                val seasonYear = anime.seasonYear
                                if (seasonYear != null) {
                                    val formattedSeason = if (seasonText != null) "$seasonText $seasonYear" else "$seasonYear"
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "•",
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formattedSeason,
                                        color = TextPrimary.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (anime.isAdult) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "•",
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFFF3B30))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "18+",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            AutoScalingTitle(
                                text = anime.getDisplayTitle(preferUk = preferUk),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (anime.titleRomaji.isNotBlank() && anime.titleRomaji != anime.getDisplayTitle(preferUk = preferUk)) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = anime.titleRomaji,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = Int.MAX_VALUE,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }

            // Body Content
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // User Tracking Section
                    TrackingWidget(
                        anime = anime,
                        tracking = tracking,
                        onStatusChange = onStatusChange,
                        onIncrement = onIncrementProgress,
                        onDecrement = onDecrementProgress,
                        onSaveNotes = onSaveNotes,
                        onScoreChange = onScoreChange
                    )

                    // Secondary Metadata Chips Layout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First Row: Studio Badges
                        if (studios.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                studios.forEach { studio ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(SurfaceCardDark)
                                            .border(1.dp, CardBorder, RoundedCornerShape(50.dp))
                                            .clickable { onStudioClick(studio.studioId) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Business,
                                                contentDescription = null,
                                                tint = CyberTeal,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = studio.name,
                                                color = CyberTeal,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Second Row: Episodes, Duration, Source and Status Badges
                        val source = anime.source
                        val status = anime.status
                        val currentEpisodes = anime.getReleasedEpisodes()
                        val totalEpisodes = anime.episodes
                        val episodeSuffix = strings.detailScreen.episodeSuffix
                        val episodesBadgeText = when {
                            status?.uppercase() == "FINISHED" && totalEpisodes != null -> "$totalEpisodes $episodeSuffix"
                            currentEpisodes != null && totalEpisodes != null -> {
                                if (currentEpisodes == totalEpisodes) {
                                    "$totalEpisodes $episodeSuffix"
                                } else {
                                    "$currentEpisodes / $totalEpisodes $episodeSuffix"
                                }
                            }
                            currentEpisodes != null && totalEpisodes == null -> "$currentEpisodes / ? $episodeSuffix"
                            else -> "? $episodeSuffix"
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(SurfaceCardDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = episodesBadgeText,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                                if (anime.duration != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(SurfaceCardDark)
                                            .border(1.dp, CardBorder, RoundedCornerShape(50.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "${anime.duration} ${strings.detailScreen.durationSuffix}",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                if (source != null) {
                                    val sourceLabel = strings.mediaSources.getMediaSourceLabel(source)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(NeonCoral.copy(alpha = 0.15f))
                                            .border(1.dp, NeonCoral.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                            .clickable { onSourceClick(source) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Book,
                                                contentDescription = null,
                                                tint = NeonCoral,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = sourceLabel,
                                                color = NeonCoral,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (status != null) {
                                    val statusLabel = strings.mediaStatuses.getMediaStatusLabel(status)
                                    val (statusColor, statusBgColor) = when (status.uppercase()) {
                                        "RELEASING" -> Color(0xFF81C784) to Color(0xFF2E7D32).copy(alpha = 0.2f)
                                        "FINISHED" -> TextSecondary to TextSecondary.copy(alpha = 0.15f)
                                        "HIATUS" -> Color(0xFFFFB74D) to Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        "NOT_YET_RELEASED" -> Color(0xFF64B5F6) to Color(0xFF1976D2).copy(alpha = 0.2f)
                                        "CANCELLED" -> Color(0xFFE57373) to Color(0xFFC62828).copy(alpha = 0.2f)
                                        else -> TextSecondary to TextSecondary.copy(alpha = 0.15f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(statusBgColor)
                                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                                            .clickable { onStatusClick(status) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = statusColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = statusLabel,
                                                color = statusColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                    }

                    // Synopsis
                    Column {
                        Text(
                            text = strings.detailScreen.synopsis,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = anime.getDisplayDescription(preferUk = preferUk) ?: strings.detailScreen.noDescription,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }

                    // Video Trailer Player Block
                    val trailerId = anime.trailerId
                    val trailerSite = anime.trailerSite
                    if (!trailerId.isNullOrBlank() && !trailerSite.isNullOrBlank()) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.detailScreen.trailer,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (trailerSite.lowercase() == "youtube") {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://www.youtube.com/watch?v=" + trailerId)
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                            ) {
                                if (!anime.trailerThumbnail.isNullOrBlank()) {
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(anime.trailerThumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Trailer Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(SurfaceCardDark)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                            )
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.White.copy(alpha = 0.7f))
                                        .align(Alignment.Center),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Trailer",
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Genres & Tags
                    if (genres.isNotEmpty() || tags.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (genres.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = strings.detailScreen.genresTitle,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    CustomStretchingFlowRow(
                                        horizontalGap = 8.dp,
                                        verticalGap = 8.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        genres.forEach { genre ->
                                            GenreChipPremium(
                                                name = genre.getDisplayName(preferUk),
                                                onClick = { onGenreClick(genre.slug) }
                                            )
                                        }
                                    }
                                }
                            }

                            if (tags.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.detailScreen.tagsTitle,
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isTagsScrollable) {
                                            IconButton(
                                                onClick = { isTagsExpanded = !isTagsExpanded },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isTagsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = if (isTagsExpanded) "Collapse" else "Expand",
                                                    tint = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                            .then(
                                                if (!isTagsExpanded && isTagsScrollable) {
                                                    Modifier
                                                        .heightIn(max = 110.dp)
                                                        .clipToBounds()
                                                        .clickable { isTagsExpanded = true }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        CustomStretchingFlowRow(
                                            horizontalGap = 8.dp,
                                            verticalGap = 8.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                                .onGloballyPositioned { coordinates ->
                                                    tagsContentHeight = coordinates.size.height
                                                }
                                        ) {
                                            tags.forEach { tag ->
                                                TagChipMinimal(
                                                    name = tag.getDisplayName(preferUk),
                                                    onClick = { onTagClick(tag.tagId) }
                                                )
                                            }
                                        }

                                        if (!isTagsExpanded && isTagsScrollable) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, BackgroundDark)
                                                        )
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
 
                    // Screenshots Gallery
                    if (screenshots.isNotEmpty()) {
                        Column {
                            Text(
                                text = strings.detailScreen.gallery,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = screenshots,
                                    key = { _, item -> item }
                                ) { index, imageUrl ->
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(imageUrl)
                                            .size(screenshotWidthPx, screenshotHeightPx)
                                            .precision(coil.size.Precision.EXACT)
                                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                            .allowHardware(true)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = strings.detailScreen.screenshot,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(220.dp)
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                            .clickable {
                                                onImageClick(screenshots, index)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Staff Section
                    if (staffList.isNotEmpty()) {
                        StaffSection(
                            staffList = staffList,
                            onStaffClick = onStaffClick
                        )
                    }

                    // Franchise Bento Widget (Replaces legacy Related Releases row)
                    if (franchise != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.libraryScreen.franchises,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceCardDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                                    .clickable { onFranchiseClick(franchise.franchiseId) }
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minimal 3-node vertical visual timeline hint
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(64.dp)
                                ) {
                                    val x = size.width / 2f
                                    val h = size.height
                                    
                                    // Spine line
                                    drawLine(
                                        color = ElectricViolet.copy(alpha = 0.4f),
                                        start = Offset(x, 4.dp.toPx()),
                                        end = Offset(x, h - 4.dp.toPx()),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                    
                                    // Node 1 (top)
                                    drawCircle(
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        radius = 3.dp.toPx(),
                                        center = Offset(x, 10.dp.toPx())
                                    )
                                    
                                    // Active Node (middle) - glowing and highlighted
                                    // Glow
                                    drawCircle(
                                        color = ElectricViolet.copy(alpha = 0.3f),
                                        radius = 12.dp.toPx(),
                                        center = Offset(x, h / 2)
                                    )
                                    // Fill
                                    drawCircle(
                                        color = ElectricViolet,
                                        radius = 6.dp.toPx(),
                                        center = Offset(x, h / 2)
                                    )
                                    
                                    // Node 3 (bottom)
                                    drawCircle(
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        radius = 3.dp.toPx(),
                                        center = Offset(x, h - 10.dp.toPx())
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Text details
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = String.format(strings.detailScreen.partOfFranchise, franchise.getDisplayName(preferUk) ?: ""),
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = strings.detailScreen.releasesCount.getPlural(franchiseReleaseCount),
                                        color = CyberTeal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Chevron arrow indicating CTA navigation pass
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Recommendations Section
                    if (recommendations.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Header Row Layout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = strings.detailScreen.recommendations,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.detailScreen.providedByAnilist,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                IconButton(onClick = onRecommendationsClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View all recommendations",
                                        tint = TextPrimary
                                    )
                                }
                            }

                            // Content Scroll Engine (LazyRow)
                            val recsRowState = rememberLazyListState()
                            LazyRow(
                                state = recsRowState,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(
                                    items = recommendations,
                                    key = { _, recAnime -> recAnime.anilistId }
                                ) { index, recAnime ->
                                    Box(
                                        modifier = Modifier
                                            .width(150.dp)
                                    ) {
                                        AnimeBentoCard(
                                            anime = recAnime,
                                            status = trackingMap[recAnime.anilistId],
                                            preferUk = preferUk,
                                            onGestureActionTriggered = { action, _ ->
                                                if (action == QuickGestureAction.Immediate.OpenDetails) {
                                                    if (recsRowState.isScrollInProgress) {
                                                        coroutineScope.launch { recsRowState.stopScroll() }
                                                    } else {
                                                        onAnimeClick(recAnime.anilistId)
                                                    }
                                                }
                                            },
                                            gestureCenter = QuickGestureAction.Immediate.None,
                                            gestureUp = QuickGestureAction.Immediate.None,
                                            gestureDown = QuickGestureAction.Immediate.None,
                                            gestureLeft = QuickGestureAction.Immediate.None,
                                            gestureRight = QuickGestureAction.Immediate.None,
                                            clickAction = QuickGestureAction.Immediate.OpenDetails,
                                            enableGestures = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                val customBottomBarHeight = 92.dp
                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars))
                Spacer(modifier = Modifier.height(customBottomBarHeight + 16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackingWidget(
    anime: Anime,
    tracking: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity?,
    onStatusChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onScoreChange: (Double) -> Unit
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    var noteText by remember(tracking?.notes) { mutableStateOf(tracking?.notes ?: "") }

    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible) {
            focusManager.clearFocus()
        }
    }

    val maxAllowedIncrement = anime.getMaxAllowedIncrement()
    val currentProgress = tracking?.episodeProgress ?: 0
    val isIncrementEnabled = currentProgress < maxAllowedIncrement
    val progressText = anime.getEpisodeBadgeText(currentProgress, strings.detailScreen.episodeSuffix)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = strings.detailScreen.myProgress, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
 
        // Status Chips row replaced by premium compact horizontal row of icon-based selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isAnimeNotYetReleased = anime.isNotYetReleased()
            statusConfigs.forEach { item ->
                val isSelected = tracking?.watchStatus == item.id
                // NOT_YET_RELEASED: only PLANNING is interactive; everything else is locked.
                val isEnabled = !isAnimeNotYetReleased || item.id == "PLANNING"
                val buttonColor = item.color
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected && isEnabled -> buttonColor.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isSelected && isEnabled -> buttonColor
                                isEnabled -> CardBorder
                                else -> CardBorder.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .then(
                            if (isEnabled) Modifier.clickable { onStatusChange(item.id) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected && isEnabled) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.getLabel(strings),
                        tint = when {
                            isSelected && isEnabled -> buttonColor
                            isEnabled -> Color.White.copy(alpha = 0.5f)
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Episode counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = strings.detailScreen.episodesWatched, color = TextSecondary, fontSize = 14.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isDecrementEnabled = !anime.isNotYetReleased() && currentProgress > 0
                OutlinedButton(
                    onClick = onDecrement,
                    enabled = isDecrementEnabled,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDecrementEnabled) CardBorder else CardBorder.copy(alpha = 0.3f))
                ) {
                    Text("-", color = if (isDecrementEnabled) TextPrimary else TextSecondary.copy(alpha = 0.3f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = progressText,
                    color = if (anime.isNotYetReleased()) TextSecondary.copy(alpha = 0.4f) else TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onIncrement,
                    enabled = isIncrementEnabled && !anime.isNotYetReleased(),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCoral,
                        disabledContainerColor = NeonCoral.copy(alpha = 0.3f)
                    )
                ) {
                    val plusEnabled = isIncrementEnabled && !anime.isNotYetReleased()
                    Text("+", color = if (plusEnabled) BackgroundDark else TextSecondary.copy(alpha = 0.5f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Score Rating Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                var sliderValue by remember(tracking?.score) { mutableFloatStateOf(tracking?.score?.toFloat() ?: 0.0f) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = strings.misc.score, color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = if (sliderValue > 0f) String.format("%.1f", sliderValue) else "-",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { 
                        sliderValue = (Math.round(it * 2.0) / 2.0).toFloat()
                    },
                    onValueChangeFinished = {
                        onScoreChange(sliderValue.toDouble())
                    },
                    valueRange = 0.0f..10.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = NeonCoral,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        thumbColor = NeonCoral
                    ),
                    modifier = Modifier.fillMaxWidth().height(32.dp)
                )
            }
        }

        // Notes Input
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = strings.detailScreen.personalNotes, color = TextSecondary, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(strings.detailScreen.addCustomNotes, color = TextSecondary, fontSize = 13.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color(0x1AFFFFFF),
                        unfocusedContainerColor = Color(0x1AFFFFFF),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSaveNotes(noteText) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal)
                ) {
                    Text(strings.misc.save, color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun GenreChipPremium(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(0.5.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TagChipMinimal(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current
    val prefix = if (strings.languageCode == "uk") "• " else "#"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2C35))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$prefix$name",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun StaffSection(
    staffList: List<AnimeStaff>,
    onStaffClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current
    val density = LocalDensity.current
    val avatarSizePx = remember(density) { with(density) { 80.dp.roundToPx() } }

    var maxCardHeightPx by remember { mutableStateOf(0) }
    val maxCardHeightDp = remember(maxCardHeightPx) {
        with(density) { maxCardHeightPx.toDp() }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = strings.detailScreen.staffTitle,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = staffList,
                key = { it.staffId.toString() + "_" + it.role }
            ) { member ->
                StaffCard(
                    member = member,
                    avatarSizePx = avatarSizePx,
                    onStaffClick = onStaffClick,
                    modifier = Modifier
                        .then(
                            if (maxCardHeightDp > 0.dp) {
                                Modifier.heightIn(min = maxCardHeightDp)
                            } else {
                                Modifier
                            }
                        )
                        .onGloballyPositioned { coords ->
                            val height = coords.size.height
                            if (height > maxCardHeightPx) {
                                maxCardHeightPx = height
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun StaffCard(
    member: AnimeStaff,
    avatarSizePx: Int,
    onStaffClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onStaffClick(member.staffId) }
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(member.imageLarge)
                .size(avatarSizePx, avatarSizePx)
                .precision(coil.size.Precision.EXACT)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .allowHardware(true)
                .crossfade(true)
                .build(),
            contentDescription = member.fullName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .border(1.dp, CardBorder, androidx.compose.foundation.shape.CircleShape)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = member.fullName,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = member.role,
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CustomStretchingFlowRow(
    modifier: Modifier = Modifier,
    horizontalGap: androidx.compose.ui.unit.Dp = 8.dp,
    verticalGap: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val g = horizontalGap.roundToPx()
        val v = verticalGap.roundToPx()
        
        val naturalWidths = measurables.map { measurable ->
            measurable.maxIntrinsicWidth(constraints.maxHeight)
        }
        
        val rows = mutableListOf<MutableList<Pair<androidx.compose.ui.layout.Measurable, Int>>>()
        var currentRow = mutableListOf<Pair<androidx.compose.ui.layout.Measurable, Int>>()
        var currentRowWidth = 0
        
        measurables.zip(naturalWidths).forEach { pair ->
            val (_, itemWidth) = pair
            if (currentRow.isEmpty()) {
                currentRow.add(pair)
                currentRowWidth = itemWidth
            } else if (currentRowWidth + g + itemWidth <= constraints.maxWidth) {
                currentRow.add(pair)
                currentRowWidth += g + itemWidth
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(pair)
                currentRowWidth = itemWidth
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        val finalPlaceables = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var totalHeight = 0
        
        rows.forEachIndexed { rowIndex, row ->
            val rowPlaceables = mutableListOf<androidx.compose.ui.layout.Placeable>()
            if (row.size == 1) {
                val (measurable, naturalWidth) = row[0]
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = naturalWidth.coerceAtMost(constraints.maxWidth),
                        minHeight = 0,
                        maxHeight = constraints.maxHeight
                    )
                )
                rowPlaceables.add(placeable)
            } else {
                val K = row.size
                val totalWidth = row.sumOf { it.second }
                val totalGaps = (K - 1) * g
                val remainingSpace = constraints.maxWidth - totalWidth - totalGaps
                val extraWidth = if (remainingSpace > 0) remainingSpace / K else 0
                val remainder = if (remainingSpace > 0) remainingSpace % K else 0
                
                row.forEachIndexed { index, (measurable, naturalWidth) ->
                    val addedWidth = extraWidth + (if (index < remainder) 1 else 0)
                    val targetWidth = naturalWidth + addedWidth
                    val remeasured = measurable.measure(
                        Constraints(
                            minWidth = targetWidth,
                            maxWidth = targetWidth,
                            minHeight = 0,
                            maxHeight = constraints.maxHeight
                        )
                    )
                    rowPlaceables.add(remeasured)
                }
            }
            finalPlaceables.add(rowPlaceables)
            
            val rowHeight = rowPlaceables.maxOf { it.height }
            totalHeight += rowHeight
            if (rowIndex < rows.size - 1) {
                totalHeight += v
            }
        }
        
        layout(constraints.maxWidth, totalHeight) {
            var currentY = 0
            finalPlaceables.forEachIndexed { rowIndex, row ->
                var currentX = 0
                val rowHeight = row.maxOf { it.height }
                row.forEach { placeable ->
                    val yOffset = (rowHeight - placeable.height) / 2
                    placeable.placeRelative(currentX, currentY + yOffset)
                    currentX += placeable.width + g
                }
                currentY += rowHeight + v
            }
        }
    }
}

@Composable
fun AutoScalingTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 3,
    initialFontSize: Float = 24f,
    minFontSize: Float = 14f
) {
    var fontSizeValue by remember(text) { mutableStateOf(initialFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontSize = fontSizeValue.sp,
        fontWeight = fontWeight,
        maxLines = maxLines,
        softWrap = true,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowHeight || textLayoutResult.lineCount > maxLines) {
                if (fontSizeValue > minFontSize) {
                    fontSizeValue = (fontSizeValue - 1f).coerceAtLeast(minFontSize)
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        },
        modifier = modifier.graphicsLayer {
            alpha = if (readyToDraw) 1f else 0f
        }
    )
}

