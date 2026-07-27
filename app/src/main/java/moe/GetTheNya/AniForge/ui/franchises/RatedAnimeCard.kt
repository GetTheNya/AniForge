package moe.GetTheNya.AniForge.ui.franchises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.dashboard.AnimeBentoCard
import moe.GetTheNya.AniForge.ui.dashboard.QuickGestureAction
import moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs

@Composable
fun RatedAnimeCard(
    anime: Anime,
    score: Double,
    watchStatus: String?,
    preferUk: Boolean,
    onCardClick: () -> Unit,
    onQuickScoreEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimeBentoCard(
        anime = anime,
        userScore = if (score > 0.0) score else null,
        initialScore = if (score > 0.0) score else null,
        status = watchStatus,
        preferUk = preferUk,
        enableGestures = false,
        clickAction = QuickGestureAction.Immediate.OpenDetails,
        onGestureActionTriggered = { action, _ ->
            if (action == QuickGestureAction.Immediate.OpenDetails) {
                onCardClick()
            }
        },
        onScoreClick = onQuickScoreEdit,
        modifier = modifier
    )
}

@Composable
fun QuickScoreDialog(
    animeTitle: String,
    currentScore: Double?,
    onDismiss: () -> Unit,
    onSaveScore: (Double?) -> Unit
) {
    val strings = LocalLocaleStrings.current
    var sliderValue by remember(currentScore) { mutableStateOf((currentScore ?: 0.0).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundDark,
        title = {
            Column {
                Text(
                    text = strings.libraryScreen.yourScore,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = animeTitle,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                val (targetPrimary, targetAccent) = getScoreBadgeColors(sliderValue.toDouble())
                val primaryColor by animateColorAsState(
                    targetValue = if (sliderValue > 0f) targetPrimary else TextSecondary,
                    animationSpec = tween(durationMillis = 500),
                    label = "primaryColor"
                )
                val accentColor by animateColorAsState(
                    targetValue = if (sliderValue > 0f) targetAccent else TextSecondary,
                    animationSpec = tween(durationMillis = 500),
                    label = "accentColor"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.2f),
                                    accentColor.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (sliderValue > 0f) primaryColor else CardBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (sliderValue > 0f) primaryColor else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (sliderValue > 0f) String.format("%.1f / 10", sliderValue) else "-",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = (kotlin.math.round(it * 2.0) / 2.0).toFloat()
                    },
                    valueRange = 0.0f..10.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                        thumbColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentScore != null && currentScore > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            onSaveScore(null)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonCoral)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.misc.clear,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveScore(if (sliderValue > 0f) sliderValue.toDouble() else null)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = strings.misc.save,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.libraryScreen.cancel,
                    color = TextSecondary
                )
            }
        }
    )
}
