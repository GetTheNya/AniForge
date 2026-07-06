package moe.GetTheNya.AniForge.ui.franchises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.CollectionDao
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.ui.dashboard.QuickGestureAction
import moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository
import moe.GetTheNya.AniForge.core.model.Franchise
import moe.GetTheNya.AniForge.core.model.ListSortOption
import moe.GetTheNya.AniForge.core.model.ListFilterState
import moe.GetTheNya.AniForge.core.model.AnimeFormat
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.Studio
import moe.GetTheNya.AniForge.core.model.Staff
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.ui.navigation.NavController
import moe.GetTheNya.AniForge.ui.navigation.Screen
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import javax.inject.Inject

data class FranchiseItem(
    val franchise: Franchise,
    val releases: List<Anime>
)

data class CollectionWithData(
    val collection: CollectionEntity,
    val animeList: List<Anime>,
    val posters: List<String>,
    val totalCount: Int,
    val statusCounts: Map<String, Int>
)

enum class LibraryFilter(val dbStatus: String?) {
    PLANNING("PLANNING"),
    WATCHING("CURRENT"),
    COMPLETED("COMPLETED"),
    PAUSED("PAUSED"),
    DROPPED("DROPPED"),
    COLLECTIONS(null)
}

sealed interface LibrarySectionData {
    data class TrackedAnime(val list: List<Anime>) : LibrarySectionData
    data class Collections(val list: List<CollectionWithData>) : LibrarySectionData
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val collectionDao: CollectionDao,
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider,
    private val userTrackingRepository: UserTrackingRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    private val _listFilterState = MutableStateFlow(ListFilterState())
    val listFilterState = _listFilterState.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    val allTracking: StateFlow<List<UserTrackingEntity>> = userTrackingDao.observeAllTracking()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trackingMap: StateFlow<Map<Long, String>> = allTracking
        .map { list -> list.associate { it.anilistId to it.watchStatus } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val trackingEntitiesMap: StateFlow<Map<Long, UserTrackingEntity>> = allTracking
        .map { list -> list.associateBy { it.anilistId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeTrackedAnimeForStatus(status: String): Flow<List<Anime>> {
        return combine(
            allTracking,
            listFilterState,
            preferUk,
            searchQuery,
            animeRepository.swapSignal.onStart { emit(Unit) }
        ) { trackingList, filterState, ukPref, query, _ ->
            val statusTracking = trackingList.filter { it.watchStatus == status }
            if (statusTracking.isEmpty()) {
                emptyList()
            } else {
                val ids = statusTracking.map { it.anilistId }
                val queryParams = SearchFilterQuery(
                    genres = filterState.genres,
                    excludedGenres = filterState.excludedGenres,
                    formats = filterState.formats,
                    excludedFormats = filterState.excludedFormats,
                    trackingStatuses = listOf("DUMMY"),
                    trackingStatusIds = ids
                )
                val matchedAnimeList = animeRepository.queryAnime(queryParams)
                val trackingMap = statusTracking.associateBy { it.anilistId }
                
                val sorted = matchedAnimeList.sortedWith { a, b ->
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
                
                if (query.isBlank()) {
                    sorted
                } else {
                    sorted.filter { anime ->
                        anime.titleRomaji.contains(query, ignoreCase = true) ||
                        (anime.titleEn?.contains(query, ignoreCase = true) == true) ||
                        (anime.titleUk?.contains(query, ignoreCase = true) == true)
                    }
                }
            }
        }
    }

    private val _activeFilter = MutableStateFlow<LibraryFilter>(LibraryFilter.WATCHING)
    val activeFilter = _activeFilter.asStateFlow()

    fun setActiveFilter(filter: LibraryFilter) {
        _activeFilter.value = filter
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeSectionData: StateFlow<LibrarySectionData> = activeFilter
        .flatMapLatest { filter ->
            if (filter == LibraryFilter.COLLECTIONS) {
                filteredCollections.map { LibrarySectionData.Collections(it) }
            } else {
                val dbStatus = filter.dbStatus ?: ""
                observeTrackedAnimeForStatus(dbStatus).map { LibrarySectionData.TrackedAnime(it) }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibrarySectionData.TrackedAnime(emptyList())
        )

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

    // --- Collections Section ---

    val selectedCollectionIds = MutableStateFlow<Set<String>>(emptySet())
    val isInSelectionMode: StateFlow<Boolean> = selectedCollectionIds.map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleCollectionSelection(collectionId: String) {
        val current = selectedCollectionIds.value
        if (current.contains(collectionId)) {
            selectedCollectionIds.value = current - collectionId
        } else {
            selectedCollectionIds.value = current + collectionId
        }
    }

    fun clearSelection() {
        selectedCollectionIds.value = emptySet()
    }

    fun deleteSelectedCollections() {
        viewModelScope.launch(Dispatchers.IO) {
            val idsToDelete = selectedCollectionIds.value.toList()
            if (idsToDelete.isNotEmpty()) {
                collectionDao.softDeleteCollectionsWithRefs(idsToDelete)
                clearSelection()
            }
        }
    }

    val preferUk: StateFlow<Boolean> = settingsProvider.preferUkTitles

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val collections: StateFlow<List<CollectionWithData>> = collectionDao.observeCollections()
        .flatMapLatest { list ->
            if (list.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = list.map { collection ->
                    collectionDao.observeCrossRefsForCollection(collection.id).flatMapLatest { refs ->
                        userTrackingDao.observeAllTracking().map { trackingList ->
                            val trackingMap = trackingList.associate { it.anilistId to it.watchStatus }
                            val ids = refs.map { it.animeId }
                            val animeList = animeRepository.getAnimeByIds(ids)
                            val animeMap = animeList.associateBy { it.anilistId }
                            
                            val sortedAnime = refs.mapNotNull { ref -> animeMap[ref.animeId] }
                            val posters = sortedAnime.mapNotNull { it.coverLarge }.take(3)
                            
                            val statusCounts = sortedAnime.associate { anime ->
                                anime.anilistId to (trackingMap[anime.anilistId] ?: "UNVIEWED")
                            }.values.groupBy { it }.mapValues { it.value.size }
                            
                            CollectionWithData(
                                collection = collection,
                                animeList = sortedAnime,
                                posters = posters,
                                totalCount = sortedAnime.size,
                                statusCounts = statusCounts
                            )
                        }
                    }
                }
                combine(flows) { it.toList() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredCollections: StateFlow<List<CollectionWithData>> = combine(
        collections,
        searchQuery
    ) { collList, query ->
        if (query.isBlank()) {
            collList
        } else {
            collList.filter { item ->
                item.collection.title.contains(query, ignoreCase = true) ||
                item.collection.description.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createCollection(title: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val collection = CollectionEntity(
                title = title,
                description = description,
                createdAt = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false,
                lastModified = System.currentTimeMillis()
            )
            collectionDao.insertCollection(collection)
        }
    }

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
        viewModelScope.launch {
            settingsProvider.show18Plus.collectLatest { show18 ->
                if (!show18) {
                    val currentFilter = _listFilterState.value
                    var changed = false
                    val cleanGenres = currentFilter.genres.filter { !it.equals("Hentai", ignoreCase = true) }
                    val cleanExcludedGenres = currentFilter.excludedGenres.filter { !it.equals("Hentai", ignoreCase = true) }
                    if (cleanGenres.size != currentFilter.genres.size || cleanExcludedGenres.size != currentFilter.excludedGenres.size) {
                        changed = true
                    }
                    if (changed) {
                        _listFilterState.value = currentFilter.copy(
                            genres = cleanGenres,
                            excludedGenres = cleanExcludedGenres
                        )
                    }
                }
            }
        }
    }

    fun pickRandomAnime(navController: NavController): Boolean {
        val currentList = (activeSectionData.value as? LibrarySectionData.TrackedAnime)?.list ?: return false
        val randomAnime = currentList.randomOrNull()
        if (randomAnime != null) {
            viewModelScope.launch {
                userTrackingRepository.incrementChaosMeter()
            }
            navController.navigate(
                Screen.Detail(
                    anilistId = randomAnime.anilistId,
                    sourceStatusId = activeFilter.value.dbStatus,
                    rouletteCount = 1,
                    visitedIds = randomAnime.anilistId.toString()
                )
            )
            return true
        }
        return false
    }
}
