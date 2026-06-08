package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.Studio
import moe.GetTheNya.AniForge.core.model.EpisodeGroup
import moe.GetTheNya.AniForge.core.model.SortOption
import moe.GetTheNya.AniForge.core.model.AnimeFormat
import moe.GetTheNya.AniForge.ui.theme.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
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

private enum class SortCategory {
    SCORE, POPULARITY, RELEASE_DATE, EPISODES, TITLE
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    viewModel: DashboardViewModel,
    onDismiss: () -> Unit
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val scope = rememberCoroutineScope()
    
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val filter by viewModel.searchFilter.collectAsState()
    val count by viewModel.filteredCount.collectAsState()
    val preferUkTitles by viewModel.preferUkTitles.collectAsState()
    
    val genres by viewModel.allGenres.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val studios by viewModel.allStudios.collectAsState()

    val inlineGenres = remember(filter.genres, filter.excludedGenres, genres) {
        val selectedGenres = genres.filter { filter.genres.contains(it.slug) || filter.excludedGenres.contains(it.slug) }
        val top8Genres = genres.take(8)
        (selectedGenres + top8Genres).distinct()
    }

    val inlineTags = remember(filter.tags, filter.excludedTags, tags) {
        val selectedTags = tags.filter { filter.tags.contains(it.tagId) || filter.excludedTags.contains(it.tagId) }
        val top8Tags = tags.take(8)
        (selectedTags + top8Tags).distinct()
    }

    val inlineStudios = remember(filter.studios, filter.excludedStudios, studios) {
        val selectedStudios = studios.filter { filter.studios.contains(it.studioId) || filter.excludedStudios.contains(it.studioId) }
        val top8Studios = studios.take(8)
        (selectedStudios + top8Studios).distinct()
    }

    var showGenreDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showStudioDialog by remember { mutableStateOf(false) }

    val readyText = strings.dashboardScreen.readyWithCount.replace("{count}", count.toString())

    val bottomSheetScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y < 0f) {
                    Offset(x = 0f, y = available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (available.y < 0f) {
                    Velocity(x = 0f, y = available.y)
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CardBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    onClick = { viewModel.clearAllFilters() }
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(bottomSheetScrollConnection),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    // 1. Sort By
                    Column {
                        Text(text = strings.dashboardScreen.sortBy, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val activeSort = filter.sortBy
                        val (activeCategory, isDescending) = when (activeSort) {
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
                                if (isDescending) {
                                    when (category) {
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
                                    SortCategory.SCORE -> SortOption.SCORE
                                    SortCategory.POPULARITY -> SortOption.POPULARITY
                                    SortCategory.RELEASE_DATE -> SortOption.START_DATE_DESC
                                    SortCategory.EPISODES -> SortOption.EPISODES_DESC
                                    SortCategory.TITLE -> SortOption.TITLE_DESC
                                }
                            }
                            viewModel.updateSortOrder(newSortOption)
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
                    }
                }

                item {
                    // 2. Score Range Slider
                    Column {
                        val currentMin = filter.minScore ?: 0.0
                        val currentMax = filter.maxScore ?: 10.0
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
                                viewModel.updateScoreRange(min, max)
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = ElectricViolet,
                                inactiveTrackColor = CardBorder,
                                thumbColor = ElectricViolet
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
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
                            for (format in AnimeFormat.entries) {
                                val isIncluded = filter.formats.contains(format)
                                val isExcluded = filter.excludedFormats.contains(format)
                                TriStateChip(
                                    label = getAnimeFormatLabel(format),
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { viewModel.toggleFormatType(format) }
                                )
                            }
                        }
                    }
                }

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
                            for (group in EpisodeGroup.entries) {
                                val isIncluded = filter.episodeGroups.contains(group)
                                val isExcluded = filter.excludedEpisodeGroups.contains(group)
                                val label = getEpisodeGroupLabel(group)
                                TriStateChip(
                                    label = label,
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { viewModel.toggleEpisodeGroup(group) }
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
                            for ((statusId, label) in statusOptions) {
                                val isIncluded = filter.trackingStatuses.contains(statusId)
                                val isExcluded = filter.excludedTrackingStatuses.contains(statusId)
                                TriStateChip(
                                    label = label,
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { viewModel.toggleTrackingStatus(statusId) }
                                )
                            }
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
                                    .clickable { viewModel.toggleUkTranslationFilter() }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = strings.dashboardScreen.hasTranslation, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(text = strings.dashboardScreen.hasTranslationDesc, color = TextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = filter.hasUkTranslation == true,
                                    onCheckedChange = { viewModel.toggleUkTranslationFilter() },
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
                                val isIncluded = filter.studios.contains(studio.studioId)
                                val isExcluded = filter.excludedStudios.contains(studio.studioId)
                                TriStateChip(
                                    label = studio.name,
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { viewModel.toggleStudioFilter(studio.studioId) }
                                )
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
                                val isIncluded = filter.genres.contains(genre.slug)
                                val isExcluded = filter.excludedGenres.contains(genre.slug)
                                TriStateChip(
                                    label = genre.getDisplayName(preferUk = true),
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { viewModel.toggleGenreFilterState(genre.slug) }
                                )
                            }
                        }
                    }
                }

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
                                val isIncluded = filter.tags.contains(tag.tagId)
                                val isExcluded = filter.excludedTags.contains(tag.tagId)
                                TriStateChip(
                                    label = tag.getDisplayName(preferUk = true),
                                    isIncluded = isIncluded,
                                    isExcluded = isExcluded,
                                    onClick = { viewModel.toggleTagFilterState(tag.tagId) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Footer Apply Button
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

    // Secondary Overlays for large taxonomies
    if (showGenreDialog) {
        TaxonomySelectionDialog<Genre>(
            title = strings.dashboardScreen.allGenres,
            items = genres,
            isIncluded = { filter.genres.contains(it.slug) },
            isExcluded = { filter.excludedGenres.contains(it.slug) },
            onClick = { viewModel.toggleGenreFilterState(it.slug) },
            onClear = { viewModel.clearGenreFilters() },
            getLabel = { it.getDisplayName(preferUk = true) },
            getItemSearchText = { it.getDisplayName(preferUk = true) + " " + it.slug },
            onDismiss = { showGenreDialog = false }
        )
    }

    if (showTagDialog) {
        TaxonomySelectionDialog<Tag>(
            title = strings.dashboardScreen.allTags,
            items = tags,
            isIncluded = { filter.tags.contains(it.tagId) },
            isExcluded = { filter.excludedTags.contains(it.tagId) },
            onClick = { viewModel.toggleTagFilterState(it.tagId) },
            onClear = { viewModel.clearTagFilters() },
            getLabel = { it.getDisplayName(preferUk = true) },
            getItemSearchText = { it.getDisplayName(preferUk = true) },
            onDismiss = { showTagDialog = false }
        )
    }

    if (showStudioDialog) {
        TaxonomySelectionDialog<Studio>(
            title = strings.dashboardScreen.allStudios,
            items = studios,
            isIncluded = { filter.studios.contains(it.studioId) },
            isExcluded = { filter.excludedStudios.contains(it.studioId) },
            onClick = { viewModel.toggleStudioFilter(it.studioId) },
            onClear = { viewModel.clearStudioFilters() },
            getLabel = { it.name },
            getItemSearchText = { it.name },
            onDismiss = { showStudioDialog = false }
        )
    }
}

@Composable
fun MoreChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, ElectricViolet, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = ElectricViolet,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = ElectricViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> TaxonomySelectionDialog(
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        IconButton(onClick = onDismiss) {
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
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(text = strings.misc.ready, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
