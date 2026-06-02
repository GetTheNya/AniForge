package moe.GetTheNya.AniForge.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class SearchFilterQuery(
    val textQuery: String = "",
    val genres: List<String> = emptyList(),
    val studios: List<Long> = emptyList(),
    val tags: List<Long> = emptyList(),
    val sortBy: SortOption = SortOption.SCORE
)

enum class SortOption {
    SCORE,
    YEAR_DESC,
    YEAR_ASC,
    TITLE
}
