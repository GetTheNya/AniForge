package moe.GetTheNya.AniForge.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.ui.theme.NeonCoral
import moe.GetTheNya.AniForge.ui.utils.ImageCacheManager
import java.io.File

@Composable
fun ImageViewerScreen(
    urls: List<String>,
    initialIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeScale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        if (urls.size <= 1) {
            // Single image mode
            val url = urls.firstOrNull() ?: ""
            ZoomableImage(
                url = url,
                isSelected = true,
                onScaleChanged = { activeScale = it }
            )
        } else {
            // Carousel Mode
            val pagerState = rememberPagerState(initialPage = initialIndex) { urls.size }

            // Reset scale when swiping between pages
            LaunchedEffect(pagerState.currentPage) {
                activeScale = 1f
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = activeScale <= 1.0f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val isSelected = pagerState.currentPage == page
                ZoomableImage(
                    url = urls[page],
                    isSelected = isSelected,
                    onScaleChanged = { scale ->
                        if (isSelected) {
                            activeScale = scale
                        }
                    }
                )
            }

            // Page count indicator
            Text(
                text = "${pagerState.currentPage + 1} / ${urls.size}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x990C0C0E))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Immersive close button overlay
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x990C0C0E))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}

@Composable
fun ZoomableImage(
    url: String,
    isSelected: Boolean,
    onScaleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cachedFile by remember(url) { mutableStateOf<File?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }

    // Fetch and process image in caching pipeline off main thread
    LaunchedEffect(url) {
        isLoading = true
        cachedFile = ImageCacheManager.getCachedImage(context, url)
        isLoading = false
    }

    val scaleAnimatable = remember { Animatable(1f) }
    val offsetXAnimatable = remember { Animatable(0f) }
    val offsetYAnimatable = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Graceful scale-in entrance transition
    val entranceScale = remember { Animatable(0.7f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(url) {
        launch {
            entranceScale.animateTo(1f, tween(durationMillis = 200))
        }
        launch {
            entranceAlpha.animateTo(1f, tween(durationMillis = 200))
        }
    }

    // Reset local zoom/pan state when this page becomes inactive
    LaunchedEffect(isSelected) {
        if (!isSelected) {
            scaleAnimatable.snapTo(1f)
            offsetXAnimatable.snapTo(0f)
            offsetYAnimatable.snapTo(0f)
            onScaleChanged(1f)
        }
    }

    var imageSize by remember { mutableStateOf<IntSize?>(null) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()

        // Helper to compute exact pan/drag boundaries based on image aspect ratio
        fun computeMaxOffset(currentScale: Float): Offset {
            val size = imageSize ?: return Offset.Zero
            val imageW = size.width.toFloat()
            val imageH = size.height.toFloat()
            if (imageW <= 0 || imageH <= 0) return Offset.Zero

            val containerRatio = containerW / containerH
            val imageRatio = imageW / imageH

            val fitWidth: Float
            val fitHeight: Float
            if (imageRatio > containerRatio) {
                fitWidth = containerW
                fitHeight = containerW / imageRatio
            } else {
                fitHeight = containerH
                fitWidth = containerH * imageRatio
            }

            val maxOffsetX = ((fitWidth * currentScale) - containerW).coerceAtLeast(0f) / 2f
            val maxOffsetY = ((fitHeight * currentScale) - containerH).coerceAtLeast(0f) / 2f
            return Offset(maxOffsetX, maxOffsetY)
        }

        if (isLoading) {
            CircularProgressIndicator(
                color = NeonCoral,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val imageModel = cachedFile ?: url

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    val drawable = state.result.drawable
                    imageSize = IntSize(drawable.intrinsicWidth, drawable.intrinsicHeight)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scaleAnimatable.value * entranceScale.value
                        scaleY = scaleAnimatable.value * entranceScale.value
                        translationX = offsetXAnimatable.value
                        translationY = offsetYAnimatable.value
                        alpha = entranceAlpha.value
                    }
                    .pointerInput(containerW, containerH) {
                        detectTapGestures(
                            onDoubleTap = { centroid ->
                                val targetScale = if (scaleAnimatable.value > 1.05f) 1f else 3f
                                val targetOffset = if (targetScale == 1f) {
                                    Offset.Zero
                                } else {
                                    val centroidPx = centroid - Offset(containerW / 2f, containerH / 2f)
                                    val maxOffset = computeMaxOffset(targetScale)
                                    val rawOffset = -centroidPx * (targetScale - 1f)
                                    Offset(
                                        x = rawOffset.x.coerceIn(-maxOffset.x, maxOffset.x),
                                        y = rawOffset.y.coerceIn(-maxOffset.y, maxOffset.y)
                                    )
                                }

                                coroutineScope.launch {
                                    launch {
                                        scaleAnimatable.animateTo(
                                            targetScale,
                                            spring(dampingRatio = 0.8f, stiffness = 300f)
                                        )
                                        onScaleChanged(targetScale)
                                    }
                                    launch {
                                        offsetXAnimatable.animateTo(
                                            targetOffset.x,
                                            spring(dampingRatio = 0.8f, stiffness = 300f)
                                        )
                                    }
                                    launch {
                                        offsetYAnimatable.animateTo(
                                            targetOffset.y,
                                            spring(dampingRatio = 0.8f, stiffness = 300f)
                                        )
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(containerW, containerH) {
                        detectZoomAndPan(
                            currentScale = { scaleAnimatable.value },
                            onGesture = { centroid, pan, zoom ->
                                val currentScale = scaleAnimatable.value
                                val targetScale = (currentScale * zoom).coerceIn(1f, 5f)
                                
                                // NaN and Infinity Guard
                                if (!targetScale.isSane()) return@detectZoomAndPan
                                val scaleChange = if (currentScale > 0f) targetScale / currentScale else 1f
                                if (!scaleChange.isSane()) return@detectZoomAndPan

                                if (!centroid.isSane() || !pan.isSane()) return@detectZoomAndPan
                                val centroidPx = centroid - Offset(containerW / 2f, containerH / 2f)

                                val currentOffset = Offset(offsetXAnimatable.value, offsetYAnimatable.value)
                                if (!currentOffset.isSane()) return@detectZoomAndPan

                                val speedMultiplier = targetScale
                                val targetOffset = (currentOffset - centroidPx) * scaleChange + centroidPx + (pan * speedMultiplier)
                                if (!targetOffset.isSane()) return@detectZoomAndPan

                                val maxOffset = computeMaxOffset(targetScale)
                                val boundedOffset = Offset(
                                    x = targetOffset.x.coerceIn(-maxOffset.x, maxOffset.x),
                                    y = targetOffset.y.coerceIn(-maxOffset.y, maxOffset.y)
                                )
                                if (!boundedOffset.isSane()) return@detectZoomAndPan

                                coroutineScope.launch {
                                    scaleAnimatable.snapTo(targetScale)
                                    offsetXAnimatable.snapTo(boundedOffset.x)
                                    offsetYAnimatable.snapTo(boundedOffset.y)
                                    onScaleChanged(targetScale)
                                }
                            },
                            onGestureEnd = {
                                // Smooth reset on release if scale fell below 1.0f or is near baseline
                                val currentScale = scaleAnimatable.value
                                if (currentScale < 1.05f) {
                                    coroutineScope.launch {
                                        launch {
                                            scaleAnimatable.animateTo(1f, spring())
                                            onScaleChanged(1f)
                                        }
                                        launch {
                                            offsetXAnimatable.animateTo(0f, spring())
                                        }
                                        launch {
                                            offsetYAnimatable.animateTo(0f, spring())
                                        }
                                    }
                                }
                            }
                        )
                    }
            )
        }
    }
}

/**
 * Custom transform gesture detector that delegates single-finger dragging to parent containers (like HorizontalPager)
 * when scale is 1.0f, but intercepts and consumes all zoom/pan gestures locally when zoomed in (scale > 1.0f).
 */
suspend fun PointerInputScope.detectZoomAndPan(
    currentScale: () -> Float,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onGestureEnd: () -> Unit
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val pointerCount = event.changes.size
                val isZooming = pointerCount > 1
                val isZoomedIn = currentScale() > 1.01f

                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (isZoomedIn) {
                        if (panMotion > touchSlop || zoomMotion > touchSlop) {
                            pastTouchSlop = true
                        }
                    } else {
                        // When zoomed out, only pinch zoom triggers gesture start
                        if (isZooming && zoomMotion > touchSlop) {
                            pastTouchSlop = true
                        }
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    onGesture(centroid, panChange, zoomChange)

                    // Consume the event to prevent swiping/dismissing parent containers
                    if (isZoomedIn || isZooming) {
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        onGestureEnd()
    }
}

private fun Offset.isSane(): Boolean = this != Offset.Unspecified && !x.isNaN() && !x.isInfinite() && !y.isNaN() && !y.isInfinite()
private fun Float.isSane(): Boolean = !this.isNaN() && !this.isInfinite()
