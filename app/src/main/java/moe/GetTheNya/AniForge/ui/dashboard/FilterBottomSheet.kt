package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.Studio
import moe.GetTheNya.AniForge.core.model.Staff
import moe.GetTheNya.AniForge.core.model.SortOption
import moe.GetTheNya.AniForge.core.model.AnimeFormat
import moe.GetTheNya.AniForge.core.model.EpisodeGroup
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.ListSortOption
import moe.GetTheNya.AniForge.core.model.ListFilterState
import moe.GetTheNya.AniForge.ui.localization.getMediaStatusLabel
import moe.GetTheNya.AniForge.ui.localization.getMediaSourceLabel
import moe.GetTheNya.AniForge.ui.theme.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalView

private enum class SortCategory {
    RELEVANCE, SCORE, POPULARITY, RELEASE_DATE, EPISODES, TITLE
}

private enum class ListSortCategory {
    DATE_ADDED, SCORE, PROGRESS, ALPHABETICAL
}

@Composable
fun FilterBottomSheet(
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val genres by viewModel.allGenres.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val studios by viewModel.allStudios.collectAsState()
    val staff by viewModel.allStaff.collectAsState()
    val preferUkTitles by viewModel.preferUkTitles.collectAsState()
    val filter by viewModel.searchFilter.collectAsState()
    val count by viewModel.filteredCount.collectAsState()

    FilterBottomSheet(
        isCatalog = true,
        onDismiss = onDismiss,
        allGenres = genres,
        allTags = tags,
        allStudios = studios,
        allStaff = staff,
        preferUkTitles = preferUkTitles,
        catalogFilter = filter,
        filteredCount = count,
        onClearAllFilters = { viewModel.clearAllFilters() },
        onSortOptionSelected = { viewModel.updateSortOrder(it) },
        onScoreRangeChanged = { min, max -> viewModel.updateScoreRange(min, max) },
        onFormatToggled = { viewModel.toggleFormatType(it) },
        onEpisodeGroupToggled = { viewModel.toggleEpisodeGroup(it) },
        onTrackingStatusToggled = { viewModel.toggleTrackingStatus(it) },
        onMediaStatusToggled = { viewModel.toggleMediaStatus(it) },
        onMediaSourceToggled = { viewModel.toggleMediaSource(it) },
        onClearMediaStatusFilters = { viewModel.clearMediaStatusFilters() },
        onClearMediaSourceFilters = { viewModel.clearMediaSourceFilters() },
        onUkTranslationFilterToggled = { viewModel.toggleUkTranslationFilter() },
        onStudioFilterToggled = { viewModel.toggleStudioFilter(it) },
        onGenreFilterToggled = { viewModel.toggleGenreFilterState(it) },
        onTagFilterToggled = { viewModel.toggleTagFilterState(it) },
        onStaffFilterToggled = { viewModel.toggleStaffFilterState(it) },
        onClearGenreFilters = { viewModel.clearGenreFilters() },
        onClearTagFilters = { viewModel.clearTagFilters() },
        onClearStudioFilters = { viewModel.clearStudioFilters() },
        onClearStaffFilters = { viewModel.clearStaffFilters() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    isCatalog: Boolean,
    onDismiss: () -> Unit,
    allGenres: List<Genre>,
    allTags: List<Tag>,
    allStudios: List<Studio>,
    allStaff: List<Staff>,
    preferUkTitles: Boolean,
    catalogFilter: SearchFilterQuery? = null,
    trackedListFilter: ListFilterState? = null,
    filteredCount: Int,
    onClearAllFilters: () -> Unit,
    onSortOptionSelected: ((SortOption) -> Unit)? = null,
    onListSortOptionSelected: ((ListSortOption) -> Unit)? = null,
    onScoreRangeChanged: ((Double?, Double?) -> Unit)? = null,
    onFormatToggled: (AnimeFormat) -> Unit = {},
    onEpisodeGroupToggled: ((EpisodeGroup) -> Unit)? = null,
    onTrackingStatusToggled: ((String) -> Unit)? = null,
    onMediaStatusToggled: ((String) -> Unit)? = null,
    onMediaSourceToggled: ((String) -> Unit)? = null,
    onClearMediaStatusFilters: (() -> Unit)? = null,
    onClearMediaSourceFilters: (() -> Unit)? = null,
    onUkTranslationFilterToggled: (() -> Unit)? = null,
    onStudioFilterToggled: ((Long) -> Unit)? = null,
    onGenreFilterToggled: (String) -> Unit = {},
    onTagFilterToggled: ((Long) -> Unit)? = null,
    onStaffFilterToggled: ((Long) -> Unit)? = null,
    onClearGenreFilters: () -> Unit = {},
    onClearTagFilters: (() -> Unit)? = null,
    onClearStudioFilters: (() -> Unit)? = null,
    onClearStaffFilters: (() -> Unit)? = null,
    isSharedProfile: Boolean = false,
    friendStatusFilters: Map<String, Int> = emptyMap(),
    localStatusFilters: Map<String, Int> = emptyMap(),
    onFriendStatusFilterToggled: ((String) -> Unit)? = null,
    onLocalStatusFilterToggled: ((String) -> Unit)? = null
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    
    var isDraggingSlider by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val genresList = if (isCatalog) catalogFilter?.genres.orEmpty() else trackedListFilter?.genres.orEmpty()
    val excludedGenresList = if (isCatalog) catalogFilter?.excludedGenres.orEmpty() else trackedListFilter?.excludedGenres.orEmpty()
    val inlineGenres = remember(genresList, excludedGenresList, allGenres) {
        val selectedGenres = allGenres.filter { genresList.contains(it.slug) || excludedGenresList.contains(it.slug) }
        val top8Genres = allGenres.take(8)
        (selectedGenres + top8Genres).distinct()
    }

    val tagsList = if (isCatalog) catalogFilter?.tags.orEmpty() else emptyList()
    val excludedTagsList = if (isCatalog) catalogFilter?.excludedTags.orEmpty() else emptyList()
    val inlineTags = remember(tagsList, excludedTagsList, allTags) {
        val selectedTags = allTags.filter { tagsList.contains(it.tagId) || excludedTagsList.contains(it.tagId) }
        val top8Tags = allTags.take(8)
        (selectedTags + top8Tags).distinct()
    }

    val studiosList = if (isCatalog) catalogFilter?.studios.orEmpty() else emptyList()
    val excludedStudiosList = if (isCatalog) catalogFilter?.excludedStudios.orEmpty() else emptyList()
    val inlineStudios = remember(studiosList, excludedStudiosList, allStudios) {
        val selectedStudios = allStudios.filter { studiosList.contains(it.studioId) || excludedStudiosList.contains(it.studioId) }
        val top8Studios = allStudios.take(8)
        (selectedStudios + top8Studios).distinct()
    }

    val staffList = if (isCatalog) catalogFilter?.staff.orEmpty() else emptyList()
    val excludedStaffList = if (isCatalog) catalogFilter?.excludedStaff.orEmpty() else emptyList()
    val inlineStaff = remember(staffList, excludedStaffList, allStaff) {
        val selectedStaff = allStaff.filter { staffList.contains(it.staffId) || excludedStaffList.contains(it.staffId) }
        val top8Staff = allStaff.take(8)
        (selectedStaff + top8Staff).distinct()
    }

    var showGenreDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showStudioDialog by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }

    val readyText = strings.dashboardScreen.readyWithCount.replace("{count}", filteredCount.toString())

    // Remember the lazy list state to track scroll position
    val lazyListState = rememberLazyListState()
    
    // Track whether the current touch gesture started while the list was at the top
    var gestureStartedAtTop by remember { mutableStateOf(true) }
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            gestureStartedAtTop = lazyListState.firstVisibleItemIndex == 0 &&
                    lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    // Intercept nested scroll from scrollable content to enforce 2-step discrete scrolling
    val bottomSheetScrollConnection = remember(isDraggingSlider) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isDraggingSlider) {
                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isDraggingSlider) {
                    return available
                }
                return Velocity.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isDraggingSlider) return Offset.Zero
                var consumedY = 0f
                if (available.y < 0f) {
                    consumedY = available.y
                    if (source == NestedScrollSource.SideEffect) {
                        scope.launch {
                            try {
                                lazyListState.scrollToItem(
                                    lazyListState.firstVisibleItemIndex,
                                    lazyListState.firstVisibleItemScrollOffset
                                )
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                } else if (available.y > 0f && !gestureStartedAtTop) {
                    consumedY = available.y
                    if (source == NestedScrollSource.SideEffect) {
                        scope.launch {
                            try {
                                lazyListState.scrollToItem(
                                    lazyListState.firstVisibleItemIndex,
                                    lazyListState.firstVisibleItemScrollOffset
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
                if (isDraggingSlider) return Velocity.Zero
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
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0.dp) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
                .nestedScroll(bottomSheetScrollConnection)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.dashboardScreen.filterHubTitle,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(
                    onClick = onClearAllFilters
                ) {
                    Text(
                        text = strings.dashboardScreen.clearAll,
                        color = NeonCoral,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
            
            LazyColumn(
                state = lazyListState,
                userScrollEnabled = !isDraggingSlider,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (isSharedProfile) {
                    item {
                        Column {
                            Text(
                                text = strings.socialScreen.friendFiltersLabel,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statusOptions = listOf(
                                    "CURRENT" to strings.misc.watching,
                                    "PLANNING" to strings.misc.planning,
                                    "COMPLETED" to strings.misc.completed,
                                    "PAUSED" to strings.misc.paused,
                                    "DROPPED" to strings.misc.dropped
                                )
                                for ((statusId, label) in statusOptions) {
                                    val filterVal = friendStatusFilters[statusId] ?: 0
                                    TriStateChip(
                                        label = label,
                                        isIncluded = filterVal == 1,
                                        isExcluded = filterVal == 2,
                                        onClick = { onFriendStatusFilterToggled?.invoke(statusId) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Text(
                                text = strings.socialScreen.localUserFiltersLabel,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statusOptions = listOf(
                                    "CURRENT" to strings.misc.watching,
                                    "PLANNING" to strings.misc.planning,
                                    "COMPLETED" to strings.misc.completed,
                                    "PAUSED" to strings.misc.paused,
                                    "DROPPED" to strings.misc.dropped
                                )
                                for ((statusId, label) in statusOptions) {
                                    val filterVal = localStatusFilters[statusId] ?: 0
                                    TriStateChip(
                                        label = label,
                                        isIncluded = filterVal == 1,
                                        isExcluded = filterVal == 2,
                                        onClick = { onLocalStatusFilterToggled?.invoke(statusId) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        // 1. Sort By
                        Column {
                            Text(text = strings.dashboardScreen.sortBy, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (isCatalog && catalogFilter != null) {
                            val activeSort = catalogFilter.sortBy
                            val (activeCategory, isDescending) = when (activeSort) {
                                SortOption.RELEVANCE -> SortCategory.RELEVANCE to true
                                SortOption.SCORE -> SortCategory.SCORE to true
                                SortOption.SCORE_ASC -> SortCategory.SCORE to false
                                SortOption.POPULARITY -> SortCategory.POPULARITY to true
                                SortOption.POPULARITY_ASC -> SortCategory.POPULARITY to false
                                SortOption.START_DATE_DESC, SortOption.YEAR_DESC -> SortCategory.RELEASE_DATE to true
                                SortOption.START_DATE_ASC, SortOption.YEAR_ASC -> SortCategory.RELEASE_DATE to false
                                SortOption.EPISODES_DESC -> SortCategory.EPISODES to true
                                SortOption.EPISODES_ASC -> SortCategory.EPISODES to false
                                SortOption.TITLE_DESC -> SortCategory.TITLE to true
                                SortOption.TITLE -> SortCategory.TITLE to false
                            }
                            
                            val onSortCategoryClick = { category: SortCategory ->
                                val newSortOption = if (activeCategory == category) {
                                    if (category == SortCategory.RELEVANCE) {
                                        SortOption.RELEVANCE
                                    } else if (isDescending) {
                                        when (category) {
                                            SortCategory.RELEVANCE -> SortOption.RELEVANCE
                                            SortCategory.SCORE -> SortOption.SCORE_ASC
                                            SortCategory.POPULARITY -> SortOption.POPULARITY_ASC
                                            SortCategory.RELEASE_DATE -> SortOption.START_DATE_ASC
                                            SortCategory.EPISODES -> SortOption.EPISODES_ASC
                                            SortCategory.TITLE -> SortOption.TITLE
                                        }
                                    } else {
                                        SortOption.SCORE
                                    }
                                } else {
                                    when (category) {
                                        SortCategory.RELEVANCE -> SortOption.RELEVANCE
                                        SortCategory.SCORE -> SortOption.SCORE
                                        SortCategory.POPULARITY -> SortOption.POPULARITY
                                        SortCategory.RELEASE_DATE -> SortOption.START_DATE_DESC
                                        SortCategory.EPISODES -> SortOption.EPISODES_DESC
                                        SortCategory.TITLE -> SortOption.TITLE_DESC
                                    }
                                }
                                onSortOptionSelected?.invoke(newSortOption)
                            }

                            val sortCategories = listOf(
                                SortCategory.SCORE to strings.dashboardScreen.scoreRange,
                                SortCategory.POPULARITY to strings.dashboardScreen.popularity,
                                SortCategory.RELEASE_DATE to strings.dashboardScreen.releaseDate,
                                SortCategory.EPISODES to strings.dashboardScreen.episodeCount,
                                SortCategory.TITLE to strings.dashboardScreen.alphabetical
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (catalogFilter.textQuery.isNotBlank()) {
                                    val isRelevanceActive = activeSort == SortOption.RELEVANCE
                                    FilterChip(
                                        selected = isRelevanceActive,
                                        onClick = { onSortCategoryClick(SortCategory.RELEVANCE) },
                                        label = { Text(strings.dashboardScreen.byRelevance, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricViolet,
                                            selectedLabelColor = Color.White,
                                            containerColor = SurfaceDark,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isRelevanceActive,
                                            borderColor = CardBorder,
                                            selectedBorderColor = ElectricViolet,
                                            borderWidth = 1.dp
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                for ((category, label) in sortCategories) {
                                    val isActive = activeCategory == category
                                    
                                    FilterChip(
                                        selected = isActive,
                                        onClick = { onSortCategoryClick(category) },
                                        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        leadingIcon = if (isActive) {
                                            {
                                                Icon(
                                                    imageVector = if (isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricViolet,
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White,
                                            containerColor = SurfaceDark,
                                            labelColor = TextSecondary,
                                            iconColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isActive,
                                            borderColor = CardBorder,
                                            selectedBorderColor = ElectricViolet,
                                            borderWidth = 1.dp
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        } else if (!isCatalog && trackedListFilter != null) {
                            val activeSort = trackedListFilter.sortBy
                            val (activeCategory, isDescending) = when (activeSort) {
                                ListSortOption.SCORE_DESC -> ListSortCategory.SCORE to true
                                ListSortOption.SCORE_ASC -> ListSortCategory.SCORE to false
                                ListSortOption.PROGRESS_DESC -> ListSortCategory.PROGRESS to true
                                ListSortOption.PROGRESS_ASC -> ListSortCategory.PROGRESS to false
                                ListSortOption.DATE_ADDED_DESC -> ListSortCategory.DATE_ADDED to true
                                ListSortOption.DATE_ADDED_ASC -> ListSortCategory.DATE_ADDED to false
                                ListSortOption.ALPHABETICAL_ASC -> ListSortCategory.ALPHABETICAL to false
                                ListSortOption.ALPHABETICAL_DESC -> ListSortCategory.ALPHABETICAL to true
                            }
                            
                            val onListSortCategoryClick = { category: ListSortCategory ->
                                val newSortOption = if (activeCategory == category) {
                                    if (isDescending) {
                                        when (category) {
                                            ListSortCategory.SCORE -> ListSortOption.SCORE_ASC
                                            ListSortCategory.PROGRESS -> ListSortOption.PROGRESS_ASC
                                            ListSortCategory.DATE_ADDED -> ListSortOption.DATE_ADDED_ASC
                                            ListSortCategory.ALPHABETICAL -> ListSortOption.ALPHABETICAL_ASC
                                        }
                                    } else {
                                        when (category) {
                                            ListSortCategory.SCORE -> ListSortOption.SCORE_DESC
                                            ListSortCategory.PROGRESS -> ListSortOption.PROGRESS_DESC
                                            ListSortCategory.DATE_ADDED -> ListSortOption.DATE_ADDED_DESC
                                            ListSortCategory.ALPHABETICAL -> ListSortOption.ALPHABETICAL_DESC
                                        }
                                    }
                                } else {
                                    when (category) {
                                        ListSortCategory.SCORE -> ListSortOption.SCORE_DESC
                                        ListSortCategory.PROGRESS -> ListSortOption.PROGRESS_DESC
                                        ListSortCategory.DATE_ADDED -> ListSortOption.DATE_ADDED_DESC
                                        ListSortCategory.ALPHABETICAL -> ListSortOption.ALPHABETICAL_ASC
                                    }
                                }
                                onListSortOptionSelected?.invoke(newSortOption)
                            }

                            val sortCategories = listOf(
                                ListSortCategory.DATE_ADDED to strings.libraryScreen.sortByDateAdded,
                                ListSortCategory.SCORE to strings.libraryScreen.sortByPersonalScore,
                                ListSortCategory.PROGRESS to strings.libraryScreen.sortByProgress,
                                ListSortCategory.ALPHABETICAL to strings.libraryScreen.sortByAlphabetical
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for ((category, label) in sortCategories) {
                                    val isActive = activeCategory == category
                                    
                                    FilterChip(
                                        selected = isActive,
                                        onClick = { onListSortCategoryClick(category) },
                                        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                        leadingIcon = if (isActive) {
                                            {
                                                Icon(
                                                    imageVector = if (isDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricViolet,
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White,
                                            containerColor = SurfaceDark,
                                            labelColor = TextSecondary,
                                            iconColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isActive,
                                            borderColor = CardBorder,
                                            selectedBorderColor = ElectricViolet,
                                            borderWidth = 1.dp
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isCatalog) {
                    item {
                        // 2. Score Range Slider
                        Column {
                            val currentMin = catalogFilter?.minScore ?: 0.0
                            val currentMax = catalogFilter?.maxScore ?: 10.0
                            var sliderPosition by remember(currentMin, currentMax) { 
                                mutableStateOf(currentMin.toFloat()..currentMax.toFloat()) 
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = strings.dashboardScreen.scoreRange, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f - %.1f", sliderPosition.start, sliderPosition.endInclusive),
                                    color = NeonCoral,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            RangeSlider(
                                value = sliderPosition,
                                onValueChange = { range ->
                                    val startRounded = Math.round(range.start * 10f) / 10f
                                    val endRounded = Math.round(range.endInclusive * 10f) / 10f
                                    sliderPosition = startRounded..endRounded
                                },
                                valueRange = 0f..10f,
                                onValueChangeFinished = {
                                    val min = if (sliderPosition.start <= 0.05f) null else sliderPosition.start.toDouble()
                                    val max = if (sliderPosition.endInclusive >= 9.95f) null else sliderPosition.endInclusive.toDouble()
                                    onScoreRangeChanged?.invoke(min, max)
                                },
                                colors = SliderDefaults.colors(
                                    activeTrackColor = ElectricViolet,
                                    inactiveTrackColor = CardBorder,
                                    thumbColor = ElectricViolet
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            view.parent?.requestDisallowInterceptTouchEvent(true)
                                            isDraggingSlider = true
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val anyPressed = event.changes.any { it.pressed }
                                                if (!anyPressed) break
                                                
                                                event.changes.forEach { change ->
                                                    if (change.positionChanged()) {
                                                        change.consume()
                                                    }
                                                }
                                            }
                                            isDraggingSlider = false
                                        }
                                    }
                            )
                        }
                    }
                }

                item {
                    // 3. Format Selection
                    Column {
                        Text(text = strings.dashboardScreen.format, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeFormats = if (isCatalog) catalogFilter?.formats.orEmpty() else trackedListFilter?.formats.orEmpty()
                            val activeExcludedFormats = if (isCatalog) catalogFilter?.excludedFormats.orEmpty() else trackedListFilter?.excludedFormats.orEmpty()
                            for (format in AnimeFormat.entries) {
                                val isIncluded = activeFormats.contains(format)
                                val isExcluded = activeExcludedFormats.contains(format)
                                TriStateChip(
                                    label = getAnimeFormatLabel(format),
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { onFormatToggled(format) }
                                )
                            }
                        }
                    }
                }

                if (isCatalog) {
                    item {
                        // 4. Episode Count groups
                        Column {
                            Text(text = strings.dashboardScreen.episodeCount, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val activeGroups = catalogFilter?.episodeGroups.orEmpty()
                                val activeExcludedGroups = catalogFilter?.excludedEpisodeGroups.orEmpty()
                                for (group in EpisodeGroup.entries) {
                                    val isIncluded = activeGroups.contains(group)
                                    val isExcluded = activeExcludedGroups.contains(group)
                                    val label = getEpisodeGroupLabel(group)
                                    TriStateChip(
                                        label = label,
                                        isIncluded = isIncluded,
                                        isExcluded = isExcluded,
                                        onClick = { onEpisodeGroupToggled?.invoke(group) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // 5. Active Lists status & Untracked
                        Column {
                            Text(text = strings.dashboardScreen.trackingStatus, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statusOptions = listOf(
                                    "CURRENT" to strings.misc.watching,
                                    "PLANNING" to strings.misc.planning,
                                    "COMPLETED" to strings.misc.completed,
                                    "PAUSED" to strings.misc.paused,
                                    "DROPPED" to strings.misc.dropped
                                )
                                val activeStatuses = catalogFilter?.trackingStatuses.orEmpty()
                                val activeExcludedStatuses = catalogFilter?.excludedTrackingStatuses.orEmpty()
                                for ((statusId, label) in statusOptions) {
                                    val isIncluded = activeStatuses.contains(statusId)
                                    val isExcluded = activeExcludedStatuses.contains(statusId)
                                    TriStateChip(
                                        label = label,
                                        isIncluded = isIncluded,
                                        isExcluded = isExcluded,
                                        onClick = { onTrackingStatusToggled?.invoke(statusId) }
                                    )
                                }
                            }
                        }
                    }

                    if (isCatalog && catalogFilter != null) {
                        // 5.1 Media release status selection
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = strings.dashboardScreen.releaseStatus, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val statusOptions = listOf(
                                        "RELEASING",
                                        "FINISHED",
                                        "HIATUS",
                                        "NOT_YET_RELEASED",
                                        "CANCELLED"
                                    )
                                    val activeStatuses = catalogFilter.mediaStatuses
                                    val activeExcludedStatuses = catalogFilter.excludedMediaStatuses
                                    for (status in statusOptions) {
                                        val isIncluded = activeStatuses.contains(status)
                                        val isExcluded = activeExcludedStatuses.contains(status)
                                        val label = strings.mediaStatuses.getMediaStatusLabel(status)
                                        TriStateChip(
                                            label = label,
                                            isIncluded = isIncluded,
                                            isExcluded = isExcluded,
                                            onClick = { onMediaStatusToggled?.invoke(status) }
                                        )
                                    }
                                }
                            }
                        }

                        // 5.2 Media source selection
                        item {
                            var showSourceDialog by remember { mutableStateOf(false) }
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = strings.dashboardScreen.sourceMaterial, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    TextButton(
                                        onClick = { showSourceDialog = true }
                                    ) {
                                        Text(
                                            text = strings.dashboardScreen.viewAll,
                                            color = ElectricViolet,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val inlineSources = listOf(
                                        "MANGA",
                                        "LIGHT_NOVEL",
                                        "ORIGINAL",
                                        "VISUAL_NOVEL"
                                    )
                                    val activeSources = catalogFilter.mediaSources
                                    val activeExcludedSources = catalogFilter.excludedMediaSources
                                    for (source in inlineSources) {
                                        val isIncluded = activeSources.contains(source)
                                        val isExcluded = activeExcludedSources.contains(source)
                                        val label = strings.mediaSources.getMediaSourceLabel(source)
                                        TriStateChip(
                                            label = label,
                                            isIncluded = isIncluded,
                                            isExcluded = isExcluded,
                                            onClick = { onMediaSourceToggled?.invoke(source) }
                                        )
                                    }
                                }
                            }

                            if (showSourceDialog) {
                                val allSources = listOf(
                                    "MANGA", "LIGHT_NOVEL", "ORIGINAL", "VISUAL_NOVEL",
                                    "VIDEO_GAME", "OTHER", "NOVEL", "DOUJINSHI", "ANIME",
                                    "WEB_NOVEL", "LIVE_ACTION", "GAME", "COMIC", "MULTIMEDIA_PROJECT",
                                    "PICTURE_BOOK"
                                )
                                TaxonomySelectionSheet<String>(
                                    title = strings.dashboardScreen.allSources,
                                    items = allSources,
                                    isIncluded = { catalogFilter.mediaSources.contains(it) },
                                    isExcluded = { catalogFilter.excludedMediaSources.contains(it) },
                                    onClick = { onMediaSourceToggled?.invoke(it) },
                                    onClear = { onClearMediaSourceFilters?.invoke() },
                                    getLabel = { strings.mediaSources.getMediaSourceLabel(it) },
                                    getItemSearchText = { it + " " + strings.mediaSources.getMediaSourceLabel(it) },
                                    onDismiss = { showSourceDialog = false }
                                )
                            }
                        }
                    }

                    if (preferUkTitles) {
                        item {
                            // 6. Localization toggle
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                        .clickable { onUkTranslationFilterToggled?.invoke() }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = strings.dashboardScreen.hasTranslation, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(text = strings.dashboardScreen.hasTranslationDesc, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = catalogFilter?.hasUkTranslation == true,
                                        onCheckedChange = { onUkTranslationFilterToggled?.invoke() },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ElectricViolet,
                                            checkedTrackColor = ElectricViolet.copy(alpha = 0.5f),
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = SurfaceDark
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // 7. Production Studio selection
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = strings.dashboardScreen.studios, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = { showStudioDialog = true }
                                ) {
                                    Text(
                                        text = strings.dashboardScreen.viewAll,
                                        color = ElectricViolet,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (studio in inlineStudios) {
                                    val isIncluded = catalogFilter?.studios?.contains(studio.studioId) == true
                                    val isExcluded = catalogFilter?.excludedStudios?.contains(studio.studioId) == true
                                    TriStateChip(
                                        label = studio.name,
                                        isIncluded = isIncluded,
                                        isExcluded = isExcluded,
                                        onClick = { onStudioFilterToggled?.invoke(studio.studioId) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // 8. Dual-state Genre Negation Filter
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = strings.dashboardScreen.genres, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { showGenreDialog = true }
                            ) {
                                Text(
                                    text = strings.dashboardScreen.viewAll,
                                    color = ElectricViolet,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (genre in inlineGenres) {
                                val isIncluded = genresList.contains(genre.slug)
                                val isExcluded = excludedGenresList.contains(genre.slug)
                                TriStateChip(
                                    label = genre.getDisplayName(preferUk = preferUkTitles),
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { onGenreFilterToggled(genre.slug) }
                                )
                            }
                        }
                    }
                }

                if (isCatalog) {
                    item {
                        // 9. Dual-state Tag Negation Filter
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = strings.dashboardScreen.tags, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = { showTagDialog = true }
                                ) {
                                    Text(
                                        text = strings.dashboardScreen.viewAll,
                                        color = ElectricViolet,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (tag in inlineTags) {
                                    val isIncluded = catalogFilter?.tags?.contains(tag.tagId) == true
                                    val isExcluded = catalogFilter?.excludedTags?.contains(tag.tagId) == true
                                    TriStateChip(
                                        label = tag.getDisplayName(preferUk = preferUkTitles),
                                        isIncluded = isIncluded,
                                        isExcluded = isExcluded,
                                        onClick = { onTagFilterToggled?.invoke(tag.tagId) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // 10. Dual-state Staff Negation Filter
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = strings.detailScreen.staffTitle, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = { showStaffDialog = true }
                                ) {
                                    Text(
                                        text = strings.dashboardScreen.viewAll,
                                        color = ElectricViolet,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (member in inlineStaff) {
                                    val isIncluded = catalogFilter?.staff?.contains(member.staffId) == true
                                    val isExcluded = catalogFilter?.excludedStaff?.contains(member.staffId) == true
                                    TriStateChip(
                                        label = member.fullName,
                                        isIncluded = isIncluded,
                                        isExcluded = isExcluded,
                                        onClick = { onStaffFilterToggled?.invoke(member.staffId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
            
            // Footer Apply Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .height(52.dp)
                ) {
                    Text(text = readyText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // Secondary Overlays for large taxonomies
    if (showGenreDialog) {
        TaxonomySelectionSheet<Genre>(
            title = strings.dashboardScreen.allGenres,
            items = allGenres,
            isIncluded = { genre -> genresList.contains(genre.slug) },
            isExcluded = { genre -> excludedGenresList.contains(genre.slug) },
            onClick = { onGenreFilterToggled(it.slug) },
            onClear = onClearGenreFilters,
            getLabel = { it.getDisplayName(preferUk = preferUkTitles) },
            getItemSearchText = { it.getDisplayName(preferUk = preferUkTitles) + " " + it.slug },
            onDismiss = { showGenreDialog = false }
        )
    }

    if (showTagDialog && onTagFilterToggled != null && onClearTagFilters != null) {
        TaxonomySelectionSheet<Tag>(
            title = strings.dashboardScreen.allTags,
            items = allTags,
            isIncluded = { catalogFilter?.tags?.contains(it.tagId) == true },
            isExcluded = { catalogFilter?.excludedTags?.contains(it.tagId) == true },
            onClick = { onTagFilterToggled(it.tagId) },
            onClear = onClearTagFilters,
            getLabel = { it.getDisplayName(preferUk = preferUkTitles) },
            getItemSearchText = { it.getDisplayName(preferUk = preferUkTitles) },
            onDismiss = { showTagDialog = false }
        )
    }

    if (showStudioDialog && onStudioFilterToggled != null && onClearStudioFilters != null) {
        TaxonomySelectionSheet<Studio>(
            title = strings.dashboardScreen.allStudios,
            items = allStudios,
            isIncluded = { catalogFilter?.studios?.contains(it.studioId) == true },
            isExcluded = { catalogFilter?.excludedStudios?.contains(it.studioId) == true },
            onClick = { onStudioFilterToggled(it.studioId) },
            onClear = onClearStudioFilters,
            getLabel = { it.name },
            getItemSearchText = { it.name },
            onDismiss = { showStudioDialog = false }
        )
    }

    if (showStaffDialog && onStaffFilterToggled != null && onClearStaffFilters != null) {
        TaxonomySelectionSheet<Staff>(
            title = strings.dashboardScreen.allStaff,
            items = allStaff,
            isIncluded = { catalogFilter?.staff?.contains(it.staffId) == true },
            isExcluded = { catalogFilter?.excludedStaff?.contains(it.staffId) == true },
            onClick = { onStaffFilterToggled(it.staffId) },
            onClear = onClearStaffFilters,
            getLabel = { it.fullName },
            getItemSearchText = { it.fullName },
            onDismiss = { showStaffDialog = false }
        )
    }
}

@Composable
fun TriStateChip(
    label: String,
    isIncluded: Boolean,
    isExcluded: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isIncluded -> Color(0x334CAF50)
        isExcluded -> Color(0x33F44336)
        else -> SurfaceDark
    }
    
    val borderColor = when {
        isIncluded -> Color(0xFF4CAF50)
        isExcluded -> Color(0xFFF44336)
        else -> CardBorder
    }

    val contentColor = when {
        isIncluded -> Color(0xFF4CAF50)
        isExcluded -> Color(0xFFF44336)
        else -> TextSecondary
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(durationMillis = 200),
        label = "containerColor"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = borderColor,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isIncluded || isExcluded) TextPrimary else contentColor,
        animationSpec = tween(durationMillis = 200),
        label = "contentColor"
    )

    Box(
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring<IntSize>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clip(RoundedCornerShape(12.dp))
            .background(animatedContainerColor)
            .border(1.dp, animatedBorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isIncluded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            AnimatedVisibility(
                visible = isExcluded,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Text(
                text = label,
                color = animatedContentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun getSortOptionLabel(option: SortOption): String {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    return when (option) {
        SortOption.RELEVANCE -> strings.dashboardScreen.byRelevance
        SortOption.SCORE -> strings.dashboardScreen.sortScoreDesc
        SortOption.SCORE_ASC -> strings.dashboardScreen.sortScoreAsc
        SortOption.TITLE -> strings.dashboardScreen.sortTitleAsc
        SortOption.TITLE_DESC -> strings.dashboardScreen.sortTitleDesc
        SortOption.POPULARITY -> strings.dashboardScreen.sortPopularityDesc
        SortOption.POPULARITY_ASC -> strings.dashboardScreen.sortPopularityAsc
        SortOption.YEAR_DESC -> strings.dashboardScreen.sortYearDesc
        SortOption.YEAR_ASC -> strings.dashboardScreen.sortYearAsc
        SortOption.START_DATE_DESC -> strings.dashboardScreen.sortReleaseDesc
        SortOption.START_DATE_ASC -> strings.dashboardScreen.sortReleaseAsc
        SortOption.EPISODES_DESC -> strings.dashboardScreen.sortEpisodesDesc
        SortOption.EPISODES_ASC -> strings.dashboardScreen.sortEpisodesAsc
    }
}

@Composable
fun getEpisodeGroupLabel(group: EpisodeGroup): String {
    return when (group) {
        EpisodeGroup.LESS_THAN_12 -> "< 12"
        EpisodeGroup.BETWEEN_12_AND_18 -> "12 - 18"
        EpisodeGroup.BETWEEN_19_AND_24 -> "19 - 24"
        EpisodeGroup.GREATER_THAN_24 -> "> 24"
    }
}

@Composable
fun getAnimeFormatLabel(format: AnimeFormat): String {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    return when (format) {
        AnimeFormat.TV -> strings.formats.tv
        AnimeFormat.TV_SHORT -> strings.formats.tvShort
        AnimeFormat.MOVIE -> strings.formats.movie
        AnimeFormat.SPECIAL -> strings.formats.special
        AnimeFormat.OVA -> strings.formats.ova
        AnimeFormat.ONA -> strings.formats.ona
        AnimeFormat.MUSIC -> strings.formats.music
        AnimeFormat.MANGA -> strings.formats.manga
        AnimeFormat.NOVEL -> strings.formats.novel
        AnimeFormat.ONE_SHOT -> strings.formats.oneShot
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> TaxonomySelectionSheet(
    title: String,
    items: List<T>,
    isIncluded: (T) -> Boolean,
    isExcluded: (T) -> Boolean,
    onClick: (T) -> Unit,
    onClear: () -> Unit,
    getLabel: @Composable (T) -> String,
    getItemSearchText: (T) -> String,
    onDismiss: () -> Unit
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { getItemSearchText(it).contains(searchQuery, ignoreCase = true) }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Remember the lazy grid state to check its scroll position
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    
    // Track whether the current touch gesture started while the list was at the top
    var gestureStartedAtTop by remember { mutableStateOf(true) }
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            gestureStartedAtTop = gridState.firstVisibleItemIndex == 0 &&
                    gridState.firstVisibleItemScrollOffset == 0
        }
    }

    val nestedScrollConnection = remember {
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
                        scope.launch {
                            try {
                                gridState.scrollToItem(
                                    gridState.firstVisibleItemIndex,
                                    gridState.firstVisibleItemScrollOffset
                                )
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                } else if (available.y > 0f && !gestureStartedAtTop) {
                    consumedY = available.y
                    if (source == NestedScrollSource.SideEffect) {
                        scope.launch {
                            try {
                                gridState.scrollToItem(
                                    gridState.firstVisibleItemIndex,
                                    gridState.firstVisibleItemScrollOffset
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
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0.dp) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp)
                .nestedScroll(nestedScrollConnection)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onClear) {
                        Text(text = strings.dashboardScreen.clearAll, color = NeonCoral, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.dashboardScreen.searchPlaceholder, color = TextSecondary, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Performant, lazy grid layout of chips to avoid layout bottlenecks on large lists
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems) { item ->
                    TriStateChip(
                        label = getLabel(item),
                        isIncluded = isIncluded(item),
                        isExcluded = isExcluded(item),
                        onClick = { onClick(item) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm button
            Button(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricViolet,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(52.dp)
            ) {
                Text(text = strings.misc.ready, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
