package moe.GetTheNya.AniForge.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.GetTheNya.AniForge.ui.utils.StatusItemConfig
import moe.GetTheNya.AniForge.ui.utils.statusConfigs

@Composable
fun AnimeStatsPieChart(
    stats: Map<String, Int>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    configs: List<StatusItemConfig> = statusConfigs
) {
    val totalAnime = remember(stats) { stats.values.sum() }

    // Isolate the animation progress state to avoid triggering recompositions on parent layout.
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(stats) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val halfStroke = strokeWidthPx / 2f
        val arcSize = Size(
            width = size.width - strokeWidthPx,
            height = size.height - strokeWidthPx
        )
        val topLeft = Offset(halfStroke, halfStroke)

        if (totalAnime == 0) {
            // Draw a beautiful empty state circle
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx)
            )
        } else {
            var currentStartAngle = -90f
            val progress = animationProgress.value

            configs.forEach { config ->
                val count = stats[config.id] ?: 0
                if (count > 0) {
                    val sweepAngle = (count.toFloat() / totalAnime) * 360f * progress
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = config.color,
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                        )
                        currentStartAngle += sweepAngle
                    }
                }
            }
        }
    }
}
