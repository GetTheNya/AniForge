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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.dashboard.AnimeBentoCard
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import androidx.compose.material.icons.filled.Casino
import moe.GetTheNya.AniForge.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    anilistId: Long,
    sourceStatusId: String? = null,
    rouletteCount: Int = 0,
    visitedIds: String = "",
    navController: NavController,
    viewModel: DetailViewModel,
    modifier: Modifier = Modifier,
    preferUk: Boolean = true,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val uiState by viewModel.uiState.collectAsState()

    // Trigger load on startup
    LaunchedEffect(anilistId) {
        viewModel.loadAnimeDetail(anilistId, sourceStatusId, rouletteCount, visitedIds)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DetailUiEvent.Navigate -> navController.navigate(event.screen)
                is DetailUiEvent.ShowToast -> {
                    val text = if (event.messageKey == "rouletteExhausted") {
                        strings.trackedListScreen.rouletteExhausted
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
            val distance = exitMax - rouletteCount
            val stepDelay = 350f / exitMax
            val finalDelay = (distance * stepDelay).toLong()
            
            kotlinx.coroutines.delay(finalDelay)
            isCollapsing = true
            
            if (rouletteCount == exitMax) {
                kotlinx.coroutines.delay(350L + 300L)
                navController.finalizeRouletteExit()
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
                        Button(onClick = { viewModel.loadAnimeDetail(anilistId, sourceStatusId, rouletteCount, visitedIds) }, colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)) {
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
                        onStatusChange = viewModel::updateWatchStatus,
                        onIncrementProgress = viewModel::incrementEpisodeProgress,
                        onDecrementProgress = viewModel::decrementEpisodeProgress,
                        onSaveNotes = viewModel::saveNotes,
                        onAnimeClick = { newId ->
                            navController.navigate(Screen.Detail(newId))
                        },
                        onImageClick = { urls, index ->
                            navController.navigate(Screen.ImageViewer(urls, index))
                        },
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

            if (sourceStatusId != null && uiState is DetailUiState.Success) {
                val listColor = statusConfigs.find { it.id == sourceStatusId }?.color ?: MaterialTheme.colorScheme.primary
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

@Composable
fun DetailContent(
    anime: Anime,
    screenshots: List<String>,
    relations: List<Anime>,
    tracking: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity?,
    trackingMap: Map<Long, String>,
    onStatusChange: (String) -> Unit,
    onIncrementProgress: () -> Unit,
    onDecrementProgress: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onAnimeClick: (Long) -> Unit,
    onImageClick: (List<String>, Int) -> Unit,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val scrollState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val headerHeightPx = remember(density) { with(density) { 300.dp.toPx() } }
    val maxOverscrollPx = remember(headerHeightPx) { headerHeightPx * 0.15f }
    val overscrollOffsetState = remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

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
                    .precision(coil.size.Precision.EXACT)
                    .allowHardware(true)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x66000000), BackgroundDark)
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
                        val posterUrl = anime.coverLarge ?: anime.coverMedium
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(posterUrl)
                                .precision(coil.size.Precision.EXACT)
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = anime.format ?: "",
                                    color = NeonCoral,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = anime.getDisplayTitle(preferUk = preferUk),
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = Int.MAX_VALUE,
                                softWrap = true
                            )
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
                        tracking = tracking,
                        maxEpisodes = anime.episodes ?: 0,
                        onStatusChange = onStatusChange,
                        onIncrement = onIncrementProgress,
                        onDecrement = onDecrementProgress,
                        onSaveNotes = onSaveNotes
                    )

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
                            text = anime.descriptionUk ?: anime.descriptionEn ?: strings.detailScreen.noDescription,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
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
                                            .precision(coil.size.Precision.EXACT)
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

                    // Related Anime List (sorted by release date)
                    if (relations.isNotEmpty()) {
                        Column {
                            Text(
                                text = strings.detailScreen.relatedTitles,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = relations,
                                    key = { rel -> rel.anilistId }
                                ) { rel ->
                                    AnimeBentoCard(
                                        anime = rel,
                                        status = trackingMap[rel.anilistId],
                                        preferUk = preferUk,
                                        onClick = { onAnimeClick(rel.anilistId) },
                                        modifier = Modifier.width(160.dp)
                                    )
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
fun TrackingWidget(
    tracking: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity?,
    maxEpisodes: Int,
    onStatusChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    var noteText by remember(tracking?.notes) { mutableStateOf(tracking?.notes ?: "") }
 
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
            statusConfigs.forEach { item ->
                val isSelected = tracking?.watchStatus == item.id
                val buttonColor = item.color
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) buttonColor.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) buttonColor else CardBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onStatusChange(item.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.getLabel(strings),
                        tint = if (isSelected) buttonColor else Color.White.copy(alpha = 0.5f),
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
                OutlinedButton(
                    onClick = onDecrement,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Text("-", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${tracking?.episodeProgress ?: 0} / ${if (maxEpisodes > 0) maxEpisodes else "?"}",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onIncrement,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                ) {
                    Text("+", color = BackgroundDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
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

