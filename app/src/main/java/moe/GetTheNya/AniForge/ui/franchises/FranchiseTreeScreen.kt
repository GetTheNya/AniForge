package moe.GetTheNya.AniForge.ui.franchises

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Brush
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.AnimeStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FranchiseTreeScreen(
    franchiseId: Long,
    viewModel: FranchiseTreeViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(franchiseId) {
        viewModel.loadFranchiseTree(franchiseId)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when (val state = uiState) {
            is FranchiseTreeUiState.Loading -> {
                CircularProgressIndicator(
                    color = ElectricViolet,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is FranchiseTreeUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "${strings.misc.error}: ${state.message}", color = NeonCoral, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadTree() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Text(strings.misc.retry)
                    }
                }
            }
            is FranchiseTreeUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Header Row
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.franchise.getDisplayName(preferUk) ?: "",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Timeline List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
                    ) {
                        itemsIndexed(
                            items = state.nodes,
                            key = { _, node -> node.anime.anilistId }
                        ) { index, node ->
                            val status = state.trackingMap[node.anime.anilistId]
                            val watchColor = AnimeStatusColors[status]
                            
                            TimelineRow(
                                node = node,
                                watchStatusColor = watchColor,
                                isFirst = index == 0,
                                isLast = index == state.nodes.lastIndex,
                                preferUk = preferUk,
                                onClick = {
                                    navController.navigate(Screen.Detail(node.anime.anilistId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineRow(
    node: FranchiseNode,
    watchStatusColor: Color?,
    isFirst: Boolean,
    isLast: Boolean,
    preferUk: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    
    // Spine offset definition (Spine is at x = 40.dp)
    val spineXOffset = 40.dp
    val targetCardOffset = if (node.isMainAxis) 70.dp else 100.dp
    
    // Setup pulsing animation if focused
    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseGlowRadius by if (node.isCurrentFocus) {
        infiniteTransition.animateFloat(
            initialValue = 10f,
            targetValue = 28f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_glow"
        )
    } else {
        remember { mutableStateOf(12f) }
    }

    val pulseGlowAlpha by if (node.isCurrentFocus) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val glowColor = watchStatusColor ?: if (node.isCurrentFocus) ElectricViolet else null
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .drawBehind {
                val spinePx = spineXOffset.toPx()
                val middleY = size.height / 2
                
                // Draw vertical Spine line segment (Electric Violet)
                val startY = if (isFirst) middleY else 0f
                val endY = if (isLast) middleY else size.height
                drawLine(
                    color = ElectricViolet,
                    start = Offset(spinePx, startY),
                    end = Offset(spinePx, endY),
                    strokeWidth = 3.dp.toPx()
                )
                
                // Draw horizontal connector line from Spine directly to card bounds
                drawLine(
                    color = if (node.isMainAxis) ElectricViolet else ElectricViolet.copy(alpha = 0.7f),
                    start = Offset(spinePx, middleY),
                    end = Offset(targetCardOffset.toPx(), middleY),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw clean node dot centered on the spine
                val dotColor = watchStatusColor ?: Color.White.copy(alpha = 0.4f)
                drawCircle(
                    color = SurfaceDark,
                    radius = 5.dp.toPx(),
                    center = Offset(spinePx, middleY)
                )
                drawCircle(
                    color = dotColor,
                    radius = 5.dp.toPx(),
                    center = Offset(spinePx, middleY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = targetCardOffset,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp
                )
        ) {
            // Anime Details Card with high-performance hardware-accelerated outer neon glow
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Emit outer neon glow bleeding out beyond physical bounds on the RenderThread
                        if (glowColor != null) {
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = glowColor.copy(alpha = pulseGlowAlpha).toArgb()
                                    isAntiAlias = true
                                    maskFilter = BlurMaskFilter(
                                        pulseGlowRadius.dp.toPx(),
                                        BlurMaskFilter.Blur.OUTER
                                    )
                                }
                                canvas.nativeCanvas.drawRoundRect(
                                    0f, 0f, size.width, size.height,
                                    16.dp.toPx(), 16.dp.toPx(),
                                    paint
                                )
                            }
                        }
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = if (node.isCurrentFocus) 1.5.dp else 1.dp,
                        color = if (node.isCurrentFocus) ElectricViolet else CardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .then(
                        if (node.isCurrentFocus) {
                            Modifier
                        } else {
                            Modifier.clickable { onClick() }
                        }
                    ),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Poster
                    AsyncImage(
                        model = node.anime.coverLarge,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(44.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Metadata Details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        val label = strings.relationTypes[node.relationType.name] ?: node.relationType.name
                        Text(
                            text = label.uppercase(),
                            color = if (node.isMainAxis) NeonCoral else CyberTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = node.anime.getDisplayTitle(preferUk = preferUk),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val yearFormat = buildString {
                            if (node.anime.seasonYear != null) {
                                append(node.anime.seasonYear)
                            } else if (node.anime.startDateYear != null) {
                                append(node.anime.startDateYear)
                            }
                            if (node.anime.format != null) {
                                if (isNotEmpty()) append(" • ")
                                append(node.anime.format)
                            }
                        }
                        if (yearFormat.isNotEmpty()) {
                            Text(
                                text = yearFormat,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (node.isCurrentFocus) {
                // Highly polished solid, flat, minimalist "You Are Here" badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .offset(x = 6.dp, y = (-6).dp)
                        .background(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = CardBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = strings.franchisesScreen.youAreHere.uppercase(),
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
