package moe.GetTheNya.AniForge.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
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
    val randomSubtitle by viewModel.randomWelcomeSubtitle.collectAsState()

    // Capture the session-wide played flag on first composition to prevent race condition during database load
    val isAnimationPlayed = remember { viewModel.isHomeAnimationPlayed }

    // Mark as played in a session-wide LaunchedEffect once composed
    LaunchedEffect(Unit) {
        viewModel.isHomeAnimationPlayed = true
    }

    // 1. Cyberpunk Text Decoding Effect
    val titleText = strings.homeScreen.appName
    val cipherPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@&?%*"
    var decodedTitle by remember(titleText) {
        mutableStateOf(
            if (isAnimationPlayed) {
                titleText
            } else {
                // Initialize with fully random characters of same length to prevent layout shift
                CharArray(titleText.length) { cipherPool.random() }.concatToString()
            }
        )
    }

    LaunchedEffect(titleText) {
        if (!isAnimationPlayed) {
            try {
                delay(200L)
                val duration = 380L
                val charsCount = titleText.length
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed >= duration) {
                        break
                    }
                    val percent = elapsed.toFloat() / duration
                    val lockedCount = (percent * charsCount).toInt().coerceIn(0, charsCount)
                    val randomChars = CharArray(charsCount) { i ->
                        if (i < lockedCount) {
                            titleText[i]
                        } else {
                            cipherPool.random()
                        }
                    }
                    decodedTitle = randomChars.concatToString()
                    delay(35)
                }
            } finally {
                // Guaranteed resolution: always resolve to the correct targetText upon completion/cancellation
                decodedTitle = titleText
            }
        }
    }

    // 2. Greeting & Subtext Smooth Unblur & Alpha Fade progress
    val headerProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!isAnimationPlayed) {
            delay(300L)
            headerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    // 3. Digital Power-Up for Bento widgets
    val contentProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!isAnimationPlayed) {
            delay(280L)
            contentProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Sticky Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
        ) {
            Text(
                text = decodedTitle,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(0.dp))

            Column(
                modifier = Modifier.graphicsLayer {
                    val progress = headerProgress.value
                    alpha = progress
                    val blurRadius = 15f * (1f - progress)
                    if (blurRadius > 0.1f) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            blurRadius,
                            blurRadius,
                            android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    } else {
                        renderEffect = null
                    }
                }
            ) {
                Text(
                    text = strings.homeScreen.welcomeBack.replace("!", ""),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = TextSecondary
                )
                val cleanSubtitle = randomSubtitle?.let {
                    it.replace(Regex("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }
                if (!cleanSubtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = cleanSubtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Crossfade(targetState = uiState, label = "homeCrossfade") { state ->
            when (state) {
                is HomeUiState.Loading -> {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val progress = contentProgress.value
                                alpha = progress
                                val scale = 0.95f + 0.05f * progress
                                scaleX = scale
                                scaleY = scale
                            }
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

        Spacer(modifier = Modifier.height(96.dp)) // extra padding so content isn't covered by floating bar
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
