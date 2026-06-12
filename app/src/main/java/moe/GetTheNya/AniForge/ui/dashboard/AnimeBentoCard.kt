package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect
import android.graphics.Shader
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.theme.CardBorder
import moe.GetTheNya.AniForge.ui.theme.NeonCoral
import moe.GetTheNya.AniForge.ui.theme.SurfaceCardDark
import moe.GetTheNya.AniForge.ui.theme.TextPrimary
import moe.GetTheNya.AniForge.ui.theme.TextSecondary
import moe.GetTheNya.AniForge.ui.utils.statusConfigs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimeBentoCard(
    anime: Anime,
    onGestureActionTriggered: (action: QuickGestureAction, value: Any?) -> Unit,
    modifier: Modifier = Modifier,
    preferUk: Boolean = true,
    status: String? = null,
    onStatusChange: ((String) -> Unit)? = null,
    isMenuVisible: Boolean = false,
    onMenuDismiss: (() -> Unit)? = null,
    initialScore: Double? = null,
    initialEpisode: Int = 0,
    onDragStateChanged: ((Boolean) -> Unit)? = null,
    onSliderStateChanged: ((Boolean) -> Unit)? = null,
    gestureCenter: QuickGestureAction = QuickGestureAction.Immediate.OpenDetails,
    gestureUp: QuickGestureAction = QuickGestureAction.Continuous.EpisodeSlider,
    gestureDown: QuickGestureAction = QuickGestureAction.Continuous.ScoreSlider,
    gestureLeft: QuickGestureAction = QuickGestureAction.Immediate.OpenWatchStatusPicker,
    gestureRight: QuickGestureAction = QuickGestureAction.Immediate.ShareLink,
    clickAction: QuickGestureAction = QuickGestureAction.Immediate.OpenDetails
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current

    val posterScale by animateFloatAsState(
        targetValue = if (isMenuVisible) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "posterScale"
    )

    val blurRadius by animateDpAsState(
        targetValue = if (isMenuVisible) 12.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "blurRadius"
    )

    QuickGestureWrapper(
        anime = anime,
        initialScore = initialScore,
        initialEpisode = initialEpisode,
        gestureCenter = gestureCenter,
        gestureUp = gestureUp,
        gestureDown = gestureDown,
        gestureLeft = gestureLeft,
        gestureRight = gestureRight,
        onGestureActionTriggered = onGestureActionTriggered,
        onDragStateChanged = { onDragStateChanged?.invoke(it) },
        onSliderStateChanged = { onSliderStateChanged?.invoke(it) },
        clickAction = clickAction,
        modifier = modifier,
        enabled = !isMenuVisible
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCardDark)
                .border(1.dp, if (isMenuVisible) Color.Transparent else CardBorder, RoundedCornerShape(24.dp))
        ) {
            // Grouped Content Layer - Applies scale and blur transformations to all visible card elements as a unit
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = posterScale
                        scaleY = posterScale
                        clip = true
                        shape = RoundedCornerShape(24.dp)
                        if (blurRadius.value > 0f) {
                            val px = blurRadius.toPx()
                            this.renderEffect = RenderEffect.createBlurEffect(
                                px,
                                px,
                                Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        } else {
                            this.renderEffect = null
                        }
                    }
            ) {
                // Background cover artwork (Coil exact resizing & GPU hardware optimization)
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(anime.coverLarge)
                        .precision(coil.size.Precision.EXACT)
                        .allowHardware(true)
                        .crossfade(true)
                        .build(),
                    contentDescription = anime.titleRomaji,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )

                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x88000000),
                                    Color(0xCC000000)
                                ),
                                startY = 100f
                            )
                        )
                )
         
                // Status Badge (top start/left)
                if (status != null && status != "NONE") {
                    val config = statusConfigs.find { it.id == status }
                    if (config != null) {
                        Text(
                            text = config.getLabel(strings).uppercase(),
                            color = config.color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xE60C0C0E)) // premium, semi-transparent background color (dark glassmorphism)
                                .border(1.dp, config.color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Score tag (top right)
                if (anime.scoreMal != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xE60C0C0E))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = strings.misc.score,
                            tint = NeonCoral,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", anime.scoreMal),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Info details (bottom)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    // Format & translation status badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val format = anime.format
                        if (format != null) {
                            Text(
                                text = format.uppercase(),
                                color = NeonCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FF2E93))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (anime.hasUkTranslation && preferUk) {
                            Text(
                                text = "🇺🇦",
                                color = Color(0xFF00F5D4),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x3300F5D4))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Main Title
                    Text(
                        text = anime.getDisplayTitle(preferUk = preferUk),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Year & Season info
                    if (anime.seasonYear != null) {
                        Text(
                            text = "${anime.season ?: ""} ${anime.seasonYear}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick status menu overlay inside the card Box
            AnimatedVisibility(
                visible = isMenuVisible,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ),
                exit = fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.9f, animationSpec = tween(100)),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer() // performance layer isolation
            ) {
                // Dismiss barrier covering the entire card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onMenuDismiss?.invoke()
                        }
                ) {
                    // Glassmorphic dark container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.50f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onMenuDismiss?.invoke()
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.detailScreen.myProgress.uppercase(),
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            statusConfigs.forEachIndexed { index, config ->
                                key(config.id) {
                                    var itemVisible by remember(isMenuVisible) { mutableStateOf(false) }
                                    LaunchedEffect(isMenuVisible) {
                                        if (isMenuVisible) {
                                            delay(index * 35L)
                                            itemVisible = true
                                        } else {
                                            itemVisible = false
                                        }
                                    }
                                    val itemAlpha by animateFloatAsState(
                                        targetValue = if (itemVisible) 1f else 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "itemAlpha_$index"
                                    )
                                    val itemScale by animateFloatAsState(
                                        targetValue = if (itemVisible) 1f else 0.85f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        ),
                                        label = "itemScale_$index"
                                    )

                                    val isSelected = status == config.id
                                    val itemColor = config.color
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp)
                                            .graphicsLayer {
                                                alpha = itemAlpha
                                                scaleX = itemScale
                                                scaleY = itemScale
                                            }
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) itemColor.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                onStatusChange?.invoke(config.id)
                                                onMenuDismiss?.invoke()
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) config.activeIcon else config.inactiveIcon,
                                            contentDescription = config.getLabel(strings),
                                            tint = if (isSelected) itemColor else Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = config.getLabel(strings),
                                            color = if (isSelected) itemColor else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
