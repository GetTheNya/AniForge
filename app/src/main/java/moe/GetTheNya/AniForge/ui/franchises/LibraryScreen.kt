package moe.GetTheNya.AniForge.ui.franchises

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import moe.GetTheNya.AniForge.core.model.WaitingItem
import moe.GetTheNya.AniForge.core.model.CandidateStatus
import moe.GetTheNya.AniForge.ui.localization.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FilterList
import android.widget.Toast
import moe.GetTheNya.AniForge.ui.dashboard.FilterBottomSheet
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.localization.getPlural
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.AnimeStatusColors
import moe.GetTheNya.AniForge.ui.utils.disableSplitTouch
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.ui.dashboard.AnimeBentoCard
import moe.GetTheNya.AniForge.ui.dashboard.QuickGestureAction
import moe.GetTheNya.AniForge.ui.dashboard.handleQuickGestureAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

@Composable
fun LibraryFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) color.copy(alpha = 0.15f) else SurfaceDark
    val borderColor = if (isSelected) color else CardBorder
    val textColor = if (isSelected) color else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun LibraryFilter.getLabel(strings: moe.GetTheNya.AniForge.ui.localization.LocaleStrings): String {
    return when (this) {
        LibraryFilter.WATCHING -> strings.misc.watching
        LibraryFilter.PLANNING -> strings.misc.planning
        LibraryFilter.COMPLETED -> strings.misc.completed
        LibraryFilter.PAUSED -> strings.misc.paused
        LibraryFilter.DROPPED -> strings.misc.dropped
        LibraryFilter.RATED -> strings.libraryScreen.ratedTab
        LibraryFilter.WAITING -> strings.libraryScreen.waiting
        LibraryFilter.COLLECTIONS -> strings.libraryScreen.collections
    }
}

fun LibraryFilter.getLabelWithCount(strings: moe.GetTheNya.AniForge.ui.localization.LocaleStrings, count: Int?): String {
    val base = getLabel(strings)
    return if (count != null && count > 0) "$base ($count)" else base
}

fun LibraryFilter.getColor(): Color {
    val matched = statusConfigs.firstOrNull { it.id == this.dbStatus }
    return when (this) {
        LibraryFilter.RATED -> Color(0xFFF5A623)
        LibraryFilter.WAITING -> Color(0xFFF59E0B)
        else -> matched?.color ?: ElectricViolet
    }
}

@Composable
fun UserTrackedListContent(
    animeList: List<Anime>,
    viewModel: LibraryViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current

    if (animeList.isEmpty()) {
        val searchQuery by viewModel.searchQuery.collectAsState()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) strings.libraryScreen.randomEmpty else strings.libraryScreen.emptyState,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    } else {
        val lazyGridState = rememberLazyGridState()
        var activeMenuAnimeId by remember { mutableStateOf<Long?>(null) }
        var scrollEnabled by remember { mutableStateOf(true) }
        var isSliderActive by remember { mutableStateOf(false) }

        val gestureCenter by viewModel.gestureCenter.collectAsState()
        val gestureUp by viewModel.gestureUp.collectAsState()
        val gestureDown by viewModel.gestureDown.collectAsState()
        val gestureLeft by viewModel.gestureLeft.collectAsState()
        val gestureRight by viewModel.gestureRight.collectAsState()

        val trackingMap by viewModel.trackingMap.collectAsState()
        val trackingEntitiesMap by viewModel.trackingEntitiesMap.collectAsState()

        if (lazyGridState.isScrollInProgress) {
            LaunchedEffect(lazyGridState.isScrollInProgress) {
                activeMenuAnimeId = null
            }
        }

        val activeFilter by viewModel.activeFilter.collectAsState()
        val isPlannedTab = activeFilter == LibraryFilter.PLANNING
        val readyToWatchItems by viewModel.readyToWatchItems.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isSliderActive) 16.dp else 0.dp)
        ) {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .disableSplitTouch(),
                userScrollEnabled = scrollEnabled
            ) {
                if (isPlannedTab && readyToWatchItems.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        PinnedAiringSection(
                            readyToWatchItems = readyToWatchItems,
                            onItemClick = { animeId -> navController.navigate(Screen.Detail(animeId, sourceStatusId = null)) },
                            onStartWatching = { animeId -> viewModel.updateWatchStatus(animeId, "CURRENT") },
                            preferUk = preferUk
                        )
                    }
                }

                items(
                    items = animeList,
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

private fun getCenteredItemIndex(lazyListState: androidx.compose.foundation.lazy.LazyListState): Int {
    val layoutInfo = lazyListState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0
    val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2f
    val closest = visibleItems.minByOrNull {
        val itemCenter = it.offset + it.size / 2f
        kotlin.math.abs(itemCenter - viewportCenter)
    }
    return closest?.index ?: 0
}

@Composable
fun WaitingListContent(
    itemsList: List<WaitingItem>,
    viewModel: LibraryViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current

    if (itemsList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = strings.libraryScreen.emptyState,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    } else {
        val lazyListState = rememberLazyListState()
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(
                items = itemsList,
                key = { it.baseAnime.anilistId }
            ) { item ->
                WaitingAnimeCard(
                    item = item,
                    viewModel = viewModel,
                    navController = navController,
                    preferUk = preferUk
                )
            }
        }
    }
}

@Composable
fun WaitingAnimeCard(
    item: WaitingItem,
    viewModel: LibraryViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val candidate = item.candidateSequel

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBorder)
                        .clickable { navController.navigate(Screen.Detail(item.baseAnime.anilistId)) }
                ) {
                    val cover = item.baseAnime.coverLarge ?: item.baseAnime.coverExtraLarge
                    if (!cover.isNullOrEmpty()) {
                        AsyncImage(
                            model = cover,
                            contentDescription = item.baseAnime.getDisplayTitle(preferUk),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.baseAnime.getDisplayTitle(preferUk),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { navController.navigate(Screen.Detail(item.baseAnime.anilistId)) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val (badgeBg, badgeFg, badgeText) = when (item.candidateStatus) {
                            CandidateStatus.READY_TO_WATCH -> Triple(
                                CyberTeal.copy(alpha = 0.18f),
                                CyberTeal,
                                strings.libraryScreen.readyToWatch
                            )
                            CandidateStatus.RELEASED -> {
                                if (candidate?.isReleasing() == true) {
                                    Triple(
                                        CyberTeal.copy(alpha = 0.18f),
                                        CyberTeal,
                                        strings.libraryScreen.currentlyAiring
                                    )
                                } else {
                                    Triple(
                                        CyberTeal.copy(alpha = 0.18f),
                                        CyberTeal,
                                        strings.libraryScreen.sequelReleased
                                    )
                                }
                            }
                            CandidateStatus.ANNOUNCED -> Triple(
                                NeonCoral.copy(alpha = 0.18f),
                                NeonCoral,
                                strings.libraryScreen.sequelAnnounced
                            )
                            CandidateStatus.QUIET -> Triple(
                                Color.White.copy(alpha = 0.08f),
                                TextSecondary,
                                strings.libraryScreen.noAnnouncements
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeFg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val baseFormat = item.baseAnime.format
                    if (baseFormat != null) {
                        Text(
                            text = baseFormat,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (candidate != null && item.candidateStatus != CandidateStatus.QUIET) {
                val isReleasedOrReady = item.candidateStatus == CandidateStatus.READY_TO_WATCH || item.candidateStatus == CandidateStatus.RELEASED
                val highlightBg = if (isReleasedOrReady) CyberTeal.copy(alpha = 0.08f) else ElectricViolet.copy(alpha = 0.08f)
                val highlightBorder = if (isReleasedOrReady) CyberTeal.copy(alpha = 0.3f) else ElectricViolet.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(highlightBg)
                        .border(1.dp, highlightBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = candidate.getDisplayTitle(preferUk),
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { navController.navigate(Screen.Detail(candidate.anilistId)) }
                        )
                        val metaInfo = buildString {
                            val fmt = candidate.format
                            if (fmt != null) append("${strings.formats.getFormatLabel(fmt)} • ")
                            if (candidate.isReleasing()) {
                                val releasingLabel = strings.mediaStatuses.getMediaStatusLabel("RELEASING")
                                val airingEp = candidate.airingEpisode
                                val releasedEps = if (airingEp != null) {
                                    (airingEp - 1).coerceAtLeast(0)
                                } else null
                                val totalEps = candidate.episodes
                                when {
                                    releasedEps != null && releasedEps > 0 && totalEps != null -> {
                                        if (releasedEps == totalEps) {
                                            append("$releasingLabel • $totalEps ${strings.libraryScreen.eps}")
                                        } else {
                                            append("$releasingLabel • $releasedEps / $totalEps ${strings.libraryScreen.eps}")
                                        }
                                    }
                                    releasedEps != null && releasedEps > 0 && totalEps == null -> {
                                        append("$releasingLabel • $releasedEps / ? ${strings.libraryScreen.eps}")
                                    }
                                    totalEps != null -> {
                                        append("$releasingLabel • $totalEps ${strings.libraryScreen.eps}")
                                    }
                                    else -> {
                                        append(releasingLabel)
                                    }
                                }
                            } else if (candidate.status?.uppercase() == "FINISHED") {
                                if (candidate.episodes != null) {
                                    append("${candidate.episodes} ${strings.libraryScreen.eps}")
                                } else {
                                    append(strings.mediaStatuses.getMediaStatusLabel("FINISHED"))
                                }
                            } else if (candidate.isNotYetReleased()) {
                                val seasonLabel = candidate.season?.let { strings.seasons.getSeasonLabel(it) }
                                val yearLabel = candidate.seasonYear?.toString() ?: candidate.startDateYear?.toString()
                                val timeframe = listOfNotNull(seasonLabel, yearLabel).joinToString(" ")
                                if (timeframe.isNotBlank()) append(timeframe) else append(strings.mediaStatuses.getMediaStatusLabel("NOT_YET_RELEASED"))
                            } else {
                                val status = candidate.status
                                if (status != null) {
                                    append(strings.mediaStatuses.getMediaStatusLabel(status))
                                } else {
                                    val seasonLabel = candidate.season?.let { strings.seasons.getSeasonLabel(it) }
                                    val yearLabel = candidate.seasonYear?.toString() ?: candidate.startDateYear?.toString()
                                    val timeframe = listOfNotNull(seasonLabel, yearLabel).joinToString(" ")
                                    if (timeframe.isNotBlank()) append(timeframe) else if (candidate.episodes != null) append("${candidate.episodes} ${strings.libraryScreen.eps}")
                                }
                            }
                        }
                        Text(
                            text = metaInfo,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (candidate != null && item.candidateStatus != CandidateStatus.QUIET) {
                    val isReleasedOrReady = item.candidateStatus == CandidateStatus.READY_TO_WATCH || item.candidateStatus == CandidateStatus.RELEASED
                    val primaryLabel = if (isReleasedOrReady) strings.libraryScreen.addToWatching else strings.libraryScreen.addToPlanned
                    val primaryStatus = if (isReleasedOrReady) "CURRENT" else "PLANNING"
                    val primaryColor = statusConfigs.firstOrNull { it.id == primaryStatus }?.color
                        ?: if (isReleasedOrReady) Color(0xFF3B82F6) else Color(0xFF9067C6)

                    Button(
                        onClick = { viewModel.updateWatchStatus(candidate.anilistId, primaryStatus) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = primaryLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.dismissSequelCandidate(candidate.anilistId) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = strings.libraryScreen.hideSequel,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { viewModel.toggleWaitingStatus(item.baseAnime.anilistId, false) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = strings.libraryScreen.stopWaiting,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RatedAnimeListContent(
    animeList: List<Anime>,
    viewModel: LibraryViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current
    var animeForScoreEdit by remember { mutableStateOf<Anime?>(null) }
    val trackingEntitiesMap by viewModel.trackingEntitiesMap.collectAsState()

    if (animeList.isEmpty()) {
        val searchQuery by viewModel.searchQuery.collectAsState()
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) strings.libraryScreen.randomEmpty else strings.libraryScreen.noRatedAnime,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        val lazyGridState = rememberLazyGridState()
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .fillMaxSize()
                .disableSplitTouch()
        ) {
            items(
                items = animeList,
                key = { it.anilistId }
            ) { anime ->
                val trackingEntity = trackingEntitiesMap[anime.anilistId]
                val userScore = trackingEntity?.score ?: 0.0
                val watchStatus = trackingEntity?.watchStatus

                RatedAnimeCard(
                    anime = anime,
                    score = userScore,
                    watchStatus = watchStatus,
                    preferUk = preferUk,
                    onCardClick = {
                        navController.navigate(Screen.Detail(anime.anilistId))
                    },
                    onQuickScoreEdit = {
                        animeForScoreEdit = anime
                    }
                )
            }
        }
    }

    animeForScoreEdit?.let { targetAnime ->
        val currentTracking = trackingEntitiesMap[targetAnime.anilistId]
        QuickScoreDialog(
            animeTitle = targetAnime.getDisplayTitle(preferUk),
            currentScore = currentTracking?.score,
            onDismiss = { animeForScoreEdit = null },
            onSaveScore = { newScore ->
                if (newScore == null || newScore == 0.0) {
                    viewModel.updateScore(targetAnime.anilistId, 0.0)
                } else {
                    viewModel.updateScore(targetAnime.anilistId, newScore)
                }
                animeForScoreEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategorySelectionRotor(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    strings: moe.GetTheNya.AniForge.ui.localization.LocaleStrings,
    onItemClick: (LibraryFilter) -> Unit,
    categoryCounts: Map<LibraryFilter, Int> = emptyMap(),
    hasWaitingUpdates: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(240.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark.copy(alpha = 0.95f))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Center highlighted slot overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, CardBorder.copy(alpha = 0.5f))
        )

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(vertical = 72.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(LibraryFilter.values()) { idx, filter ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (filter == LibraryFilter.COLLECTIONS) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(1.dp)
                                .background(CardBorder.copy(alpha = 0.5f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                val layoutInfo = lazyListState.layoutInfo
                                val visibleItems = layoutInfo.visibleItemsInfo
                                val itemInfo = visibleItems.firstOrNull { it.index == idx }
                                if (itemInfo != null) {
                                    val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2f
                                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                                    val distanceFromCenter = kotlin.math.abs(itemCenter - viewportCenter)

                                    val maxDistance = ((layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f).coerceAtLeast(1f)
                                    val fraction = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                                    scaleX = 1f - 0.35f * fraction
                                    scaleY = 1f - 0.35f * fraction
                                    alpha = 1f - 0.7f * fraction
                                } else {
                                    alpha = 0.3f
                                    scaleX = 0.65f
                                    scaleY = 0.65f
                                }
                            }
                            .clickable {
                                onItemClick(filter)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = filter.getLabelWithCount(strings, categoryCounts[filter]),
                                color = filter.getColor(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (filter == LibraryFilter.WAITING && hasWaitingUpdates) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonCoral)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    navController: NavController,
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

    val selectedCollectionIds by viewModel.selectedCollectionIds.collectAsState()
    val isInSelectionMode by viewModel.isInSelectionMode.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val activeFilter by viewModel.activeFilter.collectAsState()
    val activeSectionData by viewModel.activeSectionData.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val listFilterState by viewModel.listFilterState.collectAsState()
    val hasWaitingUpdates by viewModel.hasWaitingUpdates.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val hasActiveFilters = remember(listFilterState) {
        listFilterState.genres.isNotEmpty() ||
        listFilterState.excludedGenres.isNotEmpty() ||
        listFilterState.formats.isNotEmpty() ||
        listFilterState.excludedFormats.isNotEmpty()
    }

    LaunchedEffect(activeFilter) {
        viewModel.clearSelection()
    }

    DisposableEffect(navController) {
        navController.onLibraryClick = {
            viewModel.setActiveFilter(LibraryFilter.WATCHING)
        }
        onDispose {
            navController.onLibraryClick = null
        }
    }

    var showRotor by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var isDraggingByLongPress by remember { mutableStateOf(false) }
    var showDragOverlay by remember { mutableStateOf(false) }
    val dragLazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var touchPosition by remember { mutableStateOf(Offset.Zero) }

    val dragCenterIndex by remember {
        derivedStateOf {
            getCenteredItemIndex(dragLazyListState)
        }
    }

    // Scroll to current active filter index when long press starts
    LaunchedEffect(showDragOverlay) {
        if (showDragOverlay) {
            val index = LibraryFilter.values().indexOf(activeFilter)
            if (index >= 0) {
                dragLazyListState.scrollToItem(index)
            }
        }
    }

    // Micro-haptic tick when center index changes during gesture drag
    LaunchedEffect(dragCenterIndex) {
        if (isDraggingByLongPress) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Sticky Header Column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                if (isInSelectionMode && activeFilter == LibraryFilter.COLLECTIONS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.libraryScreen.cancel,
                                    tint = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            val selectedText = strings.libraryScreen.selectedCount.getPlural(selectedCollectionIds.size)
                            Text(
                                text = selectedText,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = strings.libraryScreen.deleteCollection,
                                tint = NeonCoral
                            )
                        }
                    }
                } else {
                    Text(
                        text = strings.libraryScreen.name,
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val searchQuery by viewModel.searchQuery.collectAsState()
                val searchPlaceholder = if (activeFilter == LibraryFilter.COLLECTIONS) {
                    strings.libraryScreen.searchCollections
                } else {
                    strings.libraryScreen.searchPlaceholder
                }

                if (activeFilter != LibraryFilter.WAITING) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            LibrarySearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.searchQuery.value = it },
                                placeholder = searchPlaceholder,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (activeFilter != LibraryFilter.COLLECTIONS) {
                            val context = LocalContext.current
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
                                    val success = viewModel.pickRandomAnime(navController)
                                    if (!success) {
                                        Toast.makeText(context, strings.libraryScreen.randomEmpty, Toast.LENGTH_SHORT).show()
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
                    }
                }
            }

            AnimatedContent(
                targetState = activeFilter,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "library_filter_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { filter ->
                when (filter) {
                    LibraryFilter.COLLECTIONS -> {
                        // Collections Page
                        val collectionsList = (activeSectionData as? LibrarySectionData.Collections)?.list ?: emptyList()
                        var showCreateDialog by remember { mutableStateOf(false) }

                        if (showCreateDialog) {
                            CollectionFormDialog(
                                initialTitle = "",
                                initialDescription = "",
                                dialogTitle = strings.libraryScreen.newCollection,
                                confirmButtonText = strings.libraryScreen.create,
                                descriptionLabel = strings.libraryScreen.descriptionOptional,
                                onDismissRequest = { showCreateDialog = false },
                                onConfirm = { title, description ->
                                    viewModel.createCollection(title, description)
                                    showCreateDialog = false
                                }
                            )
                        }

                        val collectionsLazyGridState = rememberLazyGridState()
                        LazyVerticalGrid(
                            state = collectionsLazyGridState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 110.dp, start = 20.dp, end = 20.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .disableSplitTouch()
                        ) {
                            item {
                                CreationSlotCard(onClick = { showCreateDialog = true })
                            }
                            items(
                                items = collectionsList,
                                key = { it.collection.id }
                            ) { item ->
                                val collectionId = item.collection.id
                                val isSelected = selectedCollectionIds.contains(collectionId)
                                CollectionBentoCard(
                                    title = item.collection.title,
                                    description = item.collection.description,
                                    posters = item.posters,
                                    totalCount = item.totalCount,
                                    statusCounts = item.statusCounts,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isInSelectionMode) {
                                            viewModel.toggleCollectionSelection(collectionId)
                                        } else {
                                            navController.navigate(Screen.CollectionDetail(item.collection.id))
                                        }
                                    },
                                    onLongClick = {
                                        if (!isInSelectionMode) {
                                            viewModel.toggleCollectionSelection(collectionId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    LibraryFilter.WAITING -> {
                        val waitingList = (activeSectionData as? LibrarySectionData.Waiting)?.list ?: emptyList()
                        WaitingListContent(
                            itemsList = waitingList,
                            viewModel = viewModel,
                            navController = navController,
                            preferUk = preferUk
                        )
                    }
                    LibraryFilter.RATED -> {
                        val animeList = (activeSectionData as? LibrarySectionData.TrackedAnime)?.list ?: emptyList()
                        RatedAnimeListContent(
                            animeList = animeList,
                            viewModel = viewModel,
                            navController = navController,
                            preferUk = preferUk
                        )
                    }
                    else -> {
                        val animeList = (activeSectionData as? LibrarySectionData.TrackedAnime)?.list ?: emptyList()
                        UserTrackedListContent(
                            animeList = animeList,
                            viewModel = viewModel,
                            navController = navController,
                            preferUk = preferUk
                        )
                    }
                }
            }
        }

        // Floating Selector Overlay for Mode B (Continuous Drag Gesture)
        AnimatedVisibility(
            visible = showDragOverlay,
            enter = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.8f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 180.dp) // Float above the trigger pill / thumb position
                .zIndex(15f)
        ) {
            CategorySelectionRotor(
                lazyListState = dragLazyListState,
                strings = strings,
                categoryCounts = categoryCounts,
                hasWaitingUpdates = hasWaitingUpdates,
                onItemClick = { filter ->
                    viewModel.setActiveFilter(filter)
                    showDragOverlay = false
                }
            )
        }

        // Floating Glass Trigger Pill at bottom center
        if (!isInSelectionMode || activeFilter != LibraryFilter.COLLECTIONS) {
            val isPillPressed = isPressed || isDraggingByLongPress
            val pillScale by animateFloatAsState(
                targetValue = if (isPillPressed) 0.92f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "pill_scale"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp) // Float above bottom bar
                    .zIndex(10f)
            ) {
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pillScale
                            scaleY = pillScale
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SurfaceDark.copy(alpha = 0.8f),
                                    SurfaceDark.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .pointerInput(activeFilter) {
                            coroutineScope {
                                while (true) {
                                    val down = awaitPointerEventScope {
                                        awaitFirstDown(requireUnconsumed = false)
                                    }
                                    val touchSlop = viewConfiguration.touchSlop
                                    val initialTouchAnchor = down.position
                                    var prevPosition = down.position
                                    var isPressedLocal = true
                                    var isDragged = false
                                    isPressed = true
                                    isDraggingByLongPress = false
                                    showDragOverlay = false
                                    
                                    val timerJob = launch {
                                        delay(500L)
                                        if (isPressedLocal && !isDragged) {
                                            isDraggingByLongPress = true
                                            showDragOverlay = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    
                                    var isNormalRelease = false
                                    try {
                                        awaitPointerEventScope {
                                            val dragId = down.id
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Main)
                                                val dragEvent = event.changes.firstOrNull { it.id == dragId }
                                                if (dragEvent == null || !dragEvent.pressed) {
                                                    break
                                                }
                                                
                                                val position = dragEvent.position
                                                touchPosition = position
                                                
                                                val vector = position - initialTouchAnchor
                                                val distancePx = vector.getDistance()
                                                
                                                if (!isDraggingByLongPress && distancePx > touchSlop) {
                                                    timerJob.cancel()
                                                    isDragged = true
                                                }
                                                
                                                if (isDraggingByLongPress) {
                                                    dragEvent.consume()
                                                    val dy = position.y - prevPosition.y
                                                    dragLazyListState.dispatchRawDelta(dy)
                                                }
                                                prevPosition = position
                                            }
                                        }
                                        isNormalRelease = true
                                    } finally {
                                        timerJob.cancel()
                                        isPressed = false
                                        isPressedLocal = false
                                        
                                        if (isDraggingByLongPress) {
                                            isDraggingByLongPress = false
                                            showDragOverlay = false
                                            
                                            if (isNormalRelease) {
                                                val finalIndex = getCenteredItemIndex(dragLazyListState)
                                                val targetFilter = LibraryFilter.values()[finalIndex]
                                                viewModel.setActiveFilter(targetFilter)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        } else {
                                            if (isNormalRelease && !isDragged) {
                                                showRotor = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeFilter.getLabelWithCount(strings, categoryCounts[activeFilter]),
                        color = activeFilter.getColor(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = activeFilter.getColor(),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (hasWaitingUpdates) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-3).dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(NeonCoral)
                            .border(1.5.dp, BackgroundDark, CircleShape)
                    )
                }
            }
        }

        // Bottom-Sheet Ergonomic Selector Wheel
        if (showRotor) {
            var lastCenteredIndex by remember { mutableStateOf(LibraryFilter.values().indexOf(activeFilter).coerceAtLeast(0)) }
            var tempSelectedFilter by remember { mutableStateOf(activeFilter) }
            val lazyListState = rememberLazyListState()
            val haptic = LocalHapticFeedback.current

            // scroll to active filter when sheet opens
            LaunchedEffect(showRotor) {
                if (showRotor) {
                    val index = LibraryFilter.values().indexOf(activeFilter)
                    if (index >= 0) {
                        lazyListState.scrollToItem(index)
                    }
                }
            }

            // Trigger haptic click and update temp select on snap
            LaunchedEffect(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) {
                val layoutInfo = lazyListState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2f
                    val closestItem = visibleItems.minByOrNull {
                        val itemCenter = it.offset + it.size / 2f
                        kotlin.math.abs(itemCenter - viewportCenter)
                    }
                    closestItem?.let {
                        if (it.index != lastCenteredIndex) {
                            lastCenteredIndex = it.index
                            tempSelectedFilter = LibraryFilter.values()[it.index]
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
            }

            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.setActiveFilter(tempSelectedFilter)
                    showRotor = false
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = BackgroundDark,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0.dp) },
                modifier = Modifier.fillMaxWidth()
            ) {
                val customBottomBarHeight = 92.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = customBottomBarHeight)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val chooseCategoryText = strings.libraryScreen.chooseCategory ?: "Select Category"
                    Text(
                        text = chooseCategoryText,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    CategorySelectionRotor(
                        lazyListState = lazyListState,
                        strings = strings,
                        categoryCounts = categoryCounts,
                        hasWaitingUpdates = hasWaitingUpdates,
                        onItemClick = { filter ->
                            viewModel.setActiveFilter(filter)
                            showRotor = false
                        }
                    )
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = CardBorder,
                        shape = RoundedCornerShape(24.dp)
                    ),
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(strings.libraryScreen.deleteSelectedCollections, color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text(strings.libraryScreen.deleteSelectedConfirm, color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSelectedCollections()
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(strings.libraryScreen.deleteSelectedCollections, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(strings.libraryScreen.cancel, color = TextSecondary)
                    }
                },
                containerColor = AlertBackground,
                shape = RoundedCornerShape(24.dp)
            )
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
                filteredCount = (activeSectionData as? LibrarySectionData.TrackedAnime)?.list?.size ?: 0,
                onClearAllFilters = { viewModel.clearAllFilters() },
                onListSortOptionSelected = { viewModel.updateSortOrder(it) },
                onFormatToggled = { viewModel.toggleFormatType(it) },
                onGenreFilterToggled = { viewModel.toggleGenreFilterState(it) },
                onClearGenreFilters = { viewModel.clearGenreFilters() }
            )
        }
    }
}

@Composable
fun FranchiseBentoCard(
    item: FranchiseItem,
    preferUk: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val countText = strings.detailScreen.releasesCount.getPlural(item.releases.size)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Overlapping Cover Stack Box
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val covers = item.releases.mapNotNull { it.coverLarge }.take(3)
            
            if (covers.size >= 3) {
                // Cover 3 (deepest background, rotated left)
                AsyncImage(
                    model = covers[2],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .graphicsLayer {
                            translationX = -18.dp.toPx()
                            scaleX = 0.82f
                            scaleY = 0.82f
                            rotationZ = -12f
                            alpha = 0.5f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
                // Cover 2 (middle background, rotated right)
                AsyncImage(
                    model = covers[1],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .graphicsLayer {
                            translationX = 18.dp.toPx()
                            scaleX = 0.88f
                            scaleY = 0.88f
                            rotationZ = 12f
                            alpha = 0.8f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
                // Cover 1 (foreground)
                AsyncImage(
                    model = covers[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(52.dp)
                        .height(74.dp)
                        .graphicsLayer {
                            scaleX = 1.0f
                            scaleY = 1.0f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
            } else if (covers.size == 2) {
                // Cover 2 (background, rotated right)
                AsyncImage(
                    model = covers[1],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .graphicsLayer {
                            translationX = 16.dp.toPx()
                            scaleX = 0.88f
                            scaleY = 0.88f
                            rotationZ = 10f
                            alpha = 0.8f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
                // Cover 1 (foreground)
                AsyncImage(
                    model = covers[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(52.dp)
                        .height(74.dp)
                        .graphicsLayer {
                            scaleX = 1.0f
                            scaleY = 1.0f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
            } else if (covers.isNotEmpty()) {
                // Standard single cover fallback
                AsyncImage(
                    model = covers[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(52.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Side: Title & Badge
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.franchise.getDisplayName(preferUk) ?: "",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Releases badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1A8A2BE2))
                    .border(1.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countText,
                    color = CyberTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WindowsSegmentedProgressBar(
    totalCount: Int,
    statusCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressPulse by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                -0.5f at 0
                1.5f at 3000
                1.5f at 6000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .drawBehind {
                val width = size.width
                val height = size.height

                // Draw base background (dark grey)
                drawRect(color = Color(0xFF2A2A32), size = size)

                if (totalCount == 0) return@drawBehind

                val activeStatuses = statusConfigs.map { it.id }
                var currentX = 0f
                var filledWidth = 0f

                activeStatuses.forEach { statusId ->
                    val count = statusCounts[statusId] ?: 0
                    if (count > 0) {
                        val segmentWidth = (count.toFloat() / totalCount) * width
                        val color = AnimeStatusColors[statusId] ?: Color.Gray
                        drawRect(
                            color = color,
                            topLeft = Offset(currentX, 0f),
                            size = androidx.compose.ui.geometry.Size(segmentWidth, height)
                        )
                        currentX += segmentWidth
                        filledWidth += segmentWidth
                    }
                }

                // Draw Windows copier pulse on the filled segments
                if (filledWidth > 0f) {
                    val shimmerWidth = width * 0.8f
                    val startX = progressPulse * (filledWidth + shimmerWidth) - shimmerWidth
                    val endX = startX + shimmerWidth

                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(endX, 0f)
                    )

                    drawRect(
                        brush = shimmerBrush,
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(filledWidth, height)
                    )
                }
            }
    )
}

@Composable
fun CreationSlotCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark.copy(alpha = 0.3f))
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val color = ElectricViolet.copy(alpha = 0.5f)
                val pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(12f, 8f), 0f
                )
                val radius = 24.dp.toPx()
                drawRoundRect(
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = pathEffect
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = strings.libraryScreen.newCollection,
                tint = ElectricViolet,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.libraryScreen.newCollection,
                color = ElectricViolet,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionBentoCard(
    title: String,
    description: String,
    posters: List<String>,
    totalCount: Int,
    statusCounts: Map<String, Int>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectTintAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.0f,
        label = "selectTintAlpha"
    )
    val borderThickness by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        label = "borderThickness"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ElectricViolet else CardBorder,
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardDark)
            .border(borderThickness, borderColor, RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElectricViolet.copy(alpha = selectTintAlpha))
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(ElectricViolet)
                    .border(1.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 3D Fan-Out Poster Stack Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (posters.size >= 3) {
                    // Cover 3 (rotated left)
                    AsyncImage(
                        model = posters[2],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(55.dp)
                            .height(80.dp)
                            .graphicsLayer {
                                translationX = -22.dp.toPx()
                                scaleX = 0.82f
                                scaleY = 0.82f
                                rotationZ = -12f
                                alpha = 0.5f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                    // Cover 2 (rotated right)
                    AsyncImage(
                        model = posters[1],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(55.dp)
                            .height(80.dp)
                            .graphicsLayer {
                                translationX = 22.dp.toPx()
                                scaleX = 0.88f
                                scaleY = 0.88f
                                rotationZ = 12f
                                alpha = 0.8f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                    // Cover 1 (foreground)
                    AsyncImage(
                        model = posters[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .graphicsLayer {
                                scaleX = 1.0f
                                scaleY = 1.0f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                } else if (posters.size == 2) {
                    // Cover 2 (rotated right)
                    AsyncImage(
                        model = posters[1],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(55.dp)
                            .height(80.dp)
                            .graphicsLayer {
                                translationX = 18.dp.toPx()
                                scaleX = 0.88f
                                scaleY = 0.88f
                                rotationZ = 10f
                                alpha = 0.8f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                    // Cover 1 (foreground)
                    AsyncImage(
                        model = posters[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .graphicsLayer {
                                scaleX = 1.0f
                                scaleY = 1.0f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                } else if (posters.isNotEmpty()) {
                    AsyncImage(
                        model = posters[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                } else {
                    // Placeholder card
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ElectricViolet.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Lower half: Titles & Progress
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
                Text(
                    text = if (description.isNotBlank()) description else strings.libraryScreen.noDescription,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titlesText = strings.libraryScreen.titlesCount.getPlural(totalCount)
                    Text(
                        text = titlesText,
                        color = ElectricViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                WindowsSegmentedProgressBar(
                    totalCount = totalCount,
                    statusCounts = statusCounts,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = TextSecondary) },
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
fun PinnedAiringSection(
    readyToWatchItems: List<WaitingItem>,
    onItemClick: (Long) -> Unit,
    onStartWatching: (Long) -> Unit,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, CyberTeal.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CyberTeal)
                )
                Text(
                    text = strings.libraryScreen.airingWaitedSequels,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CyberTeal.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${readyToWatchItems.size}",
                        color = CyberTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = readyToWatchItems,
                    key = { it.candidateSequel?.anilistId ?: it.baseAnime.anilistId }
                ) { item ->
                    val candidate = item.candidateSequel ?: return@items
                    val airedEpisodes = candidate.getReleasedEpisodes() ?: 1
                    val totalEpisodes = candidate.episodes?.toString() ?: "?"
                    val statusBadgeText = "🟢 Ep $airedEpisodes/$totalEpisodes"

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .width(250.dp)
                            .clickable { onItemClick(candidate.anilistId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp, 76.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardBorder)
                            ) {
                                val cover = candidate.coverLarge ?: item.baseAnime.coverLarge
                                if (!cover.isNullOrEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = cover,
                                        contentDescription = candidate.getDisplayTitle(preferUk),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = statusBadgeText,
                                    color = CyberTeal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = candidate.getDisplayTitle(preferUk),
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "Base: ${item.baseAnime.getDisplayTitle(preferUk)}",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Button(
                                    onClick = { onStartWatching(candidate.anilistId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .height(28.dp)
                                ) {
                                    Text(
                                        text = strings.libraryScreen.addToWatching,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
