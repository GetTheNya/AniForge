package moe.GetTheNya.AniForge.core.model

data class TransitionState(
    val season: String,
    val seasonYear: Int,
    val isFallbackToPrevious: Boolean
)
