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
    private val savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
    private val animeRepository: AnimeRepository
) : ViewModel() {

    val userId: String = savedStateHandle.get<String>("userId") ?: ""
    val username: String = savedStateHandle.get<String>("username") ?: ""

    private val _uiState = MutableStateFlow(
        SharedProfileUiState(
            username = username,
            avatarLetter = username.firstOrNull()?.uppercase() ?: "?"
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
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
