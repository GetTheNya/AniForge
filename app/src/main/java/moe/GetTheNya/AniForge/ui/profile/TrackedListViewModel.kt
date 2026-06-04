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
import javax.inject.Inject

@HiltViewModel
class TrackedListViewModel @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val activeTab = MutableStateFlow("")

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

    val allTrackedAnime: StateFlow<List<Anime>> = combine(
        trackingSnapshot,
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { trackingList, _ ->
        val ids = trackingList.map { it.anilistId }
        if (ids.isEmpty()) {
            emptyList()
        } else {
            animeRepository.getAnimeByIds(ids)
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

    val preferUk: StateFlow<Boolean> = settingsProvider.preferUkTitles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setActiveTab(statusId: String) {
        activeTab.value = statusId
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun getRandomAnime(currentStatusId: String): Anime? {
        val currentList = filteredAnime.value
        if (currentList.isEmpty()) return null
        return currentList.randomOrNull()
    }

    fun updateWatchStatus(anilistId: Long, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
                ) ?: UserTrackingEntity(
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
}
