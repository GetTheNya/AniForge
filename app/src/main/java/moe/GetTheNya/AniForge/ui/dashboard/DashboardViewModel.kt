package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import javax.inject.Inject

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao
) : ViewModel() {

    private val _searchFilter = MutableStateFlow(SearchFilterQuery())
    val searchFilter = _searchFilter.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = _searchFilter
        .debounce { query ->
            // Apply 250ms debounce only if typing a text search query to avoid stutter
            if (query.textQuery.isEmpty()) 0L else 250L
        }
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            animeRepository.queryAnimeFlow(filter)
        }
        .combine(userTrackingDao.observeAllTracking()) { animeList, trackingList ->
            val stats = calculateStats(trackingList)
            val featured = animeList.firstOrNull {
                val score = it.scoreMal
                score != null && score >= 8.5
            }
            DashboardUiState.Success(
                animeList = animeList,
                featuredAnime = featured,
                stats = stats
            ) as DashboardUiState
        }
        .catch { e ->
            emit(DashboardUiState.Error(e.message ?: "Unknown error"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    fun updateSearchQuery(query: String) {
        _searchFilter.value = _searchFilter.value.copy(textQuery = query)
    }

    fun toggleGenreFilter(genre: String) {
        val currentGenres = _searchFilter.value.genres.toMutableList()
        if (currentGenres.contains(genre)) {
            currentGenres.remove(genre)
        } else {
            currentGenres.add(genre)
        }
        _searchFilter.value = _searchFilter.value.copy(genres = currentGenres)
    }

    fun updateSortOrder(sortOption: SortOption) {
        _searchFilter.value = _searchFilter.value.copy(sortBy = sortOption)
    }

    private fun calculateStats(trackingList: List<moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity>): UserStats {
        val totalEpisodes = trackingList.sumOf { it.episodeProgress }
        val completedCount = trackingList.count { it.watchStatus == "COMPLETED" }
        val currentCount = trackingList.count { it.watchStatus == "CURRENT" }
        return UserStats(
            episodesWatched = totalEpisodes,
            titlesCompleted = completedCount,
            titlesWatching = currentCount
        )
    }
}

@Immutable
sealed interface DashboardUiState {
    @Immutable
    data object Loading : DashboardUiState
    @Immutable
    data class Success(
        val animeList: List<Anime>,
        val featuredAnime: Anime?,
        val stats: UserStats
    ) : DashboardUiState
    @Immutable
    data class Error(val message: String) : DashboardUiState
}

@Immutable
data class UserStats(
    val episodesWatched: Int,
    val titlesCompleted: Int,
    val titlesWatching: Int
)
