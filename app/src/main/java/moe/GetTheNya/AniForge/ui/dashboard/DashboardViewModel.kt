package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.runtime.Immutable
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
import moe.GetTheNya.AniForge.core.model.AnimeFormat
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.Studio
import moe.GetTheNya.AniForge.core.model.EpisodeGroup
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import javax.inject.Inject

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    private val _searchFilter = MutableStateFlow(SearchFilterQuery())
    val searchFilter = _searchFilter.asStateFlow()

    val preferUkTitles: StateFlow<Boolean> = settingsProvider.preferUkTitles

    val allGenres: StateFlow<List<Genre>> = flow {
        emit(animeRepository.getAllGenres())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTags: StateFlow<List<Tag>> = flow {
        emit(animeRepository.getAllTags())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStudios: StateFlow<List<Studio>> = flow {
        emit(animeRepository.getAllStudios())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<DashboardUiState> = combine(
        _searchFilter.debounce { query ->
            if (query.textQuery.isEmpty()) 0L else 250L
        },
        userTrackingDao.observeAllTracking(),
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { filter, trackingList, _ ->
        val trackingStatusIds = trackingList
            .filter { it.watchStatus in filter.trackingStatuses }
            .map { it.anilistId }
        val excludedTrackingStatusIds = trackingList
            .filter { it.watchStatus in filter.excludedTrackingStatuses }
            .map { it.anilistId }

        val finalFilter = filter.copy(
            trackingStatusIds = trackingStatusIds,
            excludedTrackingStatusIds = excludedTrackingStatusIds
        )
        finalFilter to trackingList
    }
    .distinctUntilChanged { old, new ->
        old.first == new.first && 
        old.second.size == new.second.size && 
        old.second.all { oldItem ->
            new.second.any { newItem -> newItem.anilistId == oldItem.anilistId && newItem.watchStatus == oldItem.watchStatus }
        }
    }
    .flatMapLatest { (finalFilter, trackingList) ->
        flow {
            val list = animeRepository.queryAnime(finalFilter)
            emit(list to trackingList)
        }
    }
    .map { (animeList, trackingList) ->
        val stats = calculateStats(trackingList)
        val featured = animeList.firstOrNull {
            val score = it.scoreMal
            score != null && score >= 8.5
        }
        val trackingMap = trackingList.associate { it.anilistId to it.watchStatus }
        DashboardUiState.Success(
            animeList = animeList,
            featuredAnime = featured,
            stats = stats,
            trackingMap = trackingMap
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

    val filteredCount: StateFlow<Int> = uiState
        .map { state ->
            when (state) {
                is DashboardUiState.Success -> state.animeList.size
                else -> 0
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun updateSearchQuery(query: String) {
        val wasBlank = _searchFilter.value.textQuery.isBlank()
        val isNowNotBlank = query.isNotBlank()
        var newSortBy = _searchFilter.value.sortBy
        if (wasBlank && isNowNotBlank) {
            newSortBy = SortOption.RELEVANCE
        } else if (!isNowNotBlank && newSortBy == SortOption.RELEVANCE) {
            newSortBy = SortOption.SCORE
        }
        _searchFilter.value = _searchFilter.value.copy(textQuery = query, sortBy = newSortBy)
    }

    fun toggleGenreFilterState(genre: String) {
        val currentGenres = _searchFilter.value.genres.toMutableList()
        val currentExcluded = _searchFilter.value.excludedGenres.toMutableList()
        
        if (currentGenres.contains(genre)) {
            currentGenres.remove(genre)
            currentExcluded.add(genre)
        } else if (currentExcluded.contains(genre)) {
            currentExcluded.remove(genre)
        } else {
            currentGenres.add(genre)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            genres = currentGenres,
            excludedGenres = currentExcluded
        )
    }

    fun toggleTagFilterState(tagId: Long) {
        val currentTags = _searchFilter.value.tags.toMutableList()
        val currentExcluded = _searchFilter.value.excludedTags.toMutableList()
        
        if (currentTags.contains(tagId)) {
            currentTags.remove(tagId)
            currentExcluded.add(tagId)
        } else if (currentExcluded.contains(tagId)) {
            currentExcluded.remove(tagId)
        } else {
            currentTags.add(tagId)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            tags = currentTags,
            excludedTags = currentExcluded
        )
    }

    fun updateScoreRange(min: Double?, max: Double?) {
        _searchFilter.value = _searchFilter.value.copy(minScore = min, maxScore = max)
    }

    fun toggleEpisodeGroup(group: EpisodeGroup) {
        val currentIncluded = _searchFilter.value.episodeGroups.toMutableList()
        val currentExcluded = _searchFilter.value.excludedEpisodeGroups.toMutableList()
        
        if (currentIncluded.contains(group)) {
            currentIncluded.remove(group)
            currentExcluded.add(group)
        } else if (currentExcluded.contains(group)) {
            currentExcluded.remove(group)
        } else {
            currentIncluded.add(group)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            episodeGroups = currentIncluded,
            excludedEpisodeGroups = currentExcluded
        )
    }

    fun toggleFormatType(format: AnimeFormat) {
        val currentIncluded = _searchFilter.value.formats.toMutableList()
        val currentExcluded = _searchFilter.value.excludedFormats.toMutableList()
        
        if (currentIncluded.contains(format)) {
            currentIncluded.remove(format)
            currentExcluded.add(format)
        } else if (currentExcluded.contains(format)) {
            currentExcluded.remove(format)
        } else {
            currentIncluded.add(format)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            formats = currentIncluded,
            excludedFormats = currentExcluded
        )
    }

    fun toggleStudioFilter(studioId: Long) {
        val currentIncluded = _searchFilter.value.studios.toMutableList()
        val currentExcluded = _searchFilter.value.excludedStudios.toMutableList()
        
        if (currentIncluded.contains(studioId)) {
            currentIncluded.remove(studioId)
            currentExcluded.add(studioId)
        } else if (currentExcluded.contains(studioId)) {
            currentExcluded.remove(studioId)
        } else {
            currentIncluded.add(studioId)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            studios = currentIncluded,
            excludedStudios = currentExcluded
        )
    }

    fun toggleUkTranslationFilter() {
        val current = _searchFilter.value.hasUkTranslation
        val newVal = if (current == true) null else true
        _searchFilter.value = _searchFilter.value.copy(hasUkTranslation = newVal)
    }

    fun toggleTrackingStatus(status: String) {
        val currentIncluded = _searchFilter.value.trackingStatuses.toMutableList()
        val currentExcluded = _searchFilter.value.excludedTrackingStatuses.toMutableList()
        
        if (currentIncluded.contains(status)) {
            currentIncluded.remove(status)
            currentExcluded.add(status)
        } else if (currentExcluded.contains(status)) {
            currentExcluded.remove(status)
        } else {
            currentIncluded.add(status)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            trackingStatuses = currentIncluded,
            excludedTrackingStatuses = currentExcluded
        )
    }


    fun clearAllFilters() {
        _searchFilter.value = SearchFilterQuery(textQuery = _searchFilter.value.textQuery)
    }

    fun clearGenreFilters() {
        _searchFilter.value = _searchFilter.value.copy(
            genres = emptyList(),
            excludedGenres = emptyList()
        )
    }

    fun clearTagFilters() {
        _searchFilter.value = _searchFilter.value.copy(
            tags = emptyList(),
            excludedTags = emptyList()
        )
    }

    fun clearStudioFilters() {
        _searchFilter.value = _searchFilter.value.copy(
            studios = emptyList(),
            excludedStudios = emptyList()
        )
    }

    fun updateSortOrder(sortOption: SortOption) {
        _searchFilter.value = _searchFilter.value.copy(sortBy = sortOption)
    }

    fun updateWatchStatus(anilistId: Long, status: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
            if (currentTracking != null && currentTracking.watchStatus == status) {
                userTrackingDao.delete(currentTracking)
            } else {
                val anime = animeRepository.getAnimeById(anilistId)
                val maxEpisodes = anime?.episodes ?: 0
                val progress = if (status == "COMPLETED") {
                    maxEpisodes
                } else {
                    currentTracking?.episodeProgress ?: 0
                }

                val updated = currentTracking?.copy(
                    watchStatus = status,
                    episodeProgress = progress,
                    lastModified = System.currentTimeMillis()
                ) ?: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity(
                    anilistId = anilistId,
                    watchStatus = status,
                    episodeProgress = progress,
                    score = null,
                    notes = null,
                    lastModified = System.currentTimeMillis()
                )
                userTrackingDao.insertOrUpdate(updated)
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
sealed interface DashboardUiState {
    @Immutable
    data object Loading : DashboardUiState
    @Immutable
    data class Success(
        val animeList: List<Anime>,
        val featuredAnime: Anime?,
        val stats: UserStats,
        val trackingMap: Map<Long, String> = emptyMap()
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
