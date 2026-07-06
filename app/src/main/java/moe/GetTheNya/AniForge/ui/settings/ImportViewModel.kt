package moe.GetTheNya.AniForge.ui.settings

import moe.GetTheNya.AniForge.sync.SyncEngine
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.dao.CollectionDao
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.PendingImportDao
import moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.entity.PendingImportEntity
import moe.GetTheNya.AniForge.core.database.entity.TargetStatus
import moe.GetTheNya.AniForge.core.database.entity.ImportStatus
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository
import moe.GetTheNya.AniForge.ui.localization.LocalizationService
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import moe.GetTheNya.AniForge.core.network.api.AniListApiService
import moe.GetTheNya.AniForge.core.network.model.AniListGraphQLPayload
import javax.inject.Inject

enum class MatchPriority {
    ORIGINAL_TITLE,
    ALTERNATIVE_TITLE
}

data class PendingImportItem(
    val entity: PendingImportEntity,
    val matchedAnime: Anime? = null,
    val isResolving: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Anime> = emptyList(),
    val isSearching: Boolean = false
)

data class FailedImportState(
    val isResolving: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Anime> = emptyList(),
    val isSearching: Boolean = false
)

data class ImportState(
    val isProcessing: Boolean = false,
    val totalRecords: Int = 0,
    val processedSuccessCount: Int = 0,
    val failedCount: Int = 0,
    val showSummary: Boolean = false,
    val error: String? = null
)

data class PreFlightCollisionGroup(
    val targetAnimeTitle: String,
    val targetAnimeId: Long,
    val conflictingRows: List<PendingImportEntity>
)


@HiltViewModel
class ImportViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingRepository: UserTrackingRepository,
    private val userTrackingDao: UserTrackingDao,
    private val collectionDao: CollectionDao,
    private val pendingImportDao: PendingImportDao,
    private val localizationService: LocalizationService,
    private val aniListApiService: AniListApiService,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val _matchPriority = MutableStateFlow(MatchPriority.ORIGINAL_TITLE)
    val matchPriority: StateFlow<MatchPriority> = _matchPriority.asStateFlow()

    private val _syncStatus = MutableStateFlow(true)
    val syncStatus: StateFlow<Boolean> = _syncStatus.asStateFlow()

    private val _syncRating = MutableStateFlow(true)
    val syncRating: StateFlow<Boolean> = _syncRating.asStateFlow()

    private val _resolvingStates = MutableStateFlow<Map<String, FailedImportState>>(emptyMap())

    private val _ambiguousCandidates = MutableStateFlow<List<Anime>>(emptyList())
    val ambiguousCandidates: StateFlow<List<Anime>> = _ambiguousCandidates.asStateFlow()

    private val _isFetchingCandidates = MutableStateFlow(false)
    val isFetchingCandidates: StateFlow<Boolean> = _isFetchingCandidates.asStateFlow()

    private val _isCommitting = MutableStateFlow(false)
    val isCommitting: StateFlow<Boolean> = _isCommitting.asStateFlow()

    val preFlightCollisions = MutableStateFlow<List<PreFlightCollisionGroup>>(emptyList())


    val pendingImportsCount: StateFlow<Int> = pendingImportDao.observeCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val pendingImportItems: StateFlow<List<PendingImportItem>> = combine(
        pendingImportDao.observeAll(),
        _resolvingStates
    ) { entities, states ->
        val matchedIds = entities.mapNotNull { it.matchedAnimeId }.distinct()
        val animeMap = if (matchedIds.isNotEmpty()) {
            animeRepository.getAnimeByIds(matchedIds).associateBy { it.anilistId }
        } else {
            emptyMap()
        }
        entities.map { entity ->
            val stateKey = entity.id.toString()
            val state = states[stateKey] ?: FailedImportState()
            PendingImportItem(
                entity = entity,
                matchedAnime = animeMap[entity.matchedAnimeId],
                isResolving = state.isResolving,
                searchQuery = state.searchQuery,
                searchResults = state.searchResults,
                isSearching = state.isSearching
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setMatchPriority(priority: MatchPriority) {
        _matchPriority.value = priority
    }

    fun setSyncStatus(enabled: Boolean) {
        _syncStatus.value = enabled
    }

    fun setSyncRating(enabled: Boolean) {
        _syncRating.value = enabled
    }

    fun resumeImportResolution() {
        _state.value = _state.value.copy(
            showSummary = true,
            totalRecords = pendingImportsCount.value
        )
    }

    fun deletePendingImport(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pendingImportDao.deleteById(id)
            }
            val currentStates = _resolvingStates.value.toMutableMap()
            currentStates.remove(id.toString())
            _resolvingStates.value = currentStates
        }
    }

    fun startImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = ImportState(isProcessing = true)
            try {
                val csvText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    } ?: throw Exception("Failed to open input stream")
                }
                
                val rows = parseAnixartTxt(csvText)
                if (rows.isEmpty()) {
                    _state.value = ImportState(error = "TXT file is empty or invalid")
                    return@launch
                }

                _state.value = _state.value.copy(totalRecords = rows.size)

                withContext(Dispatchers.IO) {
                    // Clean previous pending imports
                    pendingImportDao.deleteAll()

                    val pendingEntities = rows.map { row ->
                        val russianTitle = row.getOrNull(1)?.trim() ?: ""
                        val rawOriginal = row.getOrNull(2)?.trim() ?: ""
                        val originalTitle = if (rawOriginal.equals("Не указано оригинальное название", ignoreCase = true)) "" else rawOriginal
                        val alternativeTitle = row.getOrNull(3)?.trim() ?: ""

                        val favoritesVal = row.getOrNull(4)?.trim() ?: ""
                        val statusVal = row.getOrNull(5)?.trim() ?: ""
                        val ratingVal = row.getOrNull(6)?.trim() ?: ""

                        val isFavorite = (favoritesVal == "В избранном")
                        val targetStatus = mapRussianStatusToEnum(statusVal)
                        val targetScore = if (ratingVal == "Не оценено") null else ratingVal.toDoubleOrNull()

                        PendingImportEntity(
                            rawRowText = org.json.JSONArray(row).toString(),
                            russianTitle = russianTitle,
                            originalTitle = originalTitle,
                            alternativeTitles = alternativeTitle,
                            targetStatus = targetStatus,
                            targetScore = targetScore,
                            isFavorite = isFavorite,
                            matchedAnimeId = null,
                            importStatus = ImportStatus.PENDING
                        )
                    }
                    pendingImportDao.insertAll(pendingEntities)
                }

                // Launch cascade matching engine
                runAutoMatching()

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = ImportState(error = e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    private fun runAutoMatching() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true)
            withContext(Dispatchers.IO) {
                val pendingList = pendingImportDao.getByStatus(ImportStatus.PENDING)
                var successCount = 0
                var failedCount = 0
                
                for (entity in pendingList) {
                    try {
                        val candidates = getMatchingCandidates(entity)
                        when {
                            candidates.size == 1 -> {
                                pendingImportDao.updateMatchedAnime(entity.id, candidates[0].anilistId, ImportStatus.SUCCESS)
                                successCount++
                            }
                            candidates.size > 1 -> {
                                pendingImportDao.updateMatchedAnime(entity.id, null, ImportStatus.AMBIGUOUS)
                                failedCount++
                            }
                            else -> {
                                pendingImportDao.updateMatchedAnime(entity.id, null, ImportStatus.NOT_FOUND)
                                failedCount++
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingImportDao.updateStatus(entity.id, ImportStatus.ERROR)
                        failedCount++
                    }

                    _state.value = _state.value.copy(
                        processedSuccessCount = successCount,
                        failedCount = failedCount
                    )
                }
            }
            _state.value = _state.value.copy(
                isProcessing = false,
                showSummary = true
            )
        }
    }

    suspend fun getMatchingCandidates(entity: PendingImportEntity): List<Anime> {
        var matches = animeRepository.findExactAnimeMatches(entity.originalTitle)
        if (matches.isEmpty() && entity.russianTitle.isNotEmpty()) {
            matches = animeRepository.findExactAnimeMatches(entity.russianTitle)
        }
        if (matches.isEmpty() && entity.alternativeTitles.isNotEmpty()) {
            val altList = entity.alternativeTitles.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val uniqueMatches = mutableListOf<Anime>()
            for (alt in altList) {
                val currentMatches = animeRepository.findExactAnimeMatches(alt)
                if (currentMatches.isNotEmpty()) {
                    uniqueMatches.addAll(currentMatches)
                }
            }
            matches = uniqueMatches.distinctBy { it.anilistId }
        }
        return matches
    }

    fun fetchCandidatesForAmbiguous(item: PendingImportEntity) {
        viewModelScope.launch {
            _isFetchingCandidates.value = true
            val candidates = getMatchingCandidates(item)
            _ambiguousCandidates.value = candidates
            _isFetchingCandidates.value = false
        }
    }

    fun clearCandidates() {
        _ambiguousCandidates.value = emptyList()
    }

    suspend fun searchAnimeCatalog(query: String): List<Anime> {
        return withContext(Dispatchers.IO) {
            animeRepository.queryAnime(
                SearchFilterQuery(textQuery = query, sortBy = SortOption.RELEVANCE)
            )
        }
    }

    fun resolveAmbiguous(id: Long, animeId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pendingImportDao.updateMatchedAnime(id, animeId, ImportStatus.RESOLVED)
            }
        }
    }

    suspend fun resolveWithAniList(originalTitle: String): Long? {
        val query = "query (\$search: String) {\n" +
                "  Media (search: \$search, type: ANIME) {\n" +
                "    id\n" +
                "    idMal\n" +
                "  }\n" +
                "}"

        val cleanTitle = originalTitle
            .replace(Regex("\\[.*?\\\\]"), "") // Remove [anything]
            .replace(Regex("\\(.*?\\)"), "") // Remove (anything)
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ") // Remove special characters
            .replace(Regex("\\s+"), " ") // Normalize spaces
            .trim()

        if (cleanTitle.isBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val response = aniListApiService.queryAniList(
                    AniListGraphQLPayload(
                        query = query,
                        variables = mapOf("search" to cleanTitle)
                    )
                )
                response.data?.Media?.id
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun toggleResolving(id: Long) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[id.toString()] ?: FailedImportState()
        val nextResolving = !state.isResolving

        val item = pendingImportItems.value.find { it.entity.id == id }
        val defaultQuery = ""

        currentStates[id.toString()] = state.copy(
            isResolving = nextResolving,
            searchQuery = defaultQuery,
            searchResults = emptyList()
        )
        _resolvingStates.value = currentStates

        if (nextResolving) {
            searchManual(id, defaultQuery)
        }
    }

    fun updateSearchQuery(id: Long, query: String) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[id.toString()] ?: FailedImportState()
        currentStates[id.toString()] = state.copy(searchQuery = query)
        _resolvingStates.value = currentStates

        searchManual(id, query)
    }

    private fun searchManual(id: Long, query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                updateItemSearchResults(id, emptyList())
                return@launch
            }
            updateItemSearching(id, true)
            val results = withContext(Dispatchers.IO) {
                animeRepository.queryAnime(
                    SearchFilterQuery(textQuery = query, sortBy = SortOption.RELEVANCE)
                )
            }
            updateItemSearchResults(id, results.take(10))
        }
    }

    private fun updateItemSearching(id: Long, isSearching: Boolean) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[id.toString()] ?: FailedImportState()
        currentStates[id.toString()] = state.copy(isSearching = isSearching)
        _resolvingStates.value = currentStates
    }

    private fun updateItemSearchResults(id: Long, results: List<Anime>) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[id.toString()] ?: FailedImportState()
        currentStates[id.toString()] = state.copy(searchResults = results, isSearching = false)
        _resolvingStates.value = currentStates
    }

    fun resolveManualBind(id: Long, animeId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                pendingImportDao.updateMatchedAnime(id, animeId, ImportStatus.RESOLVED)
            }
            val currentStates = _resolvingStates.value.toMutableMap()
            currentStates.remove(id.toString())
            _resolvingStates.value = currentStates
        }
    }

    suspend fun checkForPreFlightCollisions(): List<PreFlightCollisionGroup> {
        return withContext(Dispatchers.IO) {
            val readyList = pendingImportDao.getReadyToImport()
            val groups = readyList.groupBy { it.matchedAnimeId }
                .filter { it.key != null && it.value.size > 1 }

            val matchedIds = groups.keys.filterNotNull()
            val animeMap = if (matchedIds.isNotEmpty()) {
                animeRepository.getAnimeByIds(matchedIds).associateBy { it.anilistId }
            } else {
                emptyMap()
            }

            groups.map { (animeId, rows) ->
                val anime = animeMap[animeId]
                val title = anime?.getDisplayTitle() ?: "Unknown Anime"
                PreFlightCollisionGroup(
                    targetAnimeTitle = title,
                    targetAnimeId = animeId!!,
                    conflictingRows = rows
                )
            }
        }
    }

    fun runPreFlightCheck(onNoCollisions: () -> Unit) {
        viewModelScope.launch {
            val collisions = checkForPreFlightCollisions()
            if (collisions.isEmpty()) {
                onNoCollisions()
            } else {
                preFlightCollisions.value = collisions
            }
        }
    }

    fun clearPreFlightCollisions() {
        preFlightCollisions.value = emptyList()
    }

    suspend fun demoteCollisionsToConflicts() {
        withContext(Dispatchers.IO) {
            val ids = preFlightCollisions.value.flatMap { group ->
                group.conflictingRows.map { it.id }
            }
            if (ids.isNotEmpty()) {
                pendingImportDao.demoteToAmbiguous(ids)
            }
        }
        clearPreFlightCollisions()
    }


    fun commitImport(skipUnresolved: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isCommitting.value = true
            try {
                withContext(Dispatchers.IO) {
                    val readyList = pendingImportDao.getReadyToImport()
                    
                    val syncStatus = _syncStatus.value
                    val syncRating = _syncRating.value
                    
                    val strings = localizationService.activeLocaleStrings.value
                    val collectionTitle = strings.settingsScreen.anixartImportCollectionTitle
                    val collectionDesc = strings.settingsScreen.anixartImportCollectionDesc
                    
                    val matchedIds = readyList.mapNotNull { it.matchedAnimeId }
                    val animeList = animeRepository.getAnimeByIds(matchedIds)
                    val episodesMap = animeList.associate { it.anilistId to it.episodes }
                    
                    pendingImportDao.commitImportTransaction(
                        syncStatus = syncStatus,
                        syncRating = syncRating,
                        collectionTitle = collectionTitle,
                        collectionDesc = collectionDesc,
                        animeEpisodesMap = episodesMap
                    )
                    
                    userTrackingRepository.recalculateTotalWatchTime()
                    syncEngine.pushDirtyAnimeOnly()
                    syncEngine.pushDirtyCollectionsOnly()
                }
                
                _state.value = ImportState()
                _resolvingStates.value = emptyMap()
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(error = e.localizedMessage ?: "Failed to commit imports")
            } finally {
                _isCommitting.value = false
            }
        }
    }

    fun clearSummary() {
        _state.value = ImportState()
    }

    private fun mapRussianStatusToEnum(statusVal: String): TargetStatus {
        return when (statusVal) {
            "Смотрю" -> TargetStatus.CURRENT
            "В планах" -> TargetStatus.PLANNING
            "Просмотрено" -> TargetStatus.COMPLETED
            "Отложено" -> TargetStatus.PAUSED
            "Брошено" -> TargetStatus.DROPPED
            else -> TargetStatus.UNKNOWN
        }
    }

    private fun parseAnixartTxt(text: String): List<List<String>> {
        val cleanText = text.replace("\uFEFF", "")
        val lines = cleanText.split(Regex("""\r?\n|\r"""))
        
        val recordLines = mutableListOf<String>()
        var currentRecord: StringBuilder? = null
        val lineRegex = Regex("""^\d+\s+/""")
        
        for (line in lines) {
            if (lineRegex.containsMatchIn(line)) {
                if (currentRecord != null) {
                    recordLines.add(currentRecord.toString())
                }
                currentRecord = StringBuilder(line)
            } else {
                if (currentRecord != null) {
                    currentRecord.append("\n").append(line)
                } else {
                    currentRecord = StringBuilder(line)
                }
            }
        }
        if (currentRecord != null) {
            recordLines.add(currentRecord.toString())
        }

        val result = mutableListOf<List<String>>()
        for (record in recordLines) {
            val columns = splitRecord(record)
            if (columns.size < 7) continue
            
            val sanitizedCols = columns.mapIndexed { index, col ->
                var cleaned = col.trim()
                if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
                    cleaned = cleaned.substring(1, cleaned.length - 1).trim()
                }
                
                if (index == 3 && cleaned == "Не указаны альтернативные названия") {
                    cleaned = ""
                }
                cleaned
            }
            result.add(sanitizedCols)
        }
        return result
    }

    private fun splitRecord(record: String): List<String> {
        val result = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = record.length
        while (i < len) {
            val c = record[i]
            if (c == '"') {
                inQuotes = !inQuotes
                currentField.append(c)
            } else if (c == '/' && !inQuotes) {
                result.add(currentField.toString())
                currentField.clear()
            } else {
                currentField.append(c)
            }
            i++
        }
        result.add(currentField.toString())
        return result
    }
}
