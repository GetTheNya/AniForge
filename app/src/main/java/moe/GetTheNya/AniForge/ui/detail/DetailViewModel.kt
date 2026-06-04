package moe.GetTheNya.AniForge.ui.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentAnimeId: Long = 0L

    fun loadAnimeDetail(anilistId: Long) {
        currentAnimeId = anilistId
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val anime = animeRepository.getAnimeById(anilistId)
                if (anime == null) {
                    _uiState.value = DetailUiState.Error("Anime not found in catalog")
                    return@launch
                }

                // Load screenshots and relations in parallel
                val screenshots = animeRepository.getScreenshots(anilistId)
                val relations = animeRepository.getRelations(anilistId)

                combine(
                    userTrackingDao.observeTrackingForAnime(anilistId),
                    userTrackingDao.observeAllTracking()
                ) { tracking, allTracking ->
                    val trackingMap = allTracking.associate { it.anilistId to it.watchStatus }
                    DetailUiState.Success(
                        anime = anime,
                        screenshots = screenshots,
                        relations = relations,
                        tracking = tracking,
                        trackingMap = trackingMap
                    )
                }.collect { successState ->
                    _uiState.value = successState
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Failed to load details")
            }
        }
    }

    fun updateWatchStatus(status: String) {
        val state = _uiState.value as? DetailUiState.Success ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val currentTracking = state.tracking
            if (currentTracking != null && currentTracking.watchStatus == status) {
                // Tapping a button that is already active must completely remove the anime from all tracking lists (delete database link).
                userTrackingDao.delete(currentTracking)
            } else {
                // New Selection
                val maxEpisodes = state.anime.episodes ?: 0
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
                    anilistId = currentAnimeId,
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

    fun incrementEpisodeProgress() {
        val state = _uiState.value as? DetailUiState.Success ?: return
        val maxEpisodes = state.anime.episodes ?: Int.MAX_VALUE
        viewModelScope.launch(Dispatchers.IO) {
            val currentTracking = state.tracking
            val currentProgress = currentTracking?.episodeProgress ?: 0
            if (currentProgress < maxEpisodes) {
                val newProgress = currentProgress + 1
                val newStatus = if (newProgress == maxEpisodes) {
                    "COMPLETED"
                } else if (currentTracking == null || currentTracking.watchStatus == "PLANNING") {
                    "CURRENT"
                } else {
                    currentTracking.watchStatus
                }

                val updated = currentTracking?.copy(
                    episodeProgress = newProgress,
                    watchStatus = newStatus,
                    lastModified = System.currentTimeMillis()
                ) ?: UserTrackingEntity(
                    anilistId = currentAnimeId,
                    watchStatus = newStatus,
                    episodeProgress = newProgress,
                    score = null,
                    notes = null,
                    lastModified = System.currentTimeMillis()
                )
                userTrackingDao.insertOrUpdate(updated)
            }
        }
    }

    fun decrementEpisodeProgress() {
        val state = _uiState.value as? DetailUiState.Success ?: return
        val maxEpisodes = state.anime.episodes ?: Int.MAX_VALUE
        viewModelScope.launch(Dispatchers.IO) {
            val currentTracking = state.tracking ?: return@launch
            if (currentTracking.episodeProgress > 0) {
                val newProgress = currentTracking.episodeProgress - 1
                val newStatus = if (currentTracking.watchStatus == "COMPLETED" && newProgress < maxEpisodes) {
                    "CURRENT"
                } else {
                    currentTracking.watchStatus
                }

                val updated = currentTracking.copy(
                    episodeProgress = newProgress,
                    watchStatus = newStatus,
                    lastModified = System.currentTimeMillis()
                )
                userTrackingDao.insertOrUpdate(updated)
            }
        }
    }

    fun saveNotes(notes: String) {
        val state = _uiState.value as? DetailUiState.Success ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val currentTracking = state.tracking ?: UserTrackingEntity(
                anilistId = currentAnimeId,
                watchStatus = "PLANNING",
                episodeProgress = 0,
                score = null,
                notes = notes,
                lastModified = System.currentTimeMillis()
            )
            val updated = currentTracking.copy(
                notes = notes,
                lastModified = System.currentTimeMillis()
            )
            userTrackingDao.insertOrUpdate(updated)
        }
    }
}

@Immutable
sealed interface DetailUiState {
    @Immutable
    data object Loading : DetailUiState
    @Immutable
    data class Success(
        val anime: Anime,
        val screenshots: List<String>,
        val relations: List<Anime>,
        val tracking: UserTrackingEntity?,
        val trackingMap: Map<Long, String> = emptyMap()
    ) : DetailUiState
    @Immutable
    data class Error(val message: String) : DetailUiState
}
