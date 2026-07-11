package moe.GetTheNya.AniForge.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import moe.GetTheNya.AniForge.core.model.AnimeFormat
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.Studio
import moe.GetTheNya.AniForge.core.model.EpisodeGroup
import moe.GetTheNya.AniForge.core.model.Staff
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import moe.GetTheNya.AniForge.core.model.AnimeWithTracking
import moe.GetTheNya.AniForge.core.database.repository.AnimeCatalogPagingSource
import moe.GetTheNya.AniForge.ui.franchises.FranchiseItem
import moe.GetTheNya.AniForge.ui.franchises.FranchiseCatalogPagingSource
import javax.inject.Inject

enum class DashboardSubTab {
    SEARCH,
    FRANCHISES
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider,
    private val userTrackingRepository: UserTrackingRepository
) : ViewModel() {

    val activeSubTab = MutableStateFlow<DashboardSubTab>(DashboardSubTab.SEARCH)

    val franchiseSearchQuery = MutableStateFlow("")

    val pagingFranchisesFlow: Flow<PagingData<FranchiseItem>> = franchiseSearchQuery
        .debounce { query ->
            if (query.isEmpty()) 0L else 250L
        }
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                    prefetchDistance = 5
                ),
                pagingSourceFactory = {
                    FranchiseCatalogPagingSource(
                        animeRepository = animeRepository,
                        query = query,
                        pageSize = 20
                    )
                }
            ).flow
        }
        .cachedIn(viewModelScope)

    fun updateFranchiseSearchQuery(query: String) {
        franchiseSearchQuery.value = query
    }

    private val _searchFilter = MutableStateFlow(SearchFilterQuery())
    val searchFilter = _searchFilter.asStateFlow()

    val preferUkTitles: StateFlow<Boolean> = settingsProvider.preferUkTitles

    val gestureCenter: StateFlow<QuickGestureAction> = userTrackingRepository.gestureCenter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Immediate.OpenWatchStatusPicker)

    val gestureUp: StateFlow<QuickGestureAction> = userTrackingRepository.gestureUp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Continuous.EpisodeSlider)

    val gestureDown: StateFlow<QuickGestureAction> = userTrackingRepository.gestureDown
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Continuous.ScoreSlider)

    val gestureLeft: StateFlow<QuickGestureAction> = userTrackingRepository.gestureLeft
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Immediate.OpenDetails)

    val gestureRight: StateFlow<QuickGestureAction> = userTrackingRepository.gestureRight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuickGestureAction.Immediate.ShareLink)

    val allGenres: StateFlow<List<Genre>> = settingsProvider.show18Plus
        .flatMapLatest { show18 ->
            flow {
                val genres = animeRepository.getAllGenres()
                if (show18) {
                    emit(genres)
                } else {
                    emit(genres.filter {
                        !it.nameEn.equals("Hentai", ignoreCase = true) &&
                        !it.slug.equals("hentai", ignoreCase = true)
                    })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTags: StateFlow<List<Tag>> = settingsProvider.show18Plus
        .flatMapLatest { show18 ->
            flow {
                val tags = animeRepository.getAllTags()
                if (show18) {
                    emit(tags)
                } else {
                    emit(tags.filter { !it.isNsfw() })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStudios: StateFlow<List<Studio>> = flow {
        emit(animeRepository.getAllStudios())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allStaff: StateFlow<List<Staff>> = flow {
        emit(animeRepository.getAllStaff())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            settingsProvider.show18Plus.collectLatest { show18 ->
                if (!show18) {
                    val currentFilter = _searchFilter.value
                    var changed = false
                    val cleanGenres = currentFilter.genres.filter { !it.equals("Hentai", ignoreCase = true) }
                    val cleanExcludedGenres = currentFilter.excludedGenres.filter { !it.equals("Hentai", ignoreCase = true) }
                    if (cleanGenres.size != currentFilter.genres.size || cleanExcludedGenres.size != currentFilter.excludedGenres.size) {
                        changed = true
                    }
                    
                    val tags = animeRepository.getAllTags()
                    val nsfwTagIds = tags.filter { it.isNsfw() }.map { it.tagId }.toSet()
                    
                    val cleanTags = currentFilter.tags.filter { !nsfwTagIds.contains(it) }
                    val cleanExcludedTags = currentFilter.excludedTags.filter { !nsfwTagIds.contains(it) }
                    if (cleanTags.size != currentFilter.tags.size || cleanExcludedTags.size != currentFilter.excludedTags.size) {
                        changed = true
                    }
                    
                    if (changed) {
                        _searchFilter.value = currentFilter.copy(
                            genres = cleanGenres,
                            excludedGenres = cleanExcludedGenres,
                            tags = cleanTags,
                            excludedTags = cleanExcludedTags
                        )
                    }
                }
            }
        }
    }

    @Volatile
    private var currentPagingSource: AnimeCatalogPagingSource? = null

    val allTracking: StateFlow<Map<Long, UserTrackingEntity>?> = userTrackingDao.observeAllTracking()
        .map { list -> list.associateBy { it.anilistId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val pagingDataFlow: Flow<PagingData<AnimeWithTracking>> = combine(
        _searchFilter.debounce { query ->
            if (query.textQuery.isEmpty()) 0L else 250L
        },
        settingsProvider.show18Plus,
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { filter, _, _ ->
        filter
    }
    .distinctUntilChanged()
    .flatMapLatest { finalFilter ->
        Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = {
                val source = AnimeCatalogPagingSource(
                    animeRepository = animeRepository,
                    userTrackingDao = userTrackingDao,
                    filter = finalFilter,
                    pageSize = 30
                )
                currentPagingSource = source
                source
            }
        ).flow
    }
    .cachedIn(viewModelScope)

    val filteredCount: StateFlow<Int> = combine(
        _searchFilter.debounce { query ->
            if (query.textQuery.isEmpty()) 0L else 250L
        },
        userTrackingDao.observeAllTracking(),
        settingsProvider.show18Plus,
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { filter, trackingList, _, _ ->
        val trackingStatusIds = trackingList
            .filter { it.watchStatus in filter.trackingStatuses }
            .map { it.anilistId }
        val excludedTrackingStatusIds = trackingList
            .filter { it.watchStatus in filter.excludedTrackingStatuses }
            .map { it.anilistId }

        filter.copy(
            trackingStatusIds = trackingStatusIds,
            excludedTrackingStatusIds = excludedTrackingStatusIds
        )
    }
    .distinctUntilChanged()
    .flatMapLatest { finalFilter ->
        flow {
            emit(animeRepository.getAnimeCount(finalFilter))
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

    fun toggleMediaStatus(status: String) {
        val currentIncluded = _searchFilter.value.mediaStatuses.toMutableList()
        val currentExcluded = _searchFilter.value.excludedMediaStatuses.toMutableList()
        
        if (currentIncluded.contains(status)) {
            currentIncluded.remove(status)
            currentExcluded.add(status)
        } else if (currentExcluded.contains(status)) {
            currentExcluded.remove(status)
        } else {
            currentIncluded.add(status)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            mediaStatuses = currentIncluded,
            excludedMediaStatuses = currentExcluded
        )
    }

    fun toggleMediaSource(source: String) {
        val currentIncluded = _searchFilter.value.mediaSources.toMutableList()
        val currentExcluded = _searchFilter.value.excludedMediaSources.toMutableList()
        
        if (currentIncluded.contains(source)) {
            currentIncluded.remove(source)
            currentExcluded.add(source)
        } else if (currentExcluded.contains(source)) {
            currentExcluded.remove(source)
        } else {
            currentIncluded.add(source)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            mediaSources = currentIncluded,
            excludedMediaSources = currentExcluded
        )
    }

    fun selectStatusOnly(status: String) {
        _searchFilter.value = SearchFilterQuery(
            mediaStatuses = listOf(status),
            sortBy = SortOption.SCORE
        )
    }

    fun selectSourceOnly(source: String) {
        _searchFilter.value = SearchFilterQuery(
            mediaSources = listOf(source),
            sortBy = SortOption.SCORE
        )
    }

    fun clearMediaStatusFilters() {
        _searchFilter.value = _searchFilter.value.copy(
            mediaStatuses = emptyList(),
            excludedMediaStatuses = emptyList()
        )
    }

    fun clearMediaSourceFilters() {
        _searchFilter.value = _searchFilter.value.copy(
            mediaSources = emptyList(),
            excludedMediaSources = emptyList()
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

    fun updateYear(yearString: String) {
        val parsed = yearString.toIntOrNull()
        _searchFilter.value = _searchFilter.value.copy(year = parsed)
    }

    fun toggleSeason(season: String) {
        val current = _searchFilter.value.season
        val newVal = if (current == season) null else season
        _searchFilter.value = _searchFilter.value.copy(season = newVal)
    }

    fun setCurrentSeasonFilter() {
        val cal = java.util.Calendar.getInstance()
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH)

        val season = when (month) {
            java.util.Calendar.JANUARY, java.util.Calendar.FEBRUARY, java.util.Calendar.MARCH -> "WINTER"
            java.util.Calendar.APRIL, java.util.Calendar.MAY, java.util.Calendar.JUNE -> "SPRING"
            java.util.Calendar.JULY, java.util.Calendar.AUGUST, java.util.Calendar.SEPTEMBER -> "SUMMER"
            else -> "FALL"
        }

        _searchFilter.value = _searchFilter.value.copy(year = year, season = season)
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

    fun toggleStaffFilterState(staffId: Long) {
        val currentStaff = _searchFilter.value.staff.toMutableList()
        val currentExcluded = _searchFilter.value.excludedStaff.toMutableList()
        
        if (currentStaff.contains(staffId)) {
            currentStaff.remove(staffId)
            currentExcluded.add(staffId)
        } else if (currentExcluded.contains(staffId)) {
            currentExcluded.remove(staffId)
        } else {
            currentStaff.add(staffId)
        }
        
        _searchFilter.value = _searchFilter.value.copy(
            staff = currentStaff,
            excludedStaff = currentExcluded
        )
    }

    fun clearStaffFilters() {
        _searchFilter.value = _searchFilter.value.copy(
            staff = emptyList(),
            excludedStaff = emptyList()
        )
    }

    fun selectGenreOnly(genreSlug: String) {
        _searchFilter.value = SearchFilterQuery(
            genres = listOf(genreSlug),
            sortBy = SortOption.SCORE
        )
    }

    fun selectTagOnly(tagId: Long) {
        _searchFilter.value = SearchFilterQuery(
            tags = listOf(tagId),
            sortBy = SortOption.SCORE
        )
    }

    fun selectStaffOnly(staffId: Long) {
        _searchFilter.value = SearchFilterQuery(
            staff = listOf(staffId),
            sortBy = SortOption.SCORE
        )
    }

    fun selectStudioOnly(studioId: Long) {
        _searchFilter.value = SearchFilterQuery(
            studios = listOf(studioId),
            sortBy = SortOption.SCORE
        )
    }

    fun updateSortOrder(sortOption: SortOption) {
        _searchFilter.value = _searchFilter.value.copy(sortBy = sortOption)
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
data class UserStats(
    val episodesWatched: Int,
    val titlesCompleted: Int,
    val titlesWatching: Int
)
