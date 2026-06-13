package moe.GetTheNya.AniForge.ui.franchises

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.localization.getPlural
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.AnimeStatusColors
import moe.GetTheNya.AniForge.ui.utils.disableSplitTouch
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val franchisesList by viewModel.franchises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val activeLibraryTab by viewModel.activeLibraryTab.collectAsState()
    val pagerState = rememberPagerState(initialPage = activeLibraryTab ?: 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeLibraryTab) {
        activeLibraryTab?.let { tab ->
            if (pagerState.currentPage != tab) {
                pagerState.animateScrollToPage(tab)
            }
            viewModel.activeLibraryTab.value = null
        }
    }

    DisposableEffect(navController, pagerState) {
        navController.onLibraryClick = {
            coroutineScope.launch {
                val nextPage = if (pagerState.currentPage == 0) 1 else 0
                pagerState.animateScrollToPage(nextPage)
            }
        }
        onDispose {
            navController.onLibraryClick = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Screen Header
            Text(
                text = strings.libraryScreen.name,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )

            // Premium minimalist Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabTitles = listOf(strings.libraryScreen.franchises, strings.libraryScreen.collections)
                tabTitles.forEachIndexed { index, title ->
                    val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    val fraction = (1f - kotlin.math.abs(pageOffset - index)).coerceIn(0f, 1f)
                    
                    val bgColor = lerp(Color.Transparent, ElectricViolet, fraction)
                    val textColor = lerp(TextSecondary, BackgroundDark, fraction)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // Franchises Page
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ElectricViolet)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom navigation
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = franchisesList,
                                    key = { it.franchise.franchiseId }
                                ) { item ->
                                    FranchiseBentoCard(
                                        item = item,
                                        preferUk = preferUk,
                                        onClick = {
                                            navController.navigate(Screen.FranchiseTree(item.franchise.franchiseId))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Collections Page
                        val collectionsList by viewModel.filteredCollections.collectAsState()
                        var showCreateDialog by remember { mutableStateOf(false) }

                        if (showCreateDialog) {
                            var title by remember { mutableStateOf("") }
                            var description by remember { mutableStateOf("") }

                            AlertDialog(
                                onDismissRequest = { showCreateDialog = false },
                                title = { Text(strings.libraryScreen.newCollection, color = TextPrimary, fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = title,
                                            onValueChange = { title = it },
                                            label = { Text(strings.libraryScreen.title, color = TextSecondary) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = ElectricViolet,
                                                unfocusedBorderColor = CardBorder,
                                                focusedContainerColor = SurfaceDark,
                                                unfocusedContainerColor = SurfaceDark
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = description,
                                            onValueChange = { description = it },
                                            label = { Text(strings.libraryScreen.descriptionOptional, color = TextSecondary) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                focusedBorderColor = ElectricViolet,
                                                unfocusedBorderColor = CardBorder,
                                                focusedContainerColor = SurfaceDark,
                                                unfocusedContainerColor = SurfaceDark
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (title.isNotBlank()) {
                                                viewModel.createCollection(title, description)
                                                showCreateDialog = false
                                            }
                                        },
                                        enabled = title.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricViolet,
                                            disabledContainerColor = ElectricViolet.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(strings.libraryScreen.create, color = BackgroundDark, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCreateDialog = false }) {
                                        Text(strings.libraryScreen.cancel, color = TextSecondary)
                                    }
                                },
                                containerColor = SurfaceCardDark,
                                shape = RoundedCornerShape(24.dp)
                            )
                        }

                        val collectionsLazyGridState = rememberLazyGridState()
                        LazyVerticalGrid(
                            state = collectionsLazyGridState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 100.dp), // Space for bottom navigation
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .disableSplitTouch()
                        ) {
                            item {
                                CreationSlotCard(onClick = { showCreateDialog = true })
                            }
                            items(
                                items = collectionsList,
                                key = { it.collection.id }
                            ) { item ->
                                CollectionBentoCard(
                                    collection = item.collection,
                                    posters = item.posters,
                                    totalCount = item.totalCount,
                                    statusCounts = item.statusCounts,
                                    onClick = {
                                        navController.navigate(Screen.CollectionDetail(item.collection.id))
                                    },
                                    onLongClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FranchiseBentoCard(
    item: FranchiseItem,
    preferUk: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val countText = strings.detailScreen.releasesCount.getPlural(item.releases.size)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Overlapping Cover Stack Box
        Box(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val covers = item.releases.mapNotNull { it.coverLarge }.take(3)
            
            if (covers.size >= 3) {
                // Cover 3 (deepest background, rotated left)
                AsyncImage(
                    model = covers[2],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .graphicsLayer {
                            translationX = -18.dp.toPx()
                            scaleX = 0.82f
                            scaleY = 0.82f
                            rotationZ = -12f
                            alpha = 0.5f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
                // Cover 2 (middle background, rotated right)
                AsyncImage(
                    model = covers[1],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .graphicsLayer {
                            translationX = 18.dp.toPx()
                            scaleX = 0.88f
                            scaleY = 0.88f
                            rotationZ = 12f
                            alpha = 0.8f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
                // Cover 1 (foreground)
                AsyncImage(
                    model = covers[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(52.dp)
                        .height(74.dp)
                        .graphicsLayer {
                            scaleX = 1.0f
                            scaleY = 1.0f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
            } else if (covers.size == 2) {
                // Cover 2 (background, rotated right)
                AsyncImage(
                    model = covers[1],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(48.dp)
                        .height(68.dp)
                        .graphicsLayer {
                            translationX = 16.dp.toPx()
                            scaleX = 0.88f
                            scaleY = 0.88f
                            rotationZ = 10f
                            alpha = 0.8f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
                // Cover 1 (foreground)
                AsyncImage(
                    model = covers[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(52.dp)
                        .height(74.dp)
                        .graphicsLayer {
                            scaleX = 1.0f
                            scaleY = 1.0f
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
            } else if (covers.isNotEmpty()) {
                // Standard single cover fallback
                AsyncImage(
                    model = covers[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(52.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Side: Title & Badge
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.franchise.getDisplayName(preferUk) ?: "",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Releases badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x1A8A2BE2))
                    .border(1.dp, ElectricViolet.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countText,
                    color = CyberTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WindowsSegmentedProgressBar(
    totalCount: Int,
    statusCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressPulse by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                -0.5f at 0
                1.5f at 3000
                1.5f at 6000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .drawBehind {
                val width = size.width
                val height = size.height

                // Draw base background (dark grey)
                drawRect(color = Color(0xFF2A2A32), size = size)

                if (totalCount == 0) return@drawBehind

                val activeStatuses = statusConfigs.map { it.id }
                var currentX = 0f
                var filledWidth = 0f

                activeStatuses.forEach { statusId ->
                    val count = statusCounts[statusId] ?: 0
                    if (count > 0) {
                        val segmentWidth = (count.toFloat() / totalCount) * width
                        val color = AnimeStatusColors[statusId] ?: Color.Gray
                        drawRect(
                            color = color,
                            topLeft = Offset(currentX, 0f),
                            size = androidx.compose.ui.geometry.Size(segmentWidth, height)
                        )
                        currentX += segmentWidth
                        filledWidth += segmentWidth
                    }
                }

                // Draw Windows copier pulse on the filled segments
                if (filledWidth > 0f) {
                    val shimmerWidth = width * 0.8f
                    val startX = progressPulse * (filledWidth + shimmerWidth) - shimmerWidth
                    val endX = startX + shimmerWidth

                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.0f)
                        ),
                        start = Offset(startX, 0f),
                        end = Offset(endX, 0f)
                    )

                    drawRect(
                        brush = shimmerBrush,
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(filledWidth, height)
                    )
                }
            }
    )
}

@Composable
fun CreationSlotCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark.copy(alpha = 0.3f))
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val color = ElectricViolet.copy(alpha = 0.5f)
                val pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(12f, 8f), 0f
                )
                val radius = 24.dp.toPx()
                drawRoundRect(
                    color = color,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = pathEffect
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = strings.libraryScreen.newCollection,
                tint = ElectricViolet,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.libraryScreen.newCollection,
                color = ElectricViolet,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionBentoCard(
    collection: CollectionEntity,
    posters: List<String>,
    totalCount: Int,
    statusCounts: Map<String, Int>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 3D Fan-Out Poster Stack Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (posters.size >= 3) {
                    // Cover 3 (rotated left)
                    AsyncImage(
                        model = posters[2],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(55.dp)
                            .height(80.dp)
                            .graphicsLayer {
                                translationX = -22.dp.toPx()
                                scaleX = 0.82f
                                scaleY = 0.82f
                                rotationZ = -12f
                                alpha = 0.5f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                    // Cover 2 (rotated right)
                    AsyncImage(
                        model = posters[1],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(55.dp)
                            .height(80.dp)
                            .graphicsLayer {
                                translationX = 22.dp.toPx()
                                scaleX = 0.88f
                                scaleY = 0.88f
                                rotationZ = 12f
                                alpha = 0.8f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                    // Cover 1 (foreground)
                    AsyncImage(
                        model = posters[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .graphicsLayer {
                                scaleX = 1.0f
                                scaleY = 1.0f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                } else if (posters.size == 2) {
                    // Cover 2 (rotated right)
                    AsyncImage(
                        model = posters[1],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(55.dp)
                            .height(80.dp)
                            .graphicsLayer {
                                translationX = 18.dp.toPx()
                                scaleX = 0.88f
                                scaleY = 0.88f
                                rotationZ = 10f
                                alpha = 0.8f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                    // Cover 1 (foreground)
                    AsyncImage(
                        model = posters[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .graphicsLayer {
                                scaleX = 1.0f
                                scaleY = 1.0f
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                } else if (posters.isNotEmpty()) {
                    AsyncImage(
                        model = posters[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )
                } else {
                    // Placeholder card
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(88.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ElectricViolet.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Lower half: Titles & Progress
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = collection.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))

                val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
                Text(
                    text = if (collection.description.isNotBlank()) collection.description else strings.libraryScreen.noDescription,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titlesText = strings.libraryScreen.titlesCount.getPlural(totalCount)
                    Text(
                        text = titlesText,
                        color = ElectricViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                WindowsSegmentedProgressBar(
                    totalCount = totalCount,
                    statusCounts = statusCounts,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
