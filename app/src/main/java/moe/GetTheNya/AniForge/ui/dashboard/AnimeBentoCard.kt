package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.theme.CardBorder
import moe.GetTheNya.AniForge.ui.theme.NeonCoral
import moe.GetTheNya.AniForge.ui.theme.SurfaceCardDark
import moe.GetTheNya.AniForge.ui.theme.TextPrimary
import moe.GetTheNya.AniForge.ui.theme.TextSecondary

@Composable
fun AnimeBentoCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preferUk: Boolean = true
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCardDark)
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        // Background cover artwork (Coil exact resizing & GPU hardware optimization)
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(anime.coverLarge ?: anime.coverMedium)
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
                if (anime.hasUkTranslation) {
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
}
