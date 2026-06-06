package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Staff(
    val staffId: Long,
    val fullName: String,
    val imageLarge: String?
)

@Immutable
data class AnimeStaff(
    val staffId: Long,
    val fullName: String,
    val imageLarge: String?,
    val role: String
)
