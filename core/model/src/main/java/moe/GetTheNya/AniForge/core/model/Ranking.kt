package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Ranking(
    val id: Long,
    val anilistId: Long,
    val rank: Int,
    val context: String,
    val allTime: Boolean,
    val season: String?,
    val year: Int?
)
