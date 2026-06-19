package moe.GetTheNya.AniForge.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.entity.WidgetConfigEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.repository.BentoWidgetRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.ui.dashboard.UserStats
import moe.GetTheNya.AniForge.ui.localization.LocalizationService
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider,
    private val animeRepository: AnimeRepository,
    private val localizationService: LocalizationService,
    private val bentoWidgetRepository: BentoWidgetRepository
) : ViewModel() {

    private var cachedPhrase: Pair<String, String?>? = null

    val titleAnimStyle: StateFlow<TitleAnimStyle> = settingsProvider.titleAnimStyleStr
        .map { str ->
            try { TitleAnimStyle.valueOf(str) } catch (e: Exception) { TitleAnimStyle.DECODING }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TitleAnimStyle.DECODING
        )

    val subtitleAnimStyle: StateFlow<SubtitleAnimStyle> = settingsProvider.subtitleAnimStyleStr
        .map { str ->
            try { SubtitleAnimStyle.valueOf(str) } catch (e: Exception) { SubtitleAnimStyle.BLUR_FADE }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubtitleAnimStyle.BLUR_FADE
        )

    val contentAnimStyle: StateFlow<ContentAnimStyle> = settingsProvider.contentAnimStyleStr
        .map { str ->
            try { ContentAnimStyle.valueOf(str) } catch (e: Exception) { ContentAnimStyle.POWER_UP }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ContentAnimStyle.POWER_UP
        )

    var isHomeAnimationPlayed: Boolean = false
    var expandedHeaderHeightPx: Float = 0f
    var metadataHeightPx: Float = 0f

    val randomWelcomeSubtitle: StateFlow<String?> = localizationService.activeLocaleStrings
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
                    null
                }
                cachedPhrase = langCode to phrase
                phrase
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val continueWatchingFlow: Flow<List<Anime>> = combine(
        userTrackingDao.observeContinueWatching(),
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { trackingList, _ ->
        val ids = trackingList.map { it.anilistId }
        val animeList = animeRepository.getAnimeByIds(ids)
        val animeMap = animeList.associateBy { it.anilistId }
        ids.mapNotNull { animeMap[it] }
    }.flowOn(Dispatchers.IO)

    private val nextUpFlow: Flow<List<Anime>> = combine(
        userTrackingDao.observeNextUp(),
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { trackingList, _ ->
        val ids = trackingList.map { it.anilistId }
        val animeList = animeRepository.getAnimeByIds(ids)
        val animeMap = animeList.associateBy { it.anilistId }
        ids.mapNotNull { animeMap[it] }
    }.flowOn(Dispatchers.IO)

    val homeUiState: StateFlow<HomeUiState> = combine(
        userTrackingDao.observeAllTracking(),
        settingsProvider.preferUkTitles,
        animeRepository.queryAnimeFlow(moe.GetTheNya.AniForge.core.model.SearchFilterQuery()),
        bentoWidgetRepository.observeWidgetConfigs,
        bentoWidgetRepository.observeUserStats,
        bentoWidgetRepository.bentoStatsFlow,
        continueWatchingFlow,
        nextUpFlow
    ) { array ->
        val trackingList = array[0] as List<UserTrackingEntity>
        val preferUk = array[1] as Boolean
        val animeList = array[2] as List<Anime>
        val widgetConfigs = array[3] as List<WidgetConfigEntity>
        val userStats = array[4] as moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity
        val bentoStats = array[5] as moe.GetTheNya.AniForge.core.model.BentoStatsData
        val continueWatching = array[6] as List<Anime>
        val nextUp = array[7] as List<Anime>

        val stats = calculateStats(trackingList)
        val featured = animeList.firstOrNull {
            val score = it.scoreMal
            score != null && score >= 8.5
        }
        val trackingStats = trackingList.groupBy { it.watchStatus }.mapValues { it.value.size }
        HomeUiState.Success(
            stats = stats,
            featuredAnime = featured,
            preferUk = preferUk,
            widgetConfigs = widgetConfigs,
            userStats = userStats,
            bentoStats = bentoStats,
            trackingStats = trackingStats,
            continueWatchingList = continueWatching,
            nextUpList = nextUp
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

    val isInitializing: StateFlow<Boolean> = homeUiState
        .map { it is HomeUiState.Loading }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val isEditMode = MutableStateFlow(false)
    val editableWidgetConfigs = mutableStateListOf<WidgetConfigEntity>()

    fun enterEditMode() {
        val successState = homeUiState.value as? HomeUiState.Success ?: return
        editableWidgetConfigs.clear()
        editableWidgetConfigs.addAll(successState.widgetConfigs)
        isEditMode.value = true
    }

    fun exitEditMode() {
        isEditMode.value = false
        saveWidgetOrder()
    }

    fun moveWidget(fromId: String, toId: String) {
        val fromIndex = editableWidgetConfigs.indexOfFirst { it.widgetId == fromId }
        val toIndex = editableWidgetConfigs.indexOfFirst { it.widgetId == toId }
        if (fromIndex != -1 && toIndex != -1) {
            val item = editableWidgetConfigs.removeAt(fromIndex)
            editableWidgetConfigs.add(toIndex, item)
            val updated = editableWidgetConfigs.mapIndexed { idx, entity ->
                entity.copy(orderIndex = idx)
            }
            editableWidgetConfigs.clear()
            editableWidgetConfigs.addAll(updated)
        }
    }

    fun deleteWidget(widgetId: String) {
        val index = editableWidgetConfigs.indexOfFirst { it.widgetId == widgetId }
        if (index != -1) {
            editableWidgetConfigs[index] = editableWidgetConfigs[index].copy(isVisible = false)
            val updated = editableWidgetConfigs.mapIndexed { idx, entity ->
                entity.copy(orderIndex = idx)
            }
            editableWidgetConfigs.clear()
            editableWidgetConfigs.addAll(updated)
            saveWidgetOrder()
        }
    }

    fun restoreWidget(widgetId: String) {
        val index = editableWidgetConfigs.indexOfFirst { it.widgetId == widgetId }
        if (index != -1) {
            val item = editableWidgetConfigs.removeAt(index)
            val visibleCount = editableWidgetConfigs.count { it.isVisible }
            editableWidgetConfigs.add(visibleCount, item.copy(isVisible = true))
            val updated = editableWidgetConfigs.mapIndexed { idx, entity ->
                entity.copy(orderIndex = idx)
            }
            editableWidgetConfigs.clear()
            editableWidgetConfigs.addAll(updated)
            saveWidgetOrder()
        }
    }

    fun saveWidgetOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            val configsToSave = editableWidgetConfigs.toList()
            bentoWidgetRepository.updateWidgetConfigs(configsToSave)
        }
    }

    private fun calculateStats(trackingList: List<UserTrackingEntity>): UserStats {
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
        val preferUk: Boolean,
        val widgetConfigs: List<WidgetConfigEntity> = emptyList(),
        val userStats: moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity = moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity(),
        val bentoStats: moe.GetTheNya.AniForge.core.model.BentoStatsData = moe.GetTheNya.AniForge.core.model.BentoStatsData(),
        val trackingStats: Map<String, Int> = emptyMap(),
        val continueWatchingList: List<Anime> = emptyList(),
        val nextUpList: List<Anime> = emptyList()
    ) : HomeUiState
    @Immutable
    data class Error(val message: String) : HomeUiState
}
