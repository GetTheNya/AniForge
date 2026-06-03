package moe.GetTheNya.AniForge.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.dashboard.UserStats
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider,
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _homeUiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _featuredAnime = MutableStateFlow<Anime?>(null)

    init {
        loadFeaturedAnime()
        observeStatsAndFeatured()
    }

    private fun loadFeaturedAnime() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val searchFilter = moe.GetTheNya.AniForge.core.model.SearchFilterQuery()
                val animeList = animeRepository.queryAnime(searchFilter)
                val featured = animeList.firstOrNull { 
                    val score = it.scoreMal
                    score != null && score >= 8.5 
                }
                _featuredAnime.value = featured
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeStatsAndFeatured() {
        viewModelScope.launch {
            userTrackingDao.observeAllTracking()
                .combine(settingsProvider.preferUkTitles) { trackingList, preferUk ->
                    trackingList to preferUk
                }
                .combine(_featuredAnime) { (trackingList, preferUk), featured ->
                    val stats = calculateStats(trackingList)
                    val catalogVersion = settingsProvider.getCatalogVersion()
                    val activeSlot = settingsProvider.getActiveCatalogFileName()
                    
                    HomeUiState.Success(
                        stats = stats,
                        featuredAnime = featured,
                        catalogVersion = catalogVersion,
                        activeSlot = activeSlot,
                        preferUk = preferUk
                    )
                }
                .catch { e ->
                    _homeUiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                }
                .collect { state ->
                    _homeUiState.value = state
                }
        }
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
sealed interface HomeUiState {
    @Immutable
    data object Loading : HomeUiState
    @Immutable
    data class Success(
        val stats: UserStats,
        val featuredAnime: Anime?,
        val catalogVersion: Long,
        val activeSlot: String,
        val preferUk: Boolean
    ) : HomeUiState
    @Immutable
    data class Error(val message: String) : HomeUiState
}
