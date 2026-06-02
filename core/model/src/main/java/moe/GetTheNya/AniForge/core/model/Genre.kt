package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Genre(
    val slug: String,
    val nameEn: String,
    val nameUk: String?
) {
    fun getDisplayName(preferUk: Boolean = true): String {
        return if (preferUk) nameUk ?: nameEn else nameEn
    }
}
