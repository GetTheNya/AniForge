package moe.GetTheNya.AniForge.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.AnimeFormat
import moe.GetTheNya.AniForge.core.model.ListSortOption
import moe.GetTheNya.AniForge.core.model.ListFilterState
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.Studio
import moe.GetTheNya.AniForge.core.model.Staff
import moe.GetTheNya.AniForge.ui.dashboard.QuickGestureAction
import moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository
import javax.inject.Inject

@HiltViewModel
class TrackedListViewModel @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider,
    private val userTrackingRepository: UserTrackingRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val activeTab = MutableStateFlow("")
    
    private val _listFilterState = MutableStateFlow(ListFilterState())
    val listFilterState = _listFilterState.asStateFlow()

    private val trackingSnapshot = MutableStateFlow<List<UserTrackingEntity>>(emptyList())

    val trackingSnapshotMap: StateFlow<Map<Long, String>> = trackingSnapshot
        .map { list -> list.associate { it.anilistId to it.watchStatus } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val trackingMap: StateFlow<Map<Long, String>> = userTrackingDao.observeAllTracking()
        .map { list -> list.associate { it.anilistId to it.watchStatus } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val trackingEntitiesMap: StateFlow<Map<Long, UserTrackingEntity>> = userTrackingDao.observeAllTracking()
        .map { list -> list.associateBy { it.anilistId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val gestureCenter: StateFlow<QuickGestureAction> = userTrackingRepository.gestureCenter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Immediate.OpenDetails)

    val gestureUp: StateFlow<QuickGestureAction> = userTrackingRepository.gestureUp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Continuous.EpisodeSlider)

    val gestureDown: StateFlow<QuickGestureAction> = userTrackingRepository.gestureDown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Continuous.ScoreSlider)

    val gestureLeft: StateFlow<QuickGestureAction> = userTrackingRepository.gestureLeft
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Immediate.OpenWatchStatusPicker)

    val gestureRight: StateFlow<QuickGestureAction> = userTrackingRepository.gestureRight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Immediate.ShareLink)

    val preferUk: StateFlow<Boolean> = settingsProvider.preferUkTitles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val allGenres: StateFlow<List<Genre>> = flow {
        emit(animeRepository.getAllGenres())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTags: StateFlow<List<Tag>> = flow {
        emit(animeRepository.getAllTags())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStudios: StateFlow<List<Studio>> = flow {
        emit(animeRepository.getAllStudios())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStaff: StateFlow<List<Staff>> = flow {
        emit(animeRepository.getAllStaff())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTrackedAnime: StateFlow<List<Anime>> = combine(
        userTrackingDao.observeAllTracking(),
        listFilterState,
        preferUk,
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { trackingList, filterState, ukPref, _ ->
        val ids = trackingList.map { it.anilistId }
        if (ids.isEmpty()) {
            emptyList()
        } else {
            val query = SearchFilterQuery(
                genres = filterState.genres,
                excludedGenres = filterState.excludedGenres,
                formats = filterState.formats,
                excludedFormats = filterState.excludedFormats,
                trackingStatuses = listOf("DUMMY"),
                trackingStatusIds = ids
            )
            val matchedAnimeList = animeRepository.queryAnime(query)
            val trackingMap = trackingList.associateBy { it.anilistId }
            
            matchedAnimeList.sortedWith { a, b ->
                val trackingA = trackingMap[a.anilistId]
                val trackingB = trackingMap[b.anilistId]
                
                when (filterState.sortBy) {
                    ListSortOption.SCORE_DESC -> {
                        val scoreA = trackingA?.score ?: -1.0
                        val scoreB = trackingB?.score ?: -1.0
                        scoreB.compareTo(scoreA)
                    }
                    ListSortOption.SCORE_ASC -> {
                        val scoreA = trackingA?.score ?: Double.MAX_VALUE
                        val scoreB = trackingB?.score ?: Double.MAX_VALUE
                        scoreA.compareTo(scoreB)
                    }
                    ListSortOption.PROGRESS_DESC -> {
                        val progA = trackingA?.episodeProgress ?: 0
                        val progB = trackingB?.episodeProgress ?: 0
                        progB.compareTo(progA)
                    }
                    ListSortOption.PROGRESS_ASC -> {
                        val progA = trackingA?.episodeProgress ?: 0
                        val progB = trackingB?.episodeProgress ?: 0
                        progA.compareTo(progB)
                    }
                    ListSortOption.DATE_ADDED_DESC -> {
                        val lmA = trackingA?.lastModified ?: 0L
                        val lmB = trackingB?.lastModified ?: 0L
                        lmB.compareTo(lmA)
                    }
                    ListSortOption.DATE_ADDED_ASC -> {
                        val lmA = trackingA?.lastModified ?: 0L
                        val lmB = trackingB?.lastModified ?: 0L
                        lmA.compareTo(lmB)
                    }
                    ListSortOption.ALPHABETICAL_ASC -> {
                        val titleA = a.getDisplayTitle(ukPref)
                        val titleB = b.getDisplayTitle(ukPref)
                        titleA.compareTo(titleB, ignoreCase = true)
                    }
                    ListSortOption.ALPHABETICAL_DESC -> {
                        val titleA = a.getDisplayTitle(ukPref)
                        val titleB = b.getDisplayTitle(ukPref)
                        titleB.compareTo(titleA, ignoreCase = true)
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredAnime: StateFlow<List<Anime>> = combine(
        allTrackedAnime,
        trackingSnapshotMap,
        activeTab,
        searchQuery
    ) { animeList, tracking, tab, query ->
        val filteredByTab = animeList.filter { tracking[it.anilistId] == tab }
        if (query.isBlank()) {
            filteredByTab
        } else {
            filteredByTab.filter { anime ->
                anime.titleRomaji.contains(query, ignoreCase = true) ||
                (anime.titleEn?.contains(query, ignoreCase = true) == true) ||
                (anime.titleUk?.contains(query, ignoreCase = true) == true)
            }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleGenreFilterState(genre: String) {
        val currentGenres = _listFilterState.value.genres.toMutableList()
        val currentExcluded = _listFilterState.value.excludedGenres.toMutableList()
        
        if (currentGenres.contains(genre)) {
            currentGenres.remove(genre)
            currentExcluded.add(genre)
        } else if (currentExcluded.contains(genre)) {
            currentExcluded.remove(genre)
        } else {
            currentGenres.add(genre)
        }
        
        _listFilterState.value = _listFilterState.value.copy(
            genres = currentGenres,
            excludedGenres = currentExcluded
        )
    }

    fun toggleFormatType(format: AnimeFormat) {
        val currentIncluded = _listFilterState.value.formats.toMutableList()
        val currentExcluded = _listFilterState.value.excludedFormats.toMutableList()
        
        if (currentIncluded.contains(format)) {
            currentIncluded.remove(format)
            currentExcluded.add(format)
        } else if (currentExcluded.contains(format)) {
            currentExcluded.remove(format)
        } else {
            currentIncluded.add(format)
        }
        
        _listFilterState.value = _listFilterState.value.copy(
            formats = currentIncluded,
            excludedFormats = currentExcluded
        )
    }

    fun updateSortOrder(sortOption: ListSortOption) {
        _listFilterState.value = _listFilterState.value.copy(sortBy = sortOption)
    }

    fun clearAllFilters() {
        _listFilterState.value = ListFilterState()
    }

    fun clearGenreFilters() {
        _listFilterState.value = _listFilterState.value.copy(
            genres = emptyList(),
            excludedGenres = emptyList()
        )
    }

    fun clearFormatFilters() {
        _listFilterState.value = _listFilterState.value.copy(
            formats = emptyList(),
            excludedFormats = emptyList()
        )
    }

    init {
        refreshSnapshot()
    }

    fun refreshSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = userTrackingDao.observeAllTracking().first()
                trackingSnapshot.value = currentList
            } catch (e: Exception) {
                // handle safely
            }
        }
    }

    fun setActiveTab(statusId: String) {
        activeTab.value = statusId
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun getRandomAnimeIdForCurrentTab(): Long? {
        return filteredAnime.value.randomOrNull()?.anilistId
    }

    fun updateWatchStatus(anilistId: Long, status: String) {
        viewModelScope.launch {
            userTrackingRepository.updateWatchStatus(anilistId, status)
        }
    }

    fun updateScore(anilistId: Long, score: Double) {
        viewModelScope.launch {
            userTrackingRepository.updateScore(anilistId, score)
        }
    }

    fun updateEpisodeProgress(anilistId: Long, progress: Int) {
        viewModelScope.launch {
            userTrackingRepository.updateEpisodeProgress(anilistId, progress)
        }
    }
}
