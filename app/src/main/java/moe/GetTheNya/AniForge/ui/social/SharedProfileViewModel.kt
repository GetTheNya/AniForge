package moe.GetTheNya.AniForge.ui.social

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.BentoStatsData
import moe.GetTheNya.AniForge.core.model.FranchiseGiantInfo
import moe.GetTheNya.AniForge.core.network.SocialRepository
import moe.GetTheNya.AniForge.core.network.SupabaseUserTrackingDto
import javax.inject.Inject

data class FriendTrackingItem(
    val tracking: SupabaseUserTrackingDto,
    val anime: Anime?
)

data class SharedProfileFilterParams(
    val query: String,
    val friendFilters: Map<String, Int>,
    val localFilters: Map<String, Int>,
    val coWatch: Boolean,
    val moviesOnly: Boolean
)

data class SharedCollectionWithData(
    val collectionId: String,
    val title: String,
    val description: String,
    val animeList: List<Anime>,
    val posters: List<String>,
    val totalCount: Int,
    val statusCounts: Map<String, Int>
)

data class SharedProfileUiState(
    val username: String = "",
    val avatarLetter: String = "",
    val userStats: UserStatsEntity = UserStatsEntity(),
    val bentoStats: BentoStatsData = BentoStatsData(),
    val trackingStats: Map<String, Int> = emptyMap(),
    val collectionsList: List<SharedCollectionWithData> = emptyList(),
    val segmentedWatchLists: Map<String, List<FriendTrackingItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SharedProfileViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao
) : ViewModel() {

    val userId: String get() = savedStateHandle.get<String>("userId") ?: ""
    val username: String get() = savedStateHandle.get<String>("username") ?: ""

    private val _uiState = MutableStateFlow(
        SharedProfileUiState(
            username = username,
            avatarLetter = username.firstOrNull()?.uppercase() ?: "?"
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _allFriendTrackingItems = MutableStateFlow<List<FriendTrackingItem>>(emptyList())
    private val _localTrackingMap = MutableStateFlow<Map<Long, UserTrackingEntity>>(emptyMap())
    private var loadJob: kotlinx.coroutines.Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _friendStatusFilters = MutableStateFlow<Map<String, Int>>(
        mapOf("CURRENT" to 1) // default to CURRENT (Watching) as Include
    )
    val friendStatusFilters = _friendStatusFilters.asStateFlow()

    private val _localStatusFilters = MutableStateFlow<Map<String, Int>>(emptyMap())
    val localStatusFilters = _localStatusFilters.asStateFlow()

    private val _coWatchActive = MutableStateFlow(false)
    val coWatchActive = _coWatchActive.asStateFlow()

    private val _moviesOnlyActive = MutableStateFlow(false)
    val moviesOnlyActive = _moviesOnlyActive.asStateFlow()

    private val _selectedRouletteAnime = MutableStateFlow<Anime?>(null)
    val selectedRouletteAnime = _selectedRouletteAnime.asStateFlow()

    val compatibilityPercentage = combine(
        _allFriendTrackingItems,
        _localTrackingMap
    ) { friendItems, localMap ->
        val friendIds = friendItems.map { it.tracking.anilistId }.toSet()
        if (friendIds.isEmpty()) return@combine 0
        val localIds = localMap.keys
        val commonIds = friendIds.intersect(localIds)
        (commonIds.size.toDouble() / friendIds.size * 100).toInt()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private val filterParams = combine(
        _searchQuery,
        _friendStatusFilters,
        _localStatusFilters,
        _coWatchActive,
        _moviesOnlyActive
    ) { query, friendFilters, localFilters, coWatch, moviesOnly ->
        SharedProfileFilterParams(query, friendFilters, localFilters, coWatch, moviesOnly)
    }

    val filteredWatchList = combine(
        _allFriendTrackingItems,
        _localTrackingMap,
        filterParams
    ) { allItems, localMap, params ->
        allItems.filter { item ->
            // 1. Text Search
            val matchesText = if (params.query.isBlank()) {
                true
            } else {
                val anime = item.anime
                anime != null && (
                    anime.titleRomaji.contains(params.query, ignoreCase = true) ||
                    anime.titleEn?.contains(params.query, ignoreCase = true) == true ||
                    anime.titleUk?.contains(params.query, ignoreCase = true) == true
                )
            }
            if (!matchesText) return@filter false

            // 2. Co-Watch check
            if (params.coWatch) {
                // The Friend's remote status is strictly PLANNING.
                // The Local user's Room database status for the same anilist_id is also strictly PLANNING.
                val friendStatus = item.tracking.watchStatus
                val localTracking = localMap[item.tracking.anilistId]
                val localStatus = if (localTracking != null && !localTracking.isDeleted) localTracking.watchStatus else ""
                if (friendStatus != "PLANNING" || localStatus != "PLANNING") {
                    return@filter false
                }
            } else {
                // Standard Friend Status Filters
                val friendStatus = item.tracking.watchStatus
                val includedFriendStatuses = params.friendFilters.filter { it.value == 1 }.keys
                val excludedFriendStatuses = params.friendFilters.filter { it.value == 2 }.keys

                val matchesFriendStatus = when {
                    excludedFriendStatuses.contains(friendStatus) -> false
                    includedFriendStatuses.isNotEmpty() -> includedFriendStatuses.contains(friendStatus)
                    else -> true
                }
                if (!matchesFriendStatus) return@filter false

                // Standard Local User Status Filters
                val localTracking = localMap[item.tracking.anilistId]
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
                val animeFormat = item.anime?.format
                if (animeFormat != "MOVIE") {
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

    fun rollRoulette() {
        val currentList = filteredWatchList.value.mapNotNull { it.anime }
        _selectedRouletteAnime.value = currentList.randomOrNull()
    }

    fun clearRoulette() {
        _selectedRouletteAnime.value = null
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _friendStatusFilters.value = mapOf("CURRENT" to 1)
        _localStatusFilters.value = emptyMap()
        _coWatchActive.value = false
        _moviesOnlyActive.value = false
        _selectedRouletteAnime.value = null
    }

    init {
        viewModelScope.launch {
            userTrackingDao.observeAllTracking().collect { list ->
                _localTrackingMap.value = list.associateBy { it.anilistId }
            }
        }
    }

    fun loadProfileData() {
        if (userId.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                username = username,
                avatarLetter = username.firstOrNull()?.uppercase() ?: "?",
                isLoading = true,
                errorMessage = null
            )
            try {
                // Fetch user tracking list, collections, and cross references in parallel
                val (trackingRecords, remoteCollections, crossRefs) = coroutineScope {
                    val trackingDeferred = async { socialRepository.getRemoteUserTrackingList(userId) }
                    val collectionsDeferred = async { socialRepository.getRemoteCollections(userId) }
                    val crossRefsDeferred = async { socialRepository.getRemoteCollectionCrossRefs(userId) }

                    Triple(
                        trackingDeferred.await(),
                        collectionsDeferred.await(),
                        crossRefsDeferred.await()
                    )
                }

                // Gather all anime IDs to resolve details in one batch
                val trackingAnimeIds = trackingRecords.map { it.anilistId }
                val crossRefAnimeIds = crossRefs.map { it.animeId }
                val allAnimeIds = (trackingAnimeIds + crossRefAnimeIds).distinct()

                val animeList = withContext(Dispatchers.IO) {
                    animeRepository.getAnimeByIds(allAnimeIds)
                }
                val animeMap = animeList.associateBy { it.anilistId }

                // 1. Group tracking records by watch status and pair them with Anime
                val pairedItems = trackingRecords.map { record ->
                    FriendTrackingItem(
                        tracking = record,
                        anime = animeMap[record.anilistId]
                    )
                }
                val segmentedWatchLists = pairedItems.groupBy { it.tracking.watchStatus }

                // 2. Build tracking stats (status counts)
                val trackingStats = trackingRecords.groupBy { it.watchStatus }.mapValues { it.value.size }

                // 3. Replicate bento metrics calculations
                val activeTracking = trackingRecords.filter {
                    it.watchStatus == "COMPLETED" || (it.watchStatus == "CURRENT" && it.episodeProgress > 0)
                }
                val activeAnimeIds = activeTracking.map { it.anilistId }
                val activeProgressMap = activeTracking.associate { it.anilistId to it.episodeProgress }

                val genreDist = if (activeAnimeIds.isNotEmpty()) {
                    animeRepository.getGenreDistributions(activeAnimeIds)
                } else emptyList()

                val studioDist = if (activeAnimeIds.isNotEmpty()) {
                    animeRepository.getStudioDistributions(activeAnimeIds)
                } else emptyList()

                val franchiseGiant = if (activeAnimeIds.isNotEmpty()) {
                    val animeFranchises = animeRepository.getFranchisesForAnimeIds(activeAnimeIds)
                    val franchiseProgress = mutableMapOf<Long, Int>()
                    for ((animeId, franchiseId) in animeFranchises) {
                        val progress = activeProgressMap[animeId] ?: 0
                        franchiseProgress[franchiseId] = (franchiseProgress[franchiseId] ?: 0) + progress
                    }
                    val topFranchiseId = franchiseProgress.maxByOrNull { it.value }?.key
                    if (topFranchiseId != null) {
                        val franchise = animeRepository.getFranchiseById(topFranchiseId)
                        val totalEps = franchiseProgress[topFranchiseId] ?: 0
                        if (franchise != null && totalEps > 0) {
                            FranchiseGiantInfo(franchise, totalEps)
                        } else null
                    } else null
                } else null

                val collectionAnimeIds = crossRefs.map { it.animeId }.distinct()
                val collectionCovers = if (collectionAnimeIds.isNotEmpty()) {
                    animeRepository.getAnimeByIds(collectionAnimeIds.take(4)).mapNotNull { it.coverLarge }
                } else emptyList()

                val bentoStats = BentoStatsData(
                    genreDistributions = genreDist,
                    studioDistributions = studioDist,
                    franchiseGiant = franchiseGiant,
                    activeCollectionsCount = remoteCollections.size,
                    collectionCovers = collectionCovers
                )

                // 4. Calculate total watch time
                var totalWatchTime = 0L
                for (tracking in trackingRecords) {
                    val anime = animeMap[tracking.anilistId]
                    if (anime != null) {
                        totalWatchTime += tracking.episodeProgress.toLong() * (anime.duration ?: 0)
                    }
                }
                val userStats = UserStatsEntity(
                    id = 0,
                    totalWatchTimeMinutes = totalWatchTime,
                    chaosMeterCount = 0 // Read-only view defaults chaos to 0
                )

                // 5. Aggregate collections list details
                val collectionsList = remoteCollections.map { col ->
                    val colRefs = crossRefs.filter { it.collectionId == col.collectionId }
                    val colAnimeIds = colRefs.sortedBy { it.orderIndex }.map { it.animeId }
                    val colAnimeList = colAnimeIds.mapNotNull { animeMap[it] }
                    val posters = colAnimeList.mapNotNull { it.coverLarge }
                    val statusCounts = trackingRecords.filter { it.anilistId in colAnimeIds }
                        .groupBy { it.watchStatus }
                        .mapValues { it.value.size }

                    SharedCollectionWithData(
                        collectionId = col.collectionId,
                        title = col.title,
                        description = col.description,
                        animeList = colAnimeList,
                        posters = posters,
                        totalCount = colRefs.size,
                        statusCounts = statusCounts
                    )
                }

                // 6. Update UI state
                _allFriendTrackingItems.value = pairedItems
                _uiState.value = _uiState.value.copy(
                    userStats = userStats,
                    bentoStats = bentoStats,
                    trackingStats = trackingStats,
                    collectionsList = collectionsList,
                    segmentedWatchLists = segmentedWatchLists,
                    isLoading = false
                )

            } catch (e: Exception) {
                Log.e("SharedProfileViewModel", "Error fetching remote profile data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to fetch profile details"
                )
            }
        }
    }
}
