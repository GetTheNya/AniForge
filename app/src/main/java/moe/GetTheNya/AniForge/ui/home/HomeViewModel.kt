package moe.GetTheNya.AniForge.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.dashboard.UserStats
import moe.GetTheNya.AniForge.ui.localization.LocalizationService
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider,
    private val animeRepository: AnimeRepository,
    private val localizationService: LocalizationService
) : ViewModel() {

    private val fallbackPhrases = listOf(
        "What are we watching today? 🍿",
        "Your next favorite story is waiting.",
        "Otaku mode: ACTIVATED. 🤖",
        "Time to clear some backlog! 😅",
        "Ready for a new dose of emotions?",
        "Your anime list won't watch itself!",
        "Let's find something historical to track."
    )

    private var cachedPhrase: Pair<String, String>? = null

    val randomWelcomeSubtitle: StateFlow<String> = localizationService.activeLocaleStrings
        .map { locale ->
            val langCode = locale.languageCode
            val currentCached = cachedPhrase
            if (currentCached != null && currentCached.first == langCode) {
                currentCached.second
            } else {
                val phrases = locale.homeScreen.welcomePhrases
                val phrase = if (phrases.isNotEmpty()) {
                    phrases.random()
                } else {
                    fallbackPhrases.random()
                }
                cachedPhrase = langCode to phrase
                phrase
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Your next favorite story is waiting."
        )

    val homeUiState: StateFlow<HomeUiState> = combine(
        userTrackingDao.observeAllTracking(),
        settingsProvider.preferUkTitles,
        animeRepository.queryAnimeFlow(moe.GetTheNya.AniForge.core.model.SearchFilterQuery())
    ) { trackingList, preferUk, animeList ->
        val stats = calculateStats(trackingList)
        val featured = animeList.firstOrNull {
            val score = it.scoreMal
            score != null && score >= 8.5
        }
        HomeUiState.Success(
            stats = stats,
            featuredAnime = featured,
            preferUk = preferUk
        ) as HomeUiState
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
    data class Success(
        val stats: UserStats,
        val featuredAnime: Anime?,
        val preferUk: Boolean
    ) : HomeUiState
    @Immutable
    data class Error(val message: String) : HomeUiState
}
