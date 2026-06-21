package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Anime(
    val anilistId: Long,
    val malId: Long?,
    val titleUk: String?,
    val titleRomaji: String,
    val titleEn: String?,
    val descriptionUk: String?,
    val descriptionEn: String?,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val duration: Int?,
    val seasonYear: Int?,
    val season: String?,
    val isAdult: Boolean,
    val scoreMal: Double?,
    val coverExtraLarge: String?,
    val coverLarge: String?,
    val coverColor: String?,
    val bannerImage: String?,
    val hasUkTranslation: Boolean,
    val updatedAt: Long,
    val airingAt: Long?,
    val airingEpisode: Int?,
    val trailerId: String?,
    val trailerSite: String?,
    val trailerThumbnail: String?,
    val startDateYear: Int?,
    val startDateMonth: Int?,
    val startDateDay: Int?,
    val popularity: Int?,
    val source: String?,
    val synonymsFlat: String? = null
) {
    // Helper to get the display title based on preference or fallback
    fun getDisplayTitle(preferUk: Boolean = true): String {
        return if (preferUk) {
            titleUk ?: titleRomaji
        } else {
            titleEn ?: titleRomaji
        }
    }

    fun getReleasedEpisodes(): Int? {
        if (status?.uppercase() == "RELEASING") {
            if (airingEpisode != null) {
                return airingEpisode - 1
            }
            return episodes
        }
        return episodes
    }

    fun getEpisodeBadgeText(progress: Int, suffix: String): String {
        val totalPlanned = episodes
        val isReleasing = status?.uppercase() == "RELEASING"
        val airingEpisode = airingEpisode

        return when {
            isReleasing && airingEpisode != null -> {
                "$progress / ${airingEpisode - 1} $suffix"
            }
            totalPlanned != null -> {
                "$progress / $totalPlanned $suffix"
            }
            isReleasing -> {
                "$progress / ? $suffix"
            }
            else -> {
                "?"
            }
        }
    }

    fun getMaxAllowedIncrement(): Int {
        if (status?.uppercase() == "RELEASING") {
            if (airingEpisode != null) {
                return airingEpisode - 1
            }
            return episodes ?: Int.MAX_VALUE
        }
        return episodes ?: Int.MAX_VALUE
    }
}
