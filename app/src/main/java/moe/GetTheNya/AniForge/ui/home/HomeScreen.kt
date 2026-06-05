package moe.GetTheNya.AniForge.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    // Animation presets
    val currentTitleStyle by viewModel.titleAnimStyle.collectAsState()
    val currentSubtitleStyle by viewModel.subtitleAnimStyle.collectAsState()
    val currentContentStyle by viewModel.contentAnimStyle.collectAsState()

    // Capture the session-wide played flag on first composition to prevent race condition during database load
    val isAnimationPlayed = remember { viewModel.isHomeAnimationPlayed }

    // Mark as played in a session-wide LaunchedEffect once composed
    LaunchedEffect(Unit) {
        viewModel.isHomeAnimationPlayed = true
    }

    // A. Dynamic Title Animation Logic
    val titleText = strings.homeScreen.appName
    val cipherPool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@&?%*"
    var decodedTitle by remember(titleText, currentTitleStyle) {
        mutableStateOf(
            if (isAnimationPlayed || currentTitleStyle != TitleAnimStyle.DECODING) {
                titleText
            } else {
                // Initialize with fully random characters of same length to prevent layout shift
                CharArray(titleText.length) { cipherPool.random() }.concatToString()
            }
        )
    }

    LaunchedEffect(titleText, currentTitleStyle) {
        if (!isAnimationPlayed && currentTitleStyle == TitleAnimStyle.DECODING) {
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
        } else {
            decodedTitle = titleText
        }
    }

    var titleGlitchAlpha by remember { mutableStateOf(1f) }
    var titleGlitchTranslationX by remember { mutableStateOf(0f) }

    LaunchedEffect(titleText, currentTitleStyle) {
        if (!isAnimationPlayed && currentTitleStyle == TitleAnimStyle.GLITCH) {
            val duration = 300L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= duration) break
                titleGlitchAlpha = kotlin.random.Random.nextFloat() * 0.6f + 0.4f
                titleGlitchTranslationX = kotlin.random.Random.nextInt(-6, 7).toFloat()
                delay(30L)
            }
            titleGlitchAlpha = 1f
            titleGlitchTranslationX = 0f
        } else {
            titleGlitchAlpha = 1f
            titleGlitchTranslationX = 0f
        }
    }

    val titleProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!isAnimationPlayed) {
            delay(200L)
            titleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    // B. Greeting & Subtext Smooth Unblur & Alpha Fade progress
    val cleanSubtitle = remember(randomSubtitle) {
        randomSubtitle?.let {
            it.replace(Regex("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        } ?: ""
    }

    var typedGreeting by remember(strings.homeScreen.welcomeBack, currentSubtitleStyle) {
        mutableStateOf(
            if (isAnimationPlayed || currentSubtitleStyle != SubtitleAnimStyle.TYPEWRITER) {
                strings.homeScreen.welcomeBack.replace("!", "")
            } else {
                ""
            }
        )
    }

    var typedSubtitle by remember(cleanSubtitle, currentSubtitleStyle) {
        mutableStateOf(
            if (isAnimationPlayed || currentSubtitleStyle != SubtitleAnimStyle.TYPEWRITER) {
                cleanSubtitle
            } else {
                ""
            }
        )
    }

    var cursorPosition by remember { mutableStateOf(0) }
    var cursorBlink by remember { mutableStateOf(false) }

    LaunchedEffect(cursorPosition) {
        if (cursorPosition != 0) {
            while (true) {
                cursorBlink = !cursorBlink
                delay(300L)
            }
        } else {
            cursorBlink = false
        }
    }

    LaunchedEffect(strings.homeScreen.welcomeBack, cleanSubtitle, currentSubtitleStyle) {
        if (!isAnimationPlayed && currentSubtitleStyle == SubtitleAnimStyle.TYPEWRITER) {
            val fullGreeting = strings.homeScreen.welcomeBack.replace("!", "")
            typedGreeting = ""
            typedSubtitle = ""
            cursorPosition = 1
            delay(200L)
            for (i in 1..fullGreeting.length) {
                typedGreeting = fullGreeting.substring(0, i)
                delay(25L)
            }
            if (cleanSubtitle.isNotEmpty()) {
                delay(100L)
                cursorPosition = 2
                for (i in 1..cleanSubtitle.length) {
                    typedSubtitle = cleanSubtitle.substring(0, i)
                    delay(25L)
                }
            }
            delay(800L)
            cursorPosition = 0
        } else {
            typedGreeting = strings.homeScreen.welcomeBack.replace("!", "")
            typedSubtitle = cleanSubtitle
            cursorPosition = 0
        }
    }

    val headerProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(currentSubtitleStyle) {
        if (!isAnimationPlayed && (currentSubtitleStyle == SubtitleAnimStyle.BLUR_FADE || currentSubtitleStyle == SubtitleAnimStyle.WORD_BY_WORD)) {
            delay(200L) // breathing room delay (synchronized with title decoding)
            headerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearOutSlowInEasing
                )
            )
        }
    }

    // C. Digital Power-Up for Bento widgets
    val contentProgress = remember { Animatable(if (isAnimationPlayed) 1f else 0f) }
    LaunchedEffect(currentContentStyle) {
        if (!isAnimationPlayed && currentContentStyle != ContentAnimStyle.NONE) {
            delay(180L) // 180ms delay from launch
            val duration = if (currentContentStyle == ContentAnimStyle.FLIP_3D) 450 else 400
            contentProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
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
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    if (!isAnimationPlayed) {
                        when (currentTitleStyle) {
                            TitleAnimStyle.NONE -> {
                                alpha = 1f
                                translationX = 0f
                                rotationY = 0f
                            }
                            TitleAnimStyle.SLIDE_SIDE -> {
                                val progress = titleProgress.value
                                alpha = progress
                                translationX = -100f * (1f - progress)
                                rotationY = 0f
                            }
                            TitleAnimStyle.TURNSTILE_3D -> {
                                val progress = titleProgress.value
                                alpha = progress
                                rotationY = 25f * (1f - progress)
                                translationX = 0f
                            }
                            TitleAnimStyle.DECODING -> {
                                alpha = 1f
                                translationX = 0f
                                rotationY = 0f
                            }
                            TitleAnimStyle.GLITCH -> {
                                alpha = titleGlitchAlpha
                                translationX = titleGlitchTranslationX
                                rotationY = 0f
                            }
                        }
                    } else {
                        alpha = 1f
                        translationX = 0f
                        rotationY = 0f
                    }
                }
            )
            Spacer(modifier = Modifier.height(0.dp))

            when (currentSubtitleStyle) {
                SubtitleAnimStyle.NONE -> {
                    Column {
                        Text(
                            text = strings.homeScreen.welcomeBack.replace("!", ""),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                            color = TextSecondary
                        )
                        if (cleanSubtitle.isNotEmpty()) {
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
                SubtitleAnimStyle.BLUR_FADE -> {
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
                        if (cleanSubtitle.isNotEmpty()) {
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
                SubtitleAnimStyle.WORD_BY_WORD -> {
                    Column {
                        WordByWordText(
                            text = strings.homeScreen.welcomeBack.replace("!", ""),
                            animProgress = { headerProgress.value },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Light,
                            color = TextSecondary
                        )
                        if (cleanSubtitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            WordByWordText(
                                text = cleanSubtitle,
                                animProgress = { headerProgress.value },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                SubtitleAnimStyle.TYPEWRITER -> {
                    val fullGreeting = strings.homeScreen.welcomeBack.replace("!", "")
                    Column {
                        Box(contentAlignment = Alignment.TopStart) {
                            Text(
                                text = fullGreeting,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.Transparent,
                                modifier = Modifier.alpha(0f)
                            )
                            Text(
                                text = if (cursorPosition == 1 && cursorBlink) typedGreeting + "▎" else typedGreeting,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = TextSecondary
                            )
                        }
                        if (cleanSubtitle.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(contentAlignment = Alignment.TopStart) {
                                Text(
                                    text = cleanSubtitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.Transparent,
                                    modifier = Modifier.alpha(0f)
                                )
                                Text(
                                    text = if (cursorPosition == 2 && cursorBlink) typedSubtitle + "▎" else typedSubtitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = TextSecondary.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
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
                                when (currentContentStyle) {
                                    ContentAnimStyle.NONE -> {
                                        alpha = 1f
                                        scaleX = 1f
                                        scaleY = 1f
                                        translationY = 0f
                                        rotationX = 0f
                                    }
                                    ContentAnimStyle.POWER_UP -> {
                                        val scale = 0.95f + 0.05f * progress
                                        scaleX = scale
                                        scaleY = scale
                                        translationY = 0f
                                        rotationX = 0f
                                    }
                                    ContentAnimStyle.SLIDE_UP -> {
                                        scaleX = 1f
                                        scaleY = 1f
                                        translationY = 80.dp.toPx() * (1f - progress)
                                        rotationX = 0f
                                    }
                                    ContentAnimStyle.FLIP_3D -> {
                                        scaleX = 1f
                                        scaleY = 1f
                                        translationY = 0f
                                        rotationX = 90f * (1f - progress)
                                        cameraDistance = 12f * density
                                    }
                                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordByWordText(
    text: String,
    animProgress: () -> Float,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified
) {
    val words = remember(text) { text.split(" ") }
    val wordCount = words.size

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        words.forEachIndexed { index, word ->
            val wordAlpha = remember(index, wordCount) {
                derivedStateOf {
                    val progress = animProgress()
                    val start = index.toFloat() / wordCount
                    val end = (index + 1).toFloat() / wordCount
                    ((progress - start) / (end - start)).coerceIn(0f, 1f)
                }
            }
            Text(
                text = word,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha.value
                }
            )
        }
    }
}
