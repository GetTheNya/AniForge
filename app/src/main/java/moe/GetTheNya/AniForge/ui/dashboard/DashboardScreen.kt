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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.disableSplitTouch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAnimeClick: (Long) -> Unit,
    viewModel: DashboardViewModel,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val searchFilter by viewModel.searchFilter.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Sticky Header & Search Bar
        SearchBar(
            query = searchFilter.textQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier.padding(16.dp)
        )

        // Main Bento list container
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCoral)
                }
            }
            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val errorMessage = when (state.message) {
                        "Unknown error" -> strings.dashboardScreen.unknownError
                        else -> state.message
                    }
                    Text(text = "${strings.misc.error}: $errorMessage", color = NeonCoral)
                }
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    animeList = state.animeList,
                    trackingMap = state.trackingMap,
                    preferUk = preferUk,
                    onAnimeClick = onAnimeClick
                )
            }
        }
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
    animeList: List<Anime>,
    trackingMap: Map<Long, String>,
    preferUk: Boolean,
    onAnimeClick: (Long) -> Unit
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().disableSplitTouch()
    ) {
        // Header for Catalog listing
        item(span = { GridItemSpan(2) }) {
            Text(
                text = strings.dashboardScreen.discoverCatalog,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Grid Catalog items
        items(
            items = animeList,
            key = { anime -> anime.anilistId },
            contentType = { "anime_card" }
        ) { anime ->
            AnimeBentoCard(
                anime = anime,
                status = trackingMap[anime.anilistId],
                preferUk = preferUk,
                onClick = { onAnimeClick(anime.anilistId) }
            )
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
    onClick: () -> Unit
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
                text = anime.getDisplayTitle(preferUk = true),
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = anime.descriptionUk ?: anime.descriptionEn ?: "",
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
