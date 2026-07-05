package moe.GetTheNya.AniForge.ui.bento

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import moe.GetTheNya.AniForge.core.model.GenreDistribution
import moe.GetTheNya.AniForge.core.model.StudioDistribution
import moe.GetTheNya.AniForge.core.model.FranchiseGiantInfo
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.localization.getPlural
import moe.GetTheNya.AniForge.ui.profile.AnimeStatsPieChart
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.core.network.UserProfileDto

val BentoCardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceCardDark,
    borderColor: Color = CardBorder,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val modifierWithClick = if (onClick != null || onLongClick != null) {
        modifier
            .clip(BentoCardShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, BentoCardShape)
            .combinedClickable(
                enabled = !isEditMode,
                onClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            )
    } else {
        modifier
            .clip(BentoCardShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, BentoCardShape)
    }

    Box(
        modifier = modifierWithClick.padding(16.dp),
        content = content
    )
}

@Composable
fun WatchTimeWidget(
    totalMinutes: Long,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current.bentoWidgets
    
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val minutes = totalMinutes % 60

    var targetDays by remember { mutableStateOf(0) }
    var targetHours by remember { mutableStateOf(0) }
    var targetMinutes by remember { mutableStateOf(0) }

    LaunchedEffect(totalMinutes) {
        delay(100L)
        targetDays = (totalMinutes / 1440).toInt()
        targetHours = ((totalMinutes % 1440) / 60).toInt()
        targetMinutes = (totalMinutes % 60).toInt()
    }

    val animatedDays by animateIntAsState(
        targetValue = targetDays,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "animatedDays"
    )
    val animatedHours by animateIntAsState(
        targetValue = targetHours,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "animatedHours"
    )
    val animatedMinutes by animateIntAsState(
        targetValue = targetMinutes,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "animatedMinutes"
    )

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = SurfaceCardDark.copy(alpha = 0.9f),
        onLongClick = onLongClick,
        isEditMode = isEditMode
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "hourglass_spin")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )

        Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            tint = ElectricViolet.copy(alpha = 0.05f),
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterEnd)
                .graphicsLayer {
                    rotationZ = rotationAngle
                }
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.watchTimeTitle,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricViolet.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TimeBlock(value = animatedDays.toString(), label = strings.watchTimeDays.getPlural(days.toInt()))
                    TimeBlock(value = animatedHours.toString(), label = strings.watchTimeHours.getPlural(hours.toInt()))
                    TimeBlock(value = animatedMinutes.toString(), label = strings.watchTimeMinutes.getPlural(minutes.toInt()))
                }
            }
        }
    }
}

@Composable
private fun TimeBlock(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ChaosMeterWidget(
    count: Int,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current.bentoWidgets

    var targetCount by remember { mutableStateOf(0) }
    LaunchedEffect(count) {
        delay(100L)
        targetCount = count
    }
    val animatedCount by animateIntAsState(
        targetValue = targetCount,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "animatedChaosCount"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "chaosMeterInfinite")

    val bobbingValue by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chaosMeterBobbing"
    )

    val wobbleValue by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chaosMeterWobble"
    )

    BentoCard(
        modifier = modifier.height(115.dp),
        onLongClick = onLongClick,
        isEditMode = isEditMode
    ) {
        Icon(
            imageVector = Icons.Default.Casino,
            contentDescription = null,
            tint = NeonCoral.copy(alpha = 0.07f),
            modifier = Modifier
                .requiredSize(180.dp)
                .align(Alignment.Center)
                .offset(x = 16.dp, y = 16.dp)
                .graphicsLayer {
                    translationY = bobbingValue.dp.toPx()
                    rotationZ = -20f + wobbleValue
                }
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.chaosMeter,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = animatedCount.toString(),
                color = NeonCoral,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                modifier = Modifier.offset(y = (-4).dp)
            )
        }
    }
}

@Composable
fun CollectionsBridgeWidget(
    activeCollections: Int,
    covers: List<String>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current.bentoWidgets

    var targetActiveCollections by remember { mutableStateOf(0) }
    LaunchedEffect(activeCollections) {
        delay(100L)
        targetActiveCollections = activeCollections
    }
    val animatedActiveCollections by animateIntAsState(
        targetValue = targetActiveCollections,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "animatedActiveCollections"
    )

    BentoCard(
        modifier = modifier.height(115.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        isEditMode = isEditMode
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.activeCollections,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = animatedActiveCollections.toString(),
                    color = CyberTeal,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.offset(y = (-4).dp)
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E24))
                ) {
                    if (covers.isNotEmpty()) {
                        val cols = 2
                        covers.take(4).forEachIndexed { index, url ->
                            val row = index / cols
                            val col = index % cols
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .offset(x = (col * 32).dp, y = (row * 32).dp)
                                    .size(32.dp)
                                    .border(0.5.dp, CardBorder)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopStudiosWidget(
    studios: List<StudioDistribution>,
    onStudioClick: (Long) -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current.dashboardScreen

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onLongClick = onLongClick,
        isEditMode = isEditMode
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.studios,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (studios.isEmpty()) {
                Text(
                    text = "—",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            } else {
                val totalCount = remember(studios) { studios.sumOf { it.count }.toFloat().coerceAtLeast(1f) }
                studios.take(3).forEach { dist ->
                    val ratio = dist.count / totalCount
                    
                    var targetProgress by remember(dist.studio.studioId) { mutableStateOf(0f) }
                    LaunchedEffect(dist.studio.studioId, ratio) {
                        delay(100L)
                        targetProgress = ratio
                    }
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        ),
                        label = "studioProgress"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                enabled = !isEditMode,
                                onClick = { onStudioClick(dist.studio.studioId) },
                                onLongClick = { onLongClick?.invoke() }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dist.studio.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = dist.count.toString(),
                                    color = CyberTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                color = CyberTeal,
                                trackColor = Color(0xFF1E1E24),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopGenresWidget(
    genres: List<GenreDistribution>,
    onGenreClick: (String) -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current.dashboardScreen
    val preferUk = LocalLocaleStrings.current.languageCode == "uk"

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onLongClick = onLongClick,
        isEditMode = isEditMode
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.genres,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (genres.isEmpty()) {
                Text(
                    text = "—",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            } else {
                val totalCount = remember(genres) { genres.sumOf { it.count }.toFloat().coerceAtLeast(1f) }
                genres.take(3).forEach { dist ->
                    val ratio = dist.count / totalCount
                    val displayName = dist.genre.nameUk?.takeIf { preferUk } ?: dist.genre.nameEn
                    
                    var targetProgress by remember(dist.genre.slug) { mutableStateOf(0f) }
                    LaunchedEffect(dist.genre.slug, ratio) {
                        delay(100L)
                        targetProgress = ratio
                    }
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        ),
                        label = "genreProgress"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                enabled = !isEditMode,
                                onClick = { onGenreClick(dist.genre.slug) },
                                onLongClick = { onLongClick?.invoke() }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = displayName,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = dist.count.toString(),
                                    color = NeonCoral,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                color = NeonCoral,
                                trackColor = Color(0xFF1E1E24),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FranchiseGiantWidget(
    info: FranchiseGiantInfo?,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current.bentoWidgets
    val preferUk = LocalLocaleStrings.current.languageCode == "uk"

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onLongClick = onLongClick,
        isEditMode = isEditMode
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = ElectricViolet.copy(alpha = 0.05f),
            modifier = Modifier
                .size(76.dp)
                .align(Alignment.CenterEnd)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricViolet.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricViolet,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = strings.mostWatchedUniverse,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (info != null) {
                    val name = info.franchise.nameUk?.takeIf { preferUk } ?: info.franchise.nameEn ?: ""
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.episodesTotal.getPlural(info.totalEpisodes),
                        color = ElectricViolet,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "—",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BentoWatchStatusPieChart(
    stats: Map<String, Int>,
    onStatusClick: (String) -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onLongClick = onLongClick,
        isEditMode = isEditMode
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.bentoWidgets.watchStatusTitle,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Side: Pie/Donut Chart
                AnimeStatsPieChart(
                    stats = stats,
                    modifier = Modifier.size(130.dp),
                    strokeWidth = 22.dp
                )

                // Right Side: Legend Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statusConfigs.forEach { config ->
                        val count = stats[config.id] ?: 0
                        val isCountZero = count == 0

                        Row(
                          modifier = Modifier
                              .fillMaxWidth()
                              .clip(RoundedCornerShape(8.dp))
                              .combinedClickable(
                                  enabled = !isEditMode,
                                  onClick = { onStatusClick(config.id) },
                                  onLongClick = { onLongClick?.invoke() }
                              )
                              .padding(vertical = 4.dp, horizontal = 6.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colorAlpha = if (isCountZero) 0.35f else 1.0f
                            val textAlpha = if (isCountZero) 0.45f else 1.0f

                            // Micro color indicator badge
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(config.color.copy(alpha = colorAlpha))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Localized label text
                            Text(
                                text = config.getLabel(strings),
                                fontSize = 12.sp,
                                fontWeight = if (isCountZero) FontWeight.Normal else FontWeight.Medium,
                                color = TextPrimary.copy(alpha = textAlpha),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // Absolute count text using pluralization engine
                            val countText = strings.profileScreen.titlesCount.getPlural(count)
                            Text(
                                text = countText,
                                fontSize = 12.sp,
                                fontWeight = if (isCountZero) FontWeight.Normal else FontWeight.Bold,
                                color = TextSecondary.copy(alpha = textAlpha)
                            )

                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = if (isCountZero) 0.3f else 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsWidget(
    friends: List<UserProfileDto>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalLocaleStrings.current
    BentoCard(
        onClick = onClick,
        onLongClick = onLongClick,
        isEditMode = isEditMode,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${strings.socialScreen.tabFriends} (${friends.size})",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Avatar stack
            val displayFriends = friends.take(5)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                displayFriends.forEachIndexed { index, friend ->
                    Box(
                        modifier = Modifier
                            .padding(start = (index * 24).dp)
                            .size(40.dp)
                            .border(2.dp, BackgroundDark, CircleShape) // Dark border separation
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(TransparentAccent)
                            .border(1.dp, NeonCoral, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friend.username.firstOrNull()?.uppercase() ?: "?",
                            color = NeonCoral,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (friends.size > 5) {
                    Box(
                        modifier = Modifier
                            .padding(start = (5 * 24).dp)
                            .size(40.dp)
                            .border(2.dp, BackgroundDark, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${friends.size - 5}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

