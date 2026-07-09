package moe.GetTheNya.AniForge.ui.social

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.CollectionDao
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef
import moe.GetTheNya.AniForge.sync.SyncEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.network.SocialRepository
import moe.GetTheNya.AniForge.core.network.SupabaseUserTrackingDto
import javax.inject.Inject

data class SharedCollectionFilterParams(
    val query: String,
    val friendFilters: Map<String, Int>,
    val localFilters: Map<String, Int>,
    val coWatch: Boolean,
    val moviesOnly: Boolean
)

@HiltViewModel
class SharedCollectionDetailViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider,
    private val userTrackingDao: UserTrackingDao,
    private val collectionDao: CollectionDao,
    private val syncEngine: SyncEngine
) : ViewModel() {

    val localCompletedAnimeIds = userTrackingDao.observeAllTracking()
        .map { list -> list.filter { it.watchStatus == "COMPLETED" }.map { it.anilistId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val targetUserId: String get() = savedStateHandle.get<String>("targetUserId") ?: ""
    val collectionId: String get() = savedStateHandle.get<String>("collectionId") ?: ""

    private val _collectionTitle = MutableStateFlow("")
    val collectionTitle = _collectionTitle.asStateFlow()

    private val _collectionDescription = MutableStateFlow("")
    val collectionDescription = _collectionDescription.asStateFlow()

    private val _animeList = MutableStateFlow<List<Anime>>(emptyList())
    val animeList = _animeList.asStateFlow()

    private val _trackingMap = MutableStateFlow<Map<Long, SupabaseUserTrackingDto>>(emptyMap())
    val trackingMap = _trackingMap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val preferUk = settingsProvider.preferUkTitles

    private var loadJob: kotlinx.coroutines.Job? = null

    private val _localTrackingMap = MutableStateFlow<Map<Long, UserTrackingEntity>>(emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _friendStatusFilters = MutableStateFlow<Map<String, Int>>(emptyMap())
    val friendStatusFilters = _friendStatusFilters.asStateFlow()

    private val _localStatusFilters = MutableStateFlow<Map<String, Int>>(emptyMap())
    val localStatusFilters = _localStatusFilters.asStateFlow()

    private val _coWatchActive = MutableStateFlow(false)
    val coWatchActive = _coWatchActive.asStateFlow()

    private val _moviesOnlyActive = MutableStateFlow(false)
    val moviesOnlyActive = _moviesOnlyActive.asStateFlow()

    private val filterParams = combine(
        _searchQuery,
        _friendStatusFilters,
        _localStatusFilters,
        _coWatchActive,
        _moviesOnlyActive
    ) { query, friendFilters, localFilters, coWatch, moviesOnly ->
        SharedCollectionFilterParams(query, friendFilters, localFilters, coWatch, moviesOnly)
    }

    val filteredAnimeList = combine(
        _animeList,
        _trackingMap,
        _localTrackingMap,
        filterParams
    ) { animeList, trackingMap, localMap, params ->
        animeList.filter { anime ->
            // 1. Text Search
            val matchesText = if (params.query.isBlank()) {
                true
            } else {
                anime.titleRomaji.contains(params.query, ignoreCase = true) ||
                anime.titleEn?.contains(params.query, ignoreCase = true) == true ||
                anime.titleUk?.contains(params.query, ignoreCase = true) == true
            }
            if (!matchesText) return@filter false

            // 2. Co-Watch check
            if (params.coWatch) {
                val friendTracking = trackingMap[anime.anilistId]
                val friendStatus = friendTracking?.watchStatus ?: ""
                val localTracking = localMap[anime.anilistId]
                val localStatus = if (localTracking != null && !localTracking.isDeleted) localTracking.watchStatus else ""
                if (friendStatus != "PLANNING" || localStatus != "PLANNING") {
                    return@filter false
                }
            } else {
                // Standard Friend Status Filters
                val friendTracking = trackingMap[anime.anilistId]
                val friendStatus = friendTracking?.watchStatus ?: ""
                val includedFriendStatuses = params.friendFilters.filter { it.value == 1 }.keys
                val excludedFriendStatuses = params.friendFilters.filter { it.value == 2 }.keys

                val matchesFriendStatus = when {
                    excludedFriendStatuses.contains(friendStatus) -> false
                    includedFriendStatuses.isNotEmpty() -> includedFriendStatuses.contains(friendStatus)
                    else -> true
                }
                if (!matchesFriendStatus) return@filter false

                // Standard Local User Status Filters
                val localTracking = localMap[anime.anilistId]
                val localStatus = if (localTracking != null && !localTracking.isDeleted) localTracking.watchStatus else ""

                val includedLocalStatuses = params.localFilters.filter { it.value == 1 }.keys
                val excludedLocalStatuses = params.localFilters.filter { it.value == 2 }.keys

                val matchesLocalStatus = when {
                    excludedLocalStatuses.contains(localStatus) -> false
                    includedLocalStatuses.isNotEmpty() -> includedLocalStatuses.contains(localStatus)
                    else -> true
                }
                if (!matchesLocalStatus) return@filter false
            }

            // 3. Movie Filter
            if (params.moviesOnly) {
                if (anime.format != "MOVIE") {
                    return@filter false
                }
            }

            true
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFriendStatusFilter(status: String) {
        val current = _friendStatusFilters.value[status] ?: 0
        val next = (current + 1) % 3
        _friendStatusFilters.value = _friendStatusFilters.value.toMutableMap().apply {
            put(status, next)
        }
    }

    fun toggleLocalStatusFilter(status: String) {
        val current = _localStatusFilters.value[status] ?: 0
        val next = (current + 1) % 3
        _localStatusFilters.value = _localStatusFilters.value.toMutableMap().apply {
            put(status, next)
        }
    }

    fun setSingleFriendStatus(status: String) {
        _friendStatusFilters.value = mapOf(status to 1)
    }

    fun toggleCoWatch() {
        _coWatchActive.value = !_coWatchActive.value
    }

    fun toggleMoviesOnly() {
        _moviesOnlyActive.value = !_moviesOnlyActive.value
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _friendStatusFilters.value = emptyMap()
        _localStatusFilters.value = emptyMap()
        _coWatchActive.value = false
        _moviesOnlyActive.value = false
    }

    init {
        viewModelScope.launch {
            userTrackingDao.observeAllTracking().collect { list ->
                _localTrackingMap.value = list.associateBy { it.anilistId }
            }
        }
    }

    fun loadSharedCollection() {
        if (targetUserId.isBlank() || collectionId.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // 1. Fetch remote collection metadata
                val collectionDto = socialRepository.getRemoteCollection(targetUserId, collectionId)
                if (collectionDto == null) {
                    _errorMessage.value = "Collection not found"
                    return@launch
                }
                _collectionTitle.value = collectionDto.title
                _collectionDescription.value = collectionDto.description

                // 2. Fetch cross refs
                val crossRefs = socialRepository.getRemoteCollectionCrossRefsForCollection(targetUserId, collectionId)
                val sortedCrossRefs = crossRefs.sortedBy { it.orderIndex }
                val animeIds = sortedCrossRefs.map { it.animeId }

                // 3. Fetch local anime detail
                if (animeIds.isNotEmpty()) {
                    val animeDetailsList = withContext(Dispatchers.IO) {
                        animeRepository.getAnimeByIds(animeIds)
                    }
                    val animeMap = animeDetailsList.associateBy { it.anilistId }
                    // Order them according to orderIndex in cross refs
                    val sortedAnimeList = sortedCrossRefs.mapNotNull { ref -> animeMap[ref.animeId] }
                    _animeList.value = sortedAnimeList

                    // 4. Fetch remote tracking list to display scores/statuses of target user
                    val trackingRecords = socialRepository.getRemoteUserTrackingList(targetUserId)
                    _trackingMap.value = trackingRecords.filter { it.anilistId in animeIds }.associateBy { it.anilistId }
                } else {
                    _animeList.value = emptyList()
                    _trackingMap.value = emptyMap()
                }
            } catch (e: Exception) {
                Log.e("SharedCollectionDetailVM", "Error loading remote collection", e)
                _errorMessage.value = e.localizedMessage ?: "Failed to load collection"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cloneCollectionToLibrary(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val title = _collectionTitle.value
        val description = _collectionDescription.value
        val animeIds = _animeList.value.map { it.anilistId }

        if (title.isBlank()) {
            onFailure("Collection metadata is not loaded")
            return
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    collectionDao.cloneCollection(title, description, animeIds)
                }

                // Silent background push
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        syncEngine.pushDirtyCollectionsOnly()
                    } catch (e: Exception) {
                        Log.e("SharedCollectionDetailVM", "Silent backup sync failed", e)
                    }
                }

                onSuccess()
            } catch (e: Exception) {
                Log.e("SharedCollectionDetailVM", "Failed to clone collection", e)
                onFailure(e.localizedMessage ?: "Cloning failed")
            }
        }
    }
}
