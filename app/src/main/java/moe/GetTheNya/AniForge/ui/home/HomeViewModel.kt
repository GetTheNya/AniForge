package moe.GetTheNya.AniForge.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.sync.DatabaseManager
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.dashboard.UserStats
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider,
    private val animeRepository: AnimeRepository,
    private val databaseManager: DatabaseManager
) : ViewModel() {

    val homeUiState: StateFlow<HomeUiState> = combine(
        userTrackingDao.observeAllTracking(),
        settingsProvider.preferUkTitles,
        animeRepository.getCatalogMetadataFlow(),
        animeRepository.queryAnimeFlow(moe.GetTheNya.AniForge.core.model.SearchFilterQuery()),
        databaseManager.isUpdating
    ) { trackingList, preferUk, metadata, animeList, isUpdating ->
        if (isUpdating) {
            HomeUiState.Updating
        } else {
            val stats = calculateStats(trackingList)
            val featured = animeList.firstOrNull {
                val score = it.scoreMal
                score != null && score >= 8.5
            }
            HomeUiState.Success(
                stats = stats,
                featuredAnime = featured,
                catalogVersion = metadata.version,
                activeSlot = metadata.activeSlot,
                preferUk = preferUk
            )
        }
    }
    .catch { e ->
        emit(HomeUiState.Error(e.message ?: "Unknown error"))
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState.Loading
    )

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
    data object Updating : HomeUiState
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
