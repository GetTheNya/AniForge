package moe.GetTheNya.AniForge.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import moe.GetTheNya.AniForge.ui.dashboard.BentoStatsCard
import moe.GetTheNya.AniForge.ui.dashboard.FeaturedBentoCard
import moe.GetTheNya.AniForge.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val uiState by viewModel.homeUiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Sticky Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.homeScreen.welcomeBack,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = strings.homeScreen.appName,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        Crossfade(targetState = uiState, label = "homeCrossfade") { state ->
            when (state) {
                is HomeUiState.Loading, HomeUiState.Updating -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonCoral)
                    }
                }
                is HomeUiState.Error -> {
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
                is HomeUiState.Success -> {
                    HomeContent(
                        state = state,
                        onAnimeClick = onAnimeClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    state: HomeUiState.Success,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section Title: Quick Stats
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.homeScreen.trackingProgress,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            BentoStatsCard(stats = state.stats)
        }

        // Section Title: Featured Title Spotlight
        if (state.featuredAnime != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.homeScreen.spotlight,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                HomeFeaturedCard(
                    anime = state.featuredAnime,
                    preferUk = state.preferUk,
                    onClick = { onAnimeClick(state.featuredAnime.anilistId) }
                )
            }
        }

        // Section Title: Local Engine Health
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 96.dp) // extra padding so content isn't covered by floating bar
        ) {
            Text(
                text = strings.homeScreen.catalogCoreStatus,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            EngineStatusCard(
                version = state.catalogVersion,
                slot = state.activeSlot
            )
        }
    }
}

@Composable
fun HomeFeaturedCard(
    anime: Anime,
    preferUk: Boolean,
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
                text = anime.getDisplayTitle(preferUk = preferUk),
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
fun EngineStatusCard(
    version: Long,
    slot: String
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.homeScreen.offlineCatalog,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.homeScreen.hotswappingEnabled,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1F00F5D4))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.misc.ready,
                    color = CyberTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Divider(color = CardBorder, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = strings.homeScreen.activeSlot,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = slot.uppercase(),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = strings.homeScreen.catalogStamp,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "v$version",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
