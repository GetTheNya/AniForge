package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

enum class CandidateStatus {
    RELEASED,
    ANNOUNCED,
    QUIET
}

@Immutable
data class WaitingItem(
    val baseAnime: Anime,
    val candidateSequel: Anime?,
    val candidateStatus: CandidateStatus
)
