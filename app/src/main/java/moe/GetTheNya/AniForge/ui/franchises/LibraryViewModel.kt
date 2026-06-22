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
import moe.GetTheNya.AniForge.core.model.Franchise
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

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val collectionDao: CollectionDao,
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider
) : ViewModel() {

    // --- Franchises Section ---
    val searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pagingFranchisesFlow: Flow<PagingData<FranchiseItem>> = searchQuery
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

    // --- Collections Section ---
    val activeLibraryTab = MutableStateFlow<Int?>(null)

    val selectedCollectionIds = MutableStateFlow<Set<Long>>(emptySet())
    val isInSelectionMode: StateFlow<Boolean> = selectedCollectionIds.map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleCollectionSelection(collectionId: Long) {
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
            val idsToDelete = selectedCollectionIds.value.map { it.toInt() }
            if (idsToDelete.isNotEmpty()) {
                collectionDao.deleteCollectionsWithRefs(idsToDelete)
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
                createdAt = System.currentTimeMillis()
            )
            collectionDao.insertCollection(collection)
        }
    }
}
