package moe.GetTheNya.AniForge.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.localization.getPlural
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs

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
            }

            // Summary Info Slot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val countText = strings.libraryScreen.itemsCount.getPlural(animeList.size)
                Text(
                    text = countText,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(animeList, key = { it.anilistId }) { anime ->
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
    }
}
