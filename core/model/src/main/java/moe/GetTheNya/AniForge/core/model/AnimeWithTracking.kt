package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class AnimeWithTracking(
    val anime: Anime,
    val watchStatus: String?,
    val episodeProgress: Int,
    val score: Double?
)
