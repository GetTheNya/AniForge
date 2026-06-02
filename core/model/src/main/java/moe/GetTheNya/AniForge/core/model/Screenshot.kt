package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Screenshot(
    val id: Long,
    val anilistId: Long,
    val imageUrl: String
)
