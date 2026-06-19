package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
enum class EpisodeGroup {
    LESS_THAN_12,
    BETWEEN_12_AND_18,
    BETWEEN_19_AND_24,
    GREATER_THAN_24
}

@Immutable
data class SearchFilterQuery(
    val textQuery: String = "",
    val genres: List<String> = emptyList(),
    val excludedGenres: List<String> = emptyList(),
    val studios: List<Long> = emptyList(),
    val excludedStudios: List<Long> = emptyList(),
    val tags: List<Long> = emptyList(),
    val excludedTags: List<Long> = emptyList(),
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val episodeGroups: List<EpisodeGroup> = emptyList(),
    val excludedEpisodeGroups: List<EpisodeGroup> = emptyList(),
    val formats: List<AnimeFormat> = emptyList(),
    val excludedFormats: List<AnimeFormat> = emptyList(),
    val hasUkTranslation: Boolean? = null,
    val trackingStatuses: List<String> = emptyList(),
    val excludedTrackingStatuses: List<String> = emptyList(),
    val trackingStatusIds: List<Long> = emptyList(),
    val excludedTrackingStatusIds: List<Long> = emptyList(),
    val staff: List<Long> = emptyList(),
    val excludedStaff: List<Long> = emptyList(),
    val mediaStatuses: List<String> = emptyList(),
    val excludedMediaStatuses: List<String> = emptyList(),
    val mediaSources: List<String> = emptyList(),
    val excludedMediaSources: List<String> = emptyList(),
    val sortBy: SortOption = SortOption.SCORE
)

enum class SortOption {
    RELEVANCE,
    SCORE,
    SCORE_ASC,
    YEAR_DESC,
    YEAR_ASC,
    TITLE,
    TITLE_DESC,
    POPULARITY,
    POPULARITY_ASC,
    START_DATE_DESC,
    START_DATE_ASC,
    EPISODES_DESC,
    EPISODES_ASC
}

@Immutable
enum class ListSortOption {
    SCORE_DESC,
    SCORE_ASC,
    PROGRESS_DESC,
    PROGRESS_ASC,
    DATE_ADDED_DESC,
    DATE_ADDED_ASC,
    ALPHABETICAL_ASC,
    ALPHABETICAL_DESC
}

@Immutable
data class ListFilterState(
    val genres: List<String> = emptyList(),
    val excludedGenres: List<String> = emptyList(),
    val formats: List<AnimeFormat> = emptyList(),
    val excludedFormats: List<AnimeFormat> = emptyList(),
    val sortBy: ListSortOption = ListSortOption.DATE_ADDED_DESC
)

