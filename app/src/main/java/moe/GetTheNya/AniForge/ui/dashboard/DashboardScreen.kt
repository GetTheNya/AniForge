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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAnimeClick: (Long) -> Unit,
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
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
                    Text(text = "Error: ${state.message}", color = NeonCoral)
                }
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    animeList = state.animeList,
                    featuredAnime = state.featuredAnime,
                    stats = state.stats,
                    selectedGenres = searchFilter.genres,
                    onGenreToggle = viewModel::toggleGenreFilter,
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
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search catalog...", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
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
    featuredAnime: Anime?,
    stats: UserStats,
    selectedGenres: List<String>,
    onGenreToggle: (String) -> Unit,
    onAnimeClick: (Long) -> Unit
) {
    val genresList = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Romance", "Sci-Fi", "Mystery", "Psychological", "Supernatural")

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Stats Bento Panel (Span full width)
        item(span = { GridItemSpan(2) }) {
            BentoStatsCard(stats)
        }

        // 2. Featured Card (Span full width)
        if (featuredAnime != null && selectedGenres.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                FeaturedBentoCard(anime = featuredAnime, onClick = { onAnimeClick(featuredAnime.anilistId) })
            }
        }

        // 3. Genre Selector Row (Span full width)
        item(span = { GridItemSpan(2) }) {
            Column {
                Text(
                    text = "Categories",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genresList.forEach { genre ->
                        val slug = genre.lowercase()
                        val isSelected = selectedGenres.contains(slug)
                        GenreChip(
                            name = genre,
                            selected = isSelected,
                            onClick = { onGenreToggle(slug) }
                        )
                    }
                }
            }
        }

        // Header for Catalog listing
        item(span = { GridItemSpan(2) }) {
            Text(
                text = if (selectedGenres.isNotEmpty()) "Filtered Catalog" else "Discover Catalog",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 4. Grid Catalog items
        items(
            items = animeList,
            key = { anime -> anime.anilistId },
            contentType = { "anime_card" }
        ) { anime ->
            AnimeBentoCard(
                anime = anime,
                onClick = { onAnimeClick(anime.anilistId) }
            )
        }
    }
}

@Composable
fun BentoStatsCard(stats: UserStats) {
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
            Text(text = "Episodes", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = CardBorder)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${stats.titlesCompleted}", color = CyberTeal, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(text = "Completed", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = CardBorder)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${stats.titlesWatching}", color = ElectricViolet, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(text = "Watching", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun FeaturedBentoCard(
    anime: Anime,
    onClick: () -> Unit
) {
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
            model = anime.bannerImage ?: anime.coverLarge,
            contentDescription = anime.titleRomaji,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
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
                    text = "FEATURED  •  ${anime.scoreMal ?: 8.5}",
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
