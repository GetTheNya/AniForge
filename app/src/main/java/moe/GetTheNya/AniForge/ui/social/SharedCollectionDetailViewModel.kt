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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.network.SocialRepository
import moe.GetTheNya.AniForge.core.network.SupabaseUserTrackingDto
import javax.inject.Inject

@HiltViewModel
class SharedCollectionDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider,
    private val userTrackingDao: UserTrackingDao
) : ViewModel() {

    val localCompletedAnimeIds = userTrackingDao.observeAllTracking()
        .map { list -> list.filter { it.watchStatus == "COMPLETED" }.map { it.anilistId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val targetUserId: String = savedStateHandle.get<String>("targetUserId") ?: ""
    val collectionId: String = savedStateHandle.get<String>("collectionId") ?: ""

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

    init {
        loadSharedCollection()
    }

    fun loadSharedCollection() {
        if (targetUserId.isBlank() || collectionId.isBlank()) return
        viewModelScope.launch {
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
}
