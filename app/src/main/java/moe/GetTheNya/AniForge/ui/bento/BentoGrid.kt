package moe.GetTheNya.AniForge.ui.bento

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity
import moe.GetTheNya.AniForge.core.model.BentoStatsData
import moe.GetTheNya.AniForge.core.network.UserProfileDto

@Composable
fun BentoDashboardGrid(
    stats: UserStatsEntity,
    bentoData: BentoStatsData,
    trackingStats: Map<String, Int>,
    onStudioClick: (Long) -> Unit,
    onGenreClick: (String) -> Unit,
    onCollectionClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    friends: List<UserProfileDto>,
    onSocialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WatchTimeWidget(
            totalMinutes = stats.totalWatchTimeMinutes
        )

        BentoWatchStatusPieChart(
            stats = trackingStats,
            onStatusClick = onStatusClick
        )

        if (friends.isNotEmpty()) {
            FriendsWidget(
                friends = friends,
                onClick = onSocialClick
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChaosMeterWidget(
                count = stats.chaosMeterCount,
                modifier = Modifier.weight(1f)
            )
            CollectionsBridgeWidget(
                activeCollections = bentoData.activeCollectionsCount,
                covers = bentoData.collectionCovers,
                onClick = onCollectionClick,
                modifier = Modifier.weight(1f)
            )
        }

        TopStudiosWidget(
            studios = bentoData.studioDistributions,
            onStudioClick = onStudioClick
        )

        TopGenresWidget(
            genres = bentoData.genreDistributions,
            onGenreClick = onGenreClick
        )

        FranchiseGiantWidget(
            info = bentoData.franchiseGiant
        )
    }
}
