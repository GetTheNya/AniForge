package moe.GetTheNya.AniForge.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.ui.theme.*
import moe.GetTheNya.AniForge.ui.utils.statusConfigs
import moe.GetTheNya.AniForge.ui.localization.getPlural

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navController: NavController,
    onStudioClick: (Long) -> Unit,
    onGenreClick: (String) -> Unit,
    onCollectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val userStats by viewModel.userStats.collectAsState()
    val bentoStats by viewModel.bentoStats.collectAsState()
    val trackingStats by viewModel.stats.collectAsState()

    val scrollState = rememberScrollState()
    val scrollOffset = scrollState.value

    val density = LocalDensity.current
    val expandedHeight = 80.dp
    val collapsedHeight = 56.dp
    val maxScrollPx = remember { with(density) { (expandedHeight - collapsedHeight).toPx() } }
    val collapseFraction = (scrollOffset.toFloat() / maxScrollPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(96.dp))

            moe.GetTheNya.AniForge.ui.bento.BentoDashboardGrid(
                stats = userStats,
                bentoData = bentoStats,
                trackingStats = trackingStats,
                onStudioClick = onStudioClick,
                onGenreClick = onGenreClick,
                onCollectionClick = onCollectionClick,
                onStatusClick = { statusId ->
                    navController.navigate(Screen.TrackedList(statusId))
                }
            )

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Floating Header Overlay
        val headerHeight = lerp(expandedHeight, collapsedHeight, collapseFraction)
        val verticalPadding = lerp(16.dp, 0.dp, collapseFraction)
        val titleSize = (28 - (28 - 20) * collapseFraction).sp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(BackgroundDark)
                .padding(horizontal = 24.dp)
                .padding(vertical = verticalPadding)
                .zIndex(5f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.profileScreen.userProfile,
                color = TextPrimary,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            IconButton(
                onClick = { navController.navigate(Screen.Settings()) },
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
    }
}
