package moe.GetTheNya.AniForge.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.GetTheNya.AniForge.core.database.dao.CollectionDao
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.model.Anime
import javax.inject.Inject
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.sync.SyncEngine

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val collectionDao: CollectionDao,
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao,
    private val settingsProvider: SettingsProvider,
    private val userTrackingRepository: moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository,
    private val syncEngine: SyncEngine,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var collectionId: String = savedStateHandle.get<String>("collectionId") ?: ""
        private set

    val preferUk = settingsProvider.preferUkTitles

    private val _collectionId = MutableStateFlow<String>("")
    private var dbCollectionJob: kotlinx.coroutines.Job? = null
    private var dbCrossRefsJob: kotlinx.coroutines.Job? = null

    private val _collection = MutableStateFlow<CollectionEntity?>(null)
    val collection = _collection.asStateFlow()

    private val _animeList = MutableStateFlow<List<Anime>>(emptyList())
    val animeList = _animeList.asStateFlow()

    private val _trackingMap = MutableStateFlow<Map<Long, String>>(emptyMap())
    val trackingMap = _trackingMap.asStateFlow()

    // Expose all user tracked anime to add/remove titles from collection
    private val _trackedAnime = MutableStateFlow<List<Anime>>(emptyList())
    val trackedAnime = _trackedAnime.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val searchResults = combine(_searchQuery.debounce(300), _trackedAnime) { query, tracked ->
        query to tracked
    }.flatMapLatest { (query, tracked) ->
        flow {
            if (query.isBlank()) {
                emit(tracked)
            } else {
                emit(animeRepository.queryAnime(moe.GetTheNya.AniForge.core.model.SearchFilterQuery(textQuery = query)))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearState() {
        _collection.value = null
        _animeList.value = emptyList()
        _trackingMap.value = emptyMap()
        _trackedAnime.value = emptyList()
        _searchQuery.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        clearState()
    }

    fun loadCollection(id: String) {
        if (collectionId != id) {
            collectionId = id
        }
        if (_collectionId.value == id && dbCollectionJob != null) return
        
        clearState()
        _collectionId.value = id
        
        dbCollectionJob?.cancel()
        dbCrossRefsJob?.cancel()
        
        dbCollectionJob = collectionDao.observeCollections()
            .map { list -> list.firstOrNull { it.id == id } }
            .onEach { _collection.value = it }
            .launchIn(viewModelScope)
            
        dbCrossRefsJob = collectionDao.observeCrossRefsForCollection(id)
            .map { refs ->
                val ids = refs.map { it.animeId }
                val animeMap = animeRepository.getAnimeByIds(ids).associateBy { it.anilistId }
                refs.mapNotNull { ref -> animeMap[ref.animeId] }
            }
            .onEach { _animeList.value = it }
            .launchIn(viewModelScope)
    }

    init {
        // Load all tracking list statuses
        viewModelScope.launch {
            userTrackingDao.observeAllTracking().collect { list ->
                _trackingMap.value = list.associate { it.anilistId to it.watchStatus }
                val ids = list.map { it.anilistId }
                _trackedAnime.value = animeRepository.getAnimeByIds(ids)
            }
        }

        val id = savedStateHandle.get<String>("collectionId") ?: ""
        if (id.isNotEmpty()) {
            loadCollection(id)
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val list = _animeList.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _animeList.value = list
        }
    }

    fun saveNewOrder() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _animeList.value
            val crossRefs = list.mapIndexed { index, anime ->
                CollectionAnimeCrossRef(
                    collectionId = collectionId,
                    animeId = anime.anilistId,
                    orderIndex = index,
                    isSynced = false,
                    isDeleted = false,
                    lastModified = System.currentTimeMillis()
                )
            }
            collectionDao.insertCrossRefs(crossRefs)
            launch {
                syncEngine.pushDirtyCollectionsOnly()
            }
        }
    }

    fun addAnimeToCollection(animeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxIndex = collectionDao.getMaxOrderIndex(collectionId) ?: -1
            val ref = CollectionAnimeCrossRef(
                collectionId = collectionId,
                animeId = animeId,
                orderIndex = maxIndex + 1,
                isSynced = false,
                isDeleted = false,
                lastModified = System.currentTimeMillis()
            )
            collectionDao.insertCrossRef(ref)
            launch {
                syncEngine.pushDirtyCollectionsOnly()
            }
        }
    }

    fun removeAnimeFromCollection(animeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            collectionDao.softDeleteCrossRef(collectionId, animeId)
            launch {
                syncEngine.pushDirtyCollectionsOnly()
            }
        }
    }

    fun deleteCollection() {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            collectionDao.softDeleteCollectionById(collectionId, timestamp)
            collectionDao.softDeleteCrossRefsForCollection(collectionId, timestamp)
            launch {
                syncEngine.pushDirtyCollectionsOnly()
            }
        }
    }

    fun updateCollectionDetails(title: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _collection.value ?: return@launch
            val updated = current.copy(
                title = title, 
                description = description,
                isSynced = false,
                isDeleted = false,
                lastModified = System.currentTimeMillis()
            )
            collectionDao.updateCollection(updated)
            launch {
                syncEngine.pushDirtyCollectionsOnly()
            }
        }
    }

    fun incrementChaosMeter() {
        viewModelScope.launch {
            userTrackingRepository.incrementChaosMeter()
        }
    }
}
