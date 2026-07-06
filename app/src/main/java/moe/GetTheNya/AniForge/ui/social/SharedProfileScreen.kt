package moe.GetTheNya.AniForge.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import moe.GetTheNya.AniForge.ui.dashboard.FilterBottomSheet
import androidx.compose.runtime.*
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
import moe.GetTheNya.AniForge.ui.bento.BentoDashboardGrid
import moe.GetTheNya.AniForge.ui.franchises.CollectionBentoCard
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.AnimeStatusColors

@Composable
fun SharedProfileScreen(
    viewModel: SharedProfileViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val strings = LocalLocaleStrings.current
    val uiState by viewModel.uiState.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val friendStatusFilters by viewModel.friendStatusFilters.collectAsState()
    val localStatusFilters by viewModel.localStatusFilters.collectAsState()
    val compatibilityPercentage by viewModel.compatibilityPercentage.collectAsState()
    val filteredWatchList by viewModel.filteredWatchList.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
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
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = strings.misc.back,
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = strings.profileScreen.userProfile,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // User Info Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(TransparentAccent)
                            .border(2.dp, NeonCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.avatarLetter,
                            color = NeonCoral,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = uiState.username,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        compatibilityPercentage?.let { pct ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ElectricViolet.copy(alpha = 0.15f),
                                modifier = Modifier.border(1.dp, ElectricViolet.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = String.format(strings.socialScreen.commonTitlesBadge, pct),
                                    color = ElectricViolet,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tabs Row (Lists, Collections, Stats)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabTitles = listOf(
                    strings.socialScreen.tabLists,
                    strings.socialScreen.tabCollections,
                    strings.socialScreen.tabStats
                )
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ElectricViolet.copy(alpha = 0.2f) else SurfaceDark)
                            .border(1.dp, if (isSelected) ElectricViolet else CardBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) ElectricViolet else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = NeonCoral,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (uiState.errorMessage != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: strings.socialScreen.networkExceptionAlert,
                            color = NeonCoral,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadProfileData() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                        ) {
                            Text(text = strings.misc.retry, color = Color.White)
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            // Lists Tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Search & Filtering Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 4.dp),
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isCoWatchActive by viewModel.coWatchActive.collectAsState()
                                    FilterChip(
                                        selected = isCoWatchActive,
                                        onClick = { viewModel.toggleCoWatch() },
                                        label = { Text(text = strings.socialScreen.coWatch) },
                                        leadingIcon = if (isCoWatchActive) {
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
                                            selected = isCoWatchActive,
                                            borderColor = if (isCoWatchActive) ElectricViolet else CardBorder,
                                            selectedBorderColor = ElectricViolet,
                                            borderWidth = 1.dp
                                        )
                                    )

                                    val isMoviesOnlyActive by viewModel.moviesOnlyActive.collectAsState()
                                    FilterChip(
                                        selected = isMoviesOnlyActive,
                                        onClick = { viewModel.toggleMoviesOnly() },
                                        label = { Text(text = strings.socialScreen.moviesOnly) },
                                        leadingIcon = if (isMoviesOnlyActive) {
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
                                            selected = isMoviesOnlyActive,
                                            borderColor = if (isMoviesOnlyActive) NeonCoral else CardBorder,
                                            selectedBorderColor = NeonCoral,
                                            borderWidth = 1.dp
                                        )
                                    )
                                }

                                val isCoWatchActive by viewModel.coWatchActive.collectAsState()
                                if (!isCoWatchActive) {
                                    // Scrollable Row of Status Chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 24.dp, vertical = 8.dp),
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

                                val currentList = filteredWatchList
                                val isMoviesOnlyActive by viewModel.moviesOnlyActive.collectAsState()

                                if (currentList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val hintText = when {
                                            isCoWatchActive && isMoviesOnlyActive -> strings.socialScreen.noOverlappingMovies
                                            isCoWatchActive -> strings.socialScreen.noOverlappingPlanning
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
                                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                                        contentPadding = PaddingValues(bottom = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(currentList, key = { it.tracking.anilistId }) { item ->
                                            val anime = item.anime
                                            val tracking = item.tracking

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        if (anime != null) {
                                                            navController.navigate(Screen.Detail(anilistId = anime.anilistId))
                                                        }
                                                    },
                                                shape = RoundedCornerShape(12.dp),
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
                                                        model = anime?.coverLarge ?: anime?.coverExtraLarge,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(width = 56.dp, height = 80.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(SurfaceCardDark)
                                                    )

                                                    Spacer(modifier = Modifier.width(16.dp))

                                                    // Details
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = anime?.getDisplayTitle(preferUk) ?: "Unknown Anime",
                                                            color = TextPrimary,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            // Episode Progress
                                                            val epText = if (anime != null) {
                                                                val totalPlanned = anime.episodes ?: 0
                                                                if (totalPlanned > 0) {
                                                                    String.format(strings.socialScreen.episodeProgress, tracking.episodeProgress, totalPlanned)
                                                                } else {
                                                                    "Ep. ${tracking.episodeProgress}"
                                                                }
                                                            } else {
                                                                "Ep. ${tracking.episodeProgress}"
                                                            }
                                                            Text(
                                                                text = epText,
                                                                color = TextSecondary,
                                                                fontSize = 13.sp
                                                            )

                                                            // Score
                                                            if (tracking.score != null) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Star,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFFFD54F), // Amber/Yellow
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                    Text(
                                                                        text = String.format(strings.socialScreen.scoreLabel, tracking.score.toString()),
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
                        }
                        1 -> {
                            // Collections Tab
                            val collectionsList = uiState.collectionsList
                            if (collectionsList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.socialScreen.noCollections,
                                        color = TextSecondary,
                                        fontSize = 15.sp
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(collectionsList) { col ->
                                        CollectionBentoCard(
                                            title = col.title,
                                            description = col.description,
                                            posters = col.posters,
                                            totalCount = col.totalCount,
                                            statusCounts = col.statusCounts,
                                            isSelected = false,
                                            onClick = {
                                                navController.navigate(
                                                    Screen.SharedCollectionDetail(
                                                        targetUserId = viewModel.userId,
                                                        collectionId = col.collectionId
                                                    )
                                                )
                                            },
                                            onLongClick = null // Disable context actions (delete/edit/drag)
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Stats Tab
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                                BentoDashboardGrid(
                                    stats = uiState.userStats,
                                    bentoData = uiState.bentoStats,
                                    trackingStats = uiState.trackingStats,
                                    onStudioClick = {}, // Read-only: disable action
                                    onGenreClick = {},
                                    onCollectionClick = {
                                        // Switch to Collections tab
                                        selectedTab = 1
                                    },
                                    onStatusClick = { statusId ->
                                        // Switch to Lists tab and select watch status
                                        selectedTab = 0
                                        viewModel.setSingleFriendStatus(statusId)
                                    },
                                    friends = emptyList(), // Read-only dashboard does not show friends list
                                    onSocialClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
        val isCoWatchActive by viewModel.coWatchActive.collectAsState()
        val isMoviesOnlyActive by viewModel.moviesOnlyActive.collectAsState()
        val selectedRouletteAnime by viewModel.selectedRouletteAnime.collectAsState()

        if (selectedTab == 0 && (isCoWatchActive || isMoviesOnlyActive)) {
            val isEnabled = filteredWatchList.isNotEmpty()
            ExtendedFloatingActionButton(
                onClick = {
                    if (isEnabled) {
                        viewModel.rollRoulette()
                    }
                },
                containerColor = if (isEnabled) ElectricViolet else SurfaceDark,
                contentColor = if (isEnabled) Color.White else TextSecondary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .border(1.dp, if (isEnabled) Color.Transparent else CardBorder, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = strings.socialScreen.randomPick
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = strings.socialScreen.randomPick)
            }
        }

        if (selectedRouletteAnime != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearRoulette() },
                title = {
                    Text(
                        text = strings.socialScreen.animeRouletteTitle,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = selectedRouletteAnime!!.coverLarge ?: selectedRouletteAnime!!.coverExtraLarge,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 120.dp, height = 170.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = selectedRouletteAnime!!.getDisplayTitle(preferUk),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        selectedRouletteAnime!!.format?.let { format ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = format,
                                color = NeonCoral,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = selectedRouletteAnime!!.anilistId
                            viewModel.clearRoulette()
                            navController.navigate(Screen.Detail(anilistId = id))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text(text = strings.socialScreen.viewDetails, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { viewModel.rollRoulette() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text(text = strings.socialScreen.reroll, color = TextPrimary)
                    }
                },
                containerColor = SurfaceDark,
                tonalElevation = 6.dp
            )
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
                filteredCount = filteredWatchList.size,
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
