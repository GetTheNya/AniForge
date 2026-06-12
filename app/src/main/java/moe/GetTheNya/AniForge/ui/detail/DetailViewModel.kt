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
import androidx.lifecycle.SavedStateHandle
import moe.GetTheNya.AniForge.ui.navigation.Screen
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.Franchise
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.AnimeStaff
import moe.GetTheNya.AniForge.core.model.Studio
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed interface DetailUiEvent {
    data class Navigate(val screen: Screen) : DetailUiEvent
    data class ShowToast(val messageKey: String) : DetailUiEvent
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao,
    private val collectionDao: moe.GetTheNya.AniForge.core.database.dao.CollectionDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val collections = collectionDao.observeCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val animeCollectionIds: StateFlow<List<Int>> = savedStateHandle.getStateFlow<Long>("anilistId", 0L)
        .flatMapLatest { animeId ->
            collectionDao.observeCollectionIdsForAnime(animeId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleAnimeInCollection(collectionId: Int) {
        val animeId = currentAnimeId
        viewModelScope.launch(Dispatchers.IO) {
            val list = animeCollectionIds.value
            if (list.contains(collectionId)) {
                collectionDao.deleteCrossRef(collectionId, animeId)
            } else {
                val maxIndex = collectionDao.getMaxOrderIndex(collectionId) ?: -1
                val ref = moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef(
                    collectionId = collectionId,
                    animeId = animeId,
                    orderIndex = maxIndex + 1
                )
                collectionDao.insertCrossRef(ref)
            }
        }
    }

    var sourceStatusId: String? = savedStateHandle.get<String>("sourceStatusId")
    var rouletteCount: Int = savedStateHandle.get<Int>("rouletteCount") ?: 0
    var visitedIds: String = savedStateHandle.get<String>("visitedIds") ?: ""

    private val _uiEvent = MutableSharedFlow<DetailUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentAnimeId: Long = 0L

    fun MapsToNextRandomAnime() {
        val statusId = sourceStatusId ?: return
        val visitedSet = if (visitedIds.isBlank()) {
            emptySet<Long>()
        } else {
            visitedIds.split(",").mapNotNull { it.toLongOrNull() }.toSet()
        }

        viewModelScope.launch {
            try {
                val ids = userTrackingDao.getAnimeIdsByStatus(statusId)
                val remainingIds = ids.filter { it != currentAnimeId && !visitedSet.contains(it) }
                val nextRandomId = remainingIds.randomOrNull()

                if (nextRandomId != null) {
                    val newVisitedIds = if (visitedIds.isBlank()) {
                        "$currentAnimeId,$nextRandomId"
                    } else {
                        "$visitedIds,$nextRandomId"
                    }
                    _uiEvent.emit(
                        DetailUiEvent.Navigate(
                            Screen.Detail(
                                anilistId = nextRandomId,
                                sourceStatusId = statusId,
                                rouletteCount = rouletteCount + 1,
                                visitedIds = newVisitedIds
                            )
                        )
                    )
                } else {
                    _uiEvent.emit(DetailUiEvent.ShowToast("rouletteExhausted"))
                }
            } catch (e: Exception) {
                // handle safely
            }
        }
    }

    fun loadAnimeDetail(
        anilistId: Long,
        sourceStatusId: String? = null,
        rouletteCount: Int = 0,
        visitedIds: String = ""
    ) {
        currentAnimeId = anilistId
        this.sourceStatusId = sourceStatusId
        this.rouletteCount = rouletteCount
        this.visitedIds = visitedIds
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val anime = animeRepository.getAnimeById(anilistId)
                if (anime == null) {
                    _uiState.value = DetailUiState.Error("Anime not found in catalog")
                    return@launch
                }

                // Load screenshots, relations, franchise details, genres, tags, and staff
                val screenshots = animeRepository.getScreenshots(anilistId)
                val relations = animeRepository.getRelations(anilistId)
                val franchise = animeRepository.getFranchiseForAnime(anilistId)
                val franchiseReleaseCount = if (franchise != null) {
                    animeRepository.getFranchiseAnime(franchise.franchiseId).size
                } else 0
                val genres = animeRepository.getGenresForAnime(anilistId)
                val tags = animeRepository.getTagsForAnime(anilistId)
                val staff = animeRepository.getStaffForAnime(anilistId)
                val studios = animeRepository.getStudiosForAnime(anilistId)

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
                        trackingMap = trackingMap,
                        franchise = franchise,
                        franchiseReleaseCount = franchiseReleaseCount,
                        genres = genres,
                        tags = tags,
                        staff = staff,
                        studios = studios
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
                val updated = currentTracking.copy(
                    watchStatus = "",
                    episodeProgress = 0,
                    lastModified = System.currentTimeMillis()
                )
                if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
                    userTrackingDao.delete(currentTracking)
                } else {
                    userTrackingDao.insertOrUpdate(updated)
                }
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
            val dbNotes = if (notes.isBlank()) null else notes
            val currentTracking = state.tracking
            
            if (currentTracking == null) {
                if (dbNotes != null) {
                    val newTracking = UserTrackingEntity(
                        anilistId = currentAnimeId,
                        watchStatus = "",
                        episodeProgress = 0,
                        score = null,
                        notes = dbNotes,
                        lastModified = System.currentTimeMillis()
                    )
                    userTrackingDao.insertOrUpdate(newTracking)
                }
            } else {
                val updated = currentTracking.copy(
                    notes = dbNotes,
                    lastModified = System.currentTimeMillis()
                )
                if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
                    userTrackingDao.delete(currentTracking)
                } else {
                    userTrackingDao.insertOrUpdate(updated)
                }
            }
        }
    }

    fun updateScore(score: Double?) {
        val state = _uiState.value as? DetailUiState.Success ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dbScore = if (score == null || score == 0.0) null else score
            val currentTracking = state.tracking
            
            if (currentTracking == null) {
                if (dbScore != null) {
                    val newTracking = UserTrackingEntity(
                        anilistId = currentAnimeId,
                        watchStatus = "",
                        episodeProgress = 0,
                        score = dbScore,
                        notes = null,
                        lastModified = System.currentTimeMillis()
                    )
                    userTrackingDao.insertOrUpdate(newTracking)
                }
            } else {
                val updated = currentTracking.copy(
                    score = dbScore,
                    lastModified = System.currentTimeMillis()
                )
                if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
                    userTrackingDao.delete(currentTracking)
                } else {
                    userTrackingDao.insertOrUpdate(updated)
                }
            }
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
        val trackingMap: Map<Long, String> = emptyMap(),
        val franchise: Franchise? = null,
        val franchiseReleaseCount: Int = 0,
        val genres: List<Genre> = emptyList(),
        val tags: List<Tag> = emptyList(),
        val staff: List<AnimeStaff> = emptyList(),
        val studios: List<Studio> = emptyList()
    ) : DetailUiState
    @Immutable
    data class Error(val message: String) : DetailUiState
}
