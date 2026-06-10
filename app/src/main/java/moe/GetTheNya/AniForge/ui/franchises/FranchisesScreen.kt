package moe.GetTheNya.AniForge.ui.franchises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.localization.getPlural

@Composable
fun FranchisesScreen(
    viewModel: FranchisesViewModel,
    navController: NavController,
    preferUk: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val franchisesList by viewModel.franchises.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
                text = strings.franchisesScreen.name,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                // Standard single cover fallback (should not occur per DB rules but handled gracefully)
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
