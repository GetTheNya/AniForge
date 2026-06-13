package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class FranchiseGiantInfo(
    val franchise: Franchise,
    val totalEpisodes: Int
)

@Immutable
data class GenreDistribution(
    val genre: Genre,
    val count: Int
)

@Immutable
data class StudioDistribution(
    val studio: Studio,
    val count: Int
)

@Immutable
data class BentoStatsData(
    val genreDistributions: List<GenreDistribution> = emptyList(),
    val studioDistributions: List<StudioDistribution> = emptyList(),
    val franchiseGiant: FranchiseGiantInfo? = null,
    val activeCollectionsCount: Int = 0,
    val collectionCovers: List<String> = emptyList()
)
