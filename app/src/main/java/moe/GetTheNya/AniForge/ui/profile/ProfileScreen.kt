package moe.GetTheNya.AniForge.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.ui.localization.getPlural

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val stats by viewModel.stats.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        // Sticky Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.profileScreen.userProfile,
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            IconButton(
                onClick = { navController.navigate(Screen.Settings) },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = strings.misc.settings,
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Side: Pie/Donut Chart
                AnimeStatsPieChart(
                    stats = stats,
                    modifier = Modifier.size(140.dp),
                    strokeWidth = 16.dp
                )

                // Right Side: Legend Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    statusConfigs.forEach { config ->
                        val count = stats[config.id] ?: 0
                        val isCountZero = count == 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colorAlpha = if (isCountZero) 0.35f else 1.0f
                            val textAlpha = if (isCountZero) 0.45f else 1.0f

                            // Micro color indicator badge
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(config.color.copy(alpha = colorAlpha))
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Localized label text
                            Text(
                                text = config.getLabel(strings),
                                fontSize = 13.sp,
                                fontWeight = if (isCountZero) FontWeight.Normal else FontWeight.Medium,
                                color = TextPrimary.copy(alpha = textAlpha)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Absolute count text using pluralization engine
                            val formattedCountText = strings.profileScreen.titlesCount.getPlural(count)
                            Text(
                                text = formattedCountText,
                                fontSize = 13.sp,
                                fontWeight = if (isCountZero) FontWeight.Normal else FontWeight.Bold,
                                color = TextSecondary.copy(alpha = textAlpha)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(96.dp)) // padding so content isn't covered by bottom navigation
    }
}
