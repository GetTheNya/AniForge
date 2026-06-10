package moe.GetTheNya.AniForge.ui.franchises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.Franchise
import moe.GetTheNya.AniForge.core.model.RelationType
import moe.GetTheNya.AniForge.core.model.Relation
import javax.inject.Inject

data class FranchiseNode(
    val anime: Anime,
    val relationType: RelationType,
    val isMainAxis: Boolean,
    val isCurrentFocus: Boolean = false
)

sealed interface FranchiseTreeUiState {
    object Loading : FranchiseTreeUiState
    data class Success(
        val franchise: Franchise,
        val nodes: List<FranchiseNode>,
        val trackingMap: Map<Long, String>
    ) : FranchiseTreeUiState
    data class Error(val message: String) : FranchiseTreeUiState
}

@HiltViewModel
class FranchiseTreeViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _franchiseIdFlow = MutableStateFlow(0L)
    val franchiseIdFlow: StateFlow<Long> = _franchiseIdFlow.asStateFlow()

    fun loadFranchiseTree(id: Long) {
        _franchiseIdFlow.value = id
    }

    val activeAnimeIdFlow: StateFlow<Long?> = savedStateHandle.getStateFlow<Long?>("activeAnimeId", null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FranchiseTreeUiState> = franchiseIdFlow
        .flatMapLatest { id ->
            if (id == 0L) {
                flowOf<FranchiseTreeUiState>(FranchiseTreeUiState.Loading)
            } else {
                flow<FranchiseTreeUiState> {
                    emit(FranchiseTreeUiState.Loading)
                    try {
                        val franchise = animeRepository.getFranchiseById(id)
                        if (franchise == null) {
                            emit(FranchiseTreeUiState.Error("Franchise not found"))
                            return@flow
                        }

                        val animeList = animeRepository.getFranchiseAnime(id)
                        val relations = animeRepository.getFranchiseRelations(id)

                        // Sort the animeList chronologically
                        val sortedAnime = animeList.sortedWith(
                            compareBy<Anime> { it.startDateYear ?: it.seasonYear ?: 9999 }
                                .thenBy { it.startDateMonth ?: 13 }
                                .thenBy { it.startDateDay ?: 32 }
                                .thenBy { it.titleRomaji }
                        )

                        val trackingFlow = userTrackingDao.observeAllTracking().map { list ->
                            list.associate { it.anilistId to it.watchStatus }
                        }

                        combine(trackingFlow, activeAnimeIdFlow) { trackingMap, activeAnimeId ->
                            val nodes = sortedAnime.map { anime ->
                                val relType = resolveRelationType(anime.anilistId, franchise.mainAnilistId, relations)
                                val isMainAxis = when (relType) {
                                    RelationType.SEQUEL, RelationType.PREQUEL, RelationType.PARENT -> true
                                    else -> false
                                }
                                FranchiseNode(
                                    anime = anime,
                                    relationType = relType,
                                    isMainAxis = isMainAxis,
                                    isCurrentFocus = anime.anilistId == activeAnimeId
                                )
                            }
                            FranchiseTreeUiState.Success(
                                franchise = franchise,
                                nodes = nodes,
                                trackingMap = trackingMap
                            )
                        }.collect { successState ->
                            emit(successState)
                        }
                    } catch (e: Exception) {
                        emit(FranchiseTreeUiState.Error(e.message ?: "Failed to load franchise tree"))
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FranchiseTreeUiState.Loading
        )

    fun loadTree() {
        val id = _franchiseIdFlow.value
        if (id != 0L) {
            _franchiseIdFlow.value = 0L
            _franchiseIdFlow.value = id
        }
    }

    private fun resolveRelationType(
        animeId: Long,
        mainAnimeId: Long,
        relations: List<Relation>
    ): RelationType {
        val incoming = relations.filter { it.targetAnilistId == animeId }
        if (incoming.isNotEmpty()) {
            val priority = listOf(
                "SEQUEL", "PREQUEL", "PARENT", "SIDE_STORY", "SPIN_OFF",
                "ALTERNATIVE", "ADAPTATION", "CHARACTER", "SUMMARY",
                "SOURCE", "COMPILATION", "CONTAINS", "OTHER"
            )
            val bestMatch = incoming.minByOrNull { rel ->
                val idx = priority.indexOf(rel.relationType.uppercase())
                if (idx == -1) priority.size else idx
            }
            if (bestMatch != null) {
                return RelationType.fromString(bestMatch.relationType)
            }
        }

        if (animeId == mainAnimeId) {
            return RelationType.PARENT
        }

        val outgoing = relations.filter { it.sourceAnilistId == animeId }
        if (outgoing.isNotEmpty()) {
            val edge = outgoing.firstOrNull()
            if (edge != null) {
                return RelationType.fromString(edge.relationType)
            }
        }

        return RelationType.OTHER
    }
}
