package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Relation(
    val edgeId: Long,
    val sourceAnilistId: Long,
    val targetAnilistId: Long,
    val relationType: String
)
