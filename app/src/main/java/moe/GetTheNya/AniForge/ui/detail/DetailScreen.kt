package moe.GetTheNya.AniForge.ui.detail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import moe.GetTheNya.AniForge.ui.dashboard.AnimeBentoCard
import moe.GetTheNya.AniForge.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    anilistId: Long,
    onBackClick: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    viewModel: DetailViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Trigger load on startup
    LaunchedEffect(anilistId) {
        viewModel.loadAnimeDetail(anilistId)
    }

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
                    Text(text = "Error: ${state.message}", color = NeonCoral, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAnimeDetail(anilistId) }, colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)) {
                        Text("Retry")
                    }
                }
            }
            is DetailUiState.Success -> {
                DetailContent(
                    anime = state.anime,
                    screenshots = state.screenshots,
                    relations = state.relations,
                    tracking = state.tracking,
                    onStatusChange = viewModel::updateWatchStatus,
                    onIncrementProgress = viewModel::incrementEpisodeProgress,
                    onDecrementProgress = viewModel::decrementEpisodeProgress,
                    onSaveNotes = viewModel::saveNotes,
                    onAnimeClick = onAnimeClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x990C0C0E))
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }
    }
}

@Composable
fun DetailContent(
    anime: Anime,
    screenshots: List<String>,
    relations: List<Anime>,
    tracking: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity?,
    onStatusChange: (String) -> Unit,
    onIncrementProgress: () -> Unit,
    onDecrementProgress: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onAnimeClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        // Banner Image & Title Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
            // Title info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
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
                    text = anime.getDisplayTitle(preferUk = true),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Body Content
        Column(
            modifier = Modifier.padding(20.dp),
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
                    text = "Synopsis",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = anime.descriptionUk ?: anime.descriptionEn ?: "No description available.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            // Screenshots Gallery
            if (screenshots.isNotEmpty()) {
                Column {
                    Text(
                        text = "Gallery",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = screenshots,
                            key = { it }
                        ) { imageUrl ->
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(imageUrl)
                                    .precision(coil.size.Precision.EXACT)
                                    .allowHardware(true)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Screenshot",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            )
                        }
                    }
                }
            }

            // Related Anime List (sorted by release date)
            if (relations.isNotEmpty()) {
                Column {
                    Text(
                        text = "Related Titles",
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

@Composable
fun TrackingWidget(
    tracking: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity?,
    maxEpisodes: Int,
    onStatusChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    val statuses = listOf("CURRENT" to "Watching", "COMPLETED" to "Completed", "PLANNING" to "Planning", "PAUSED" to "Paused")
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
        Text(text = "My Progress", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        // Status Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { (statusId, label) ->
                val isSelected = tracking?.watchStatus == statusId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonCoral else Color(0x33FFFFFF))
                        .clickable { onStatusChange(statusId) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) BackgroundDark else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
            Text(text = "Episodes Watched", color = TextSecondary, fontSize = 14.sp)
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
            Text(text = "Personal Notes", color = TextSecondary, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Add custom notes...", color = TextSecondary, fontSize = 13.sp) },
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
                    Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
