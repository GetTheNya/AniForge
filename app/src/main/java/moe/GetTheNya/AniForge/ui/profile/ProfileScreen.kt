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
    onStudioClick: (Long) -> Unit,
    onGenreClick: (String) -> Unit,
    onCollectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = moe.GetTheNya.AniForge.ui.localization.LocalLocaleStrings.current
    val userStats by viewModel.userStats.collectAsState()
    val bentoStats by viewModel.bentoStats.collectAsState()
    val trackingStats by viewModel.stats.collectAsState()

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

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
    }
}
