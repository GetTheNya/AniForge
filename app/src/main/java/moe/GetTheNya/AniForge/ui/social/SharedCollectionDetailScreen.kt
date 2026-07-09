package moe.GetTheNya.AniForge.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Check
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.ui.dashboard.FilterBottomSheet
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.localization.getPlural
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.ui.utils.AnimeStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCollectionDetailScreen(
    targetUserId: String,
    collectionId: String,
    viewModel: SharedCollectionDetailViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val strings = LocalLocaleStrings.current
    val collectionTitle by viewModel.collectionTitle.collectAsState()
    val collectionDescription by viewModel.collectionDescription.collectAsState()
    val animeList by viewModel.animeList.collectAsState()
    val trackingMap by viewModel.trackingMap.collectAsState()
    val preferUk by viewModel.preferUk.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val friendStatusFilters by viewModel.friendStatusFilters.collectAsState()
    val localStatusFilters by viewModel.localStatusFilters.collectAsState()
    val coWatchActive by viewModel.coWatchActive.collectAsState()
    val moviesOnlyActive by viewModel.moviesOnlyActive.collectAsState()
    val filteredAnimeList by viewModel.filteredAnimeList.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    val selectedStatus = remember(friendStatusFilters) {
        val included = friendStatusFilters.filter { it.value == 1 }.keys
        if (included.size == 1) included.first() else ""
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar header
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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = collectionTitle.ifBlank { strings.socialScreen.sharedCollectionsTitle },
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (collectionDescription.isNotBlank()) {
                        Text(
                            text = collectionDescription,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!isLoading && errorMessage == null && animeList.isNotEmpty()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(
                        onClick = {
                            viewModel.cloneCollectionToLibrary(
                                onSuccess = {
                                    Toast.makeText(context, strings.socialScreen.cloneSuccess, Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = strings.socialScreen.cloneCollection,
                            tint = TextPrimary
                        )
                    }
                }
            }

            // Local Progress Context & Summary Info Slot
            val localCompletedAnimeIds by viewModel.localCompletedAnimeIds.collectAsState()
            val completedCount = remember(animeList, localCompletedAnimeIds) {
                animeList.count { it.anilistId in localCompletedAnimeIds }
            }
            val totalCount = animeList.size
            val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countText = strings.libraryScreen.itemsCount.getPlural(filteredAnimeList.size)
                    Text(
                        text = countText,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(strings.socialScreen.localProgressLabel, completedCount, totalCount),
                        color = ElectricViolet,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ElectricViolet,
                    trackColor = CardBorder
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search & Filtering Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = strings.misc.search,
                                color = TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = NeonCoral,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = strings.dashboardScreen.filterTooltip,
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Extra modifier chips: Co-Watch, Movies Only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = coWatchActive,
                        onClick = { viewModel.toggleCoWatch() },
                        label = { Text(text = strings.socialScreen.coWatch) },
                        leadingIcon = if (coWatchActive) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.15f),
                            selectedLabelColor = ElectricViolet,
                            selectedLeadingIconColor = ElectricViolet,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = coWatchActive,
                            borderColor = if (coWatchActive) ElectricViolet else CardBorder,
                            selectedBorderColor = ElectricViolet,
                            borderWidth = 1.dp
                        )
                    )

                    FilterChip(
                        selected = moviesOnlyActive,
                        onClick = { viewModel.toggleMoviesOnly() },
                        label = { Text(text = strings.socialScreen.moviesOnly) },
                        leadingIcon = if (moviesOnlyActive) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCoral.copy(alpha = 0.15f),
                            selectedLabelColor = NeonCoral,
                            selectedLeadingIconColor = NeonCoral,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = moviesOnlyActive,
                            borderColor = if (moviesOnlyActive) NeonCoral else CardBorder,
                            selectedBorderColor = NeonCoral,
                            borderWidth = 1.dp
                        )
                    )
                }

                if (!coWatchActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Scrollable Row of Status Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusPairs = listOf(
                            "CURRENT" to strings.misc.watching,
                            "PLANNING" to strings.misc.planning,
                            "COMPLETED" to strings.misc.completed,
                            "PAUSED" to strings.misc.paused,
                            "DROPPED" to strings.misc.dropped
                        )
                        statusPairs.forEach { (status, label) ->
                            val isSelected = selectedStatus == status
                            val chipColor = AnimeStatusColors[status] ?: NeonCoral
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) chipColor.copy(alpha = 0.15f) else SurfaceDark)
                                    .border(1.dp, if (isSelected) chipColor else CardBorder, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.setSingleFriendStatus(status) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) chipColor else TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Content List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = NeonCoral,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: strings.misc.error,
                            color = NeonCoral,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadSharedCollection() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                        ) {
                            Text(text = strings.misc.retry, color = Color.White)
                        }
                    }
                } else if (animeList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.libraryScreen.collectionIsEmpty.replace("\nClick Add Title to populate it.", ""),
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                } else if (filteredAnimeList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val hintText = when {
                            coWatchActive && moviesOnlyActive -> strings.socialScreen.noOverlappingMovies
                            coWatchActive -> strings.socialScreen.noOverlappingPlanning
                            else -> strings.socialScreen.noActiveAnime
                        }
                        Text(
                            text = hintText,
                            color = TextSecondary,
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAnimeList, key = { it.anilistId }) { anime ->
                            val tracking = trackingMap[anime.anilistId]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        navController.navigate(Screen.Detail(anilistId = anime.anilistId))
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Poster Image
                                    AsyncImage(
                                        model = anime.coverLarge ?: anime.coverExtraLarge,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 56.dp, height = 80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Details Block
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = anime.getDisplayTitle(preferUk),
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Progress Row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (tracking != null) {
                                                val totalPlanned = anime.episodes ?: 0
                                                val epText = if (totalPlanned > 0) {
                                                    String.format(strings.socialScreen.episodeProgress, tracking.episodeProgress, totalPlanned)
                                                } else {
                                                    "Ep. ${tracking.episodeProgress}"
                                                }
                                                Text(
                                                    text = epText,
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
                                                )

                                                // Status Indicator with specific color
                                                val statusCfg = statusConfigs.find { it.id == tracking.watchStatus }
                                                if (statusCfg != null) {
                                                    val statusLabel = when (tracking.watchStatus) {
                                                        "CURRENT" -> strings.misc.watching
                                                        "PLANNING" -> strings.misc.planning
                                                        "COMPLETED" -> strings.misc.completed
                                                        "PAUSED" -> strings.misc.paused
                                                        "DROPPED" -> strings.misc.dropped
                                                        else -> tracking.watchStatus
                                                    }
                                                    Text(
                                                        text = statusLabel,
                                                        color = statusCfg.color,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                if (tracking.score != null) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFFD54F),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = String.format(strings.socialScreen.scoreLabel, tracking.score.toString()),
                                                            color = TextSecondary,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }
                                            } else {
                                                val eps = anime.episodes
                                                Text(
                                                    text = if (eps != null) {
                                                        strings.bentoWidgets.episodesTotal.getPlural(eps.toInt())
                                                    } else {
                                                        "?"
                                                    },
                                                    color = TextSecondary,
                                                    fontSize = 13.sp
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
        }

        if (showFilterSheet) {
            FilterBottomSheet(
                isCatalog = false,
                onDismiss = { showFilterSheet = false },
                allGenres = emptyList(),
                allTags = emptyList(),
                allStudios = emptyList(),
                allStaff = emptyList(),
                preferUkTitles = preferUk,
                filteredCount = filteredAnimeList.size,
                friendStatusFilters = friendStatusFilters,
                localStatusFilters = localStatusFilters,
                onFriendStatusFilterToggled = { viewModel.toggleFriendStatusFilter(it) },
                onLocalStatusFilterToggled = { viewModel.toggleLocalStatusFilter(it) },
                onClearAllFilters = { viewModel.clearAllFilters() },
                isSharedProfile = true
            )
        }
    }
}
