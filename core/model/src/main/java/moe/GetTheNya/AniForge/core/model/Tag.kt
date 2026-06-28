package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Tag(
    val tagId: Long,
    val nameEn: String,
    val nameUk: String?,
    val category: String?
) {
    fun getDisplayName(preferUk: Boolean = true): String {
        return if (preferUk) nameUk ?: nameEn else nameEn
    }

    fun isNsfw(): Boolean {
        return category?.equals("Sexual Content", ignoreCase = true) == true
    }
}
