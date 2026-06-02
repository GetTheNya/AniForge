package moe.GetTheNya.AniForge.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao
) : ViewModel() {

    private val _searchFilter = MutableStateFlow(SearchFilterQuery())
    val searchFilter = _searchFilter.asStateFlow()

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        observeCatalogAndStats()
    }

    private fun observeCatalogAndStats() {
        viewModelScope.launch {
            // Combine FTS queries (debounced) with stats observations
            _searchFilter
                .debounce { query ->
                    // Apply 250ms debounce only if typing a text search query to avoid stutter
                    if (query.textQuery.isEmpty()) 0L else 250L
                }
                .distinctUntilChanged()
                .flatMapLatest { filter ->
                    flow {
                        emit(animeRepository.queryAnime(filter))
                    }
                }
                .combine(userTrackingDao.observeAllTracking()) { animeList, trackingList ->
                    val stats = calculateStats(trackingList)
                    val featured = animeList.firstOrNull { it.scoreMal != null && it.scoreMal!! >= 8.5 }
                    DashboardUiState.Success(
                        animeList = animeList,
                        featuredAnime = featured,
                        stats = stats
                    )
                }
                .catch { e ->
                    _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

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

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val animeList: List<Anime>,
        val featuredAnime: Anime?,
        val stats: UserStats
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

data class UserStats(
    val episodesWatched: Int,
    val titlesCompleted: Int,
    val titlesWatching: Int
)
