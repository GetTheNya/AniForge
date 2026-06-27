package moe.GetTheNya.AniForge.ui.settings

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
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository
import moe.GetTheNya.AniForge.ui.localization.LocalizationService
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import javax.inject.Inject

enum class MatchPriority {
    ORIGINAL_TITLE,
    ALTERNATIVE_TITLE
}

data class SuccessfulImport(
    val animeId: Long,
    val animeTitle: String,
    val russianTitle: String,
    val originalTitle: String,
    val alternativeTitle: String
)

data class FailedImport(
    val id: String,
    val russianTitle: String,
    val originalTitle: String,
    val alternativeTitle: String,
    val rawRow: List<String>,
    val isResolving: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<Anime> = emptyList(),
    val isSearching: Boolean = false,
    val targetStatus: TargetStatus = TargetStatus.UNKNOWN,
    val isFavorite: Boolean = false
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
    val successfulImports: List<SuccessfulImport> = emptyList(),
    val failedImports: List<FailedImport> = emptyList(),
    val showSummary: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val animeRepository: AnimeRepository,
    private val userTrackingRepository: UserTrackingRepository,
    private val userTrackingDao: UserTrackingDao,
    private val collectionDao: CollectionDao,
    private val pendingImportDao: PendingImportDao,
    private val localizationService: LocalizationService
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

    val pendingImportsCount: StateFlow<Int> = pendingImportDao.observeCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val failedImports: StateFlow<List<FailedImport>> = combine(
        pendingImportDao.observeAll(),
        _resolvingStates
    ) { entities, states ->
        entities.map { entity ->
            val stateKey = entity.id.toString()
            val state = states[stateKey] ?: FailedImportState()
            val jsonArray = org.json.JSONArray(entity.rawRowText)
            val rawRow = List(jsonArray.length()) { jsonArray.getString(it) }
            FailedImport(
                id = stateKey,
                russianTitle = entity.russianTitle,
                originalTitle = entity.originalTitle,
                alternativeTitle = entity.alternativeTitles,
                rawRow = rawRow,
                isResolving = state.isResolving,
                searchQuery = state.searchQuery,
                searchResults = state.searchResults,
                isSearching = state.isSearching,
                targetStatus = entity.targetStatus,
                isFavorite = entity.isFavorite
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
            totalRecords = pendingImportsCount.value,
            failedCount = pendingImportsCount.value,
            processedSuccessCount = 0
        )
    }

    fun deletePendingImport(failedImportId: String) {
        viewModelScope.launch {
            val failedIdLong = failedImportId.toLongOrNull() ?: return@launch
            withContext(Dispatchers.IO) {
                pendingImportDao.deleteById(failedIdLong)
            }
            val currentStates = _resolvingStates.value.toMutableMap()
            currentStates.remove(failedImportId)
            _resolvingStates.value = currentStates

            // Mapped counts update
            val currentFailedCount = pendingImportsCount.value
            _state.value = _state.value.copy(
                failedCount = currentFailedCount
            )
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
                
                val rows = parseCsv(csvText)
                if (rows.isEmpty()) {
                    _state.value = ImportState(error = "CSV file is empty or invalid")
                    return@launch
                }

                _state.value = _state.value.copy(totalRecords = rows.size)

                val successList = mutableListOf<SuccessfulImport>()
                val failedList = mutableListOf<FailedImport>()

                withContext(Dispatchers.IO) {
                    // Clean previous pending imports
                    pendingImportDao.deleteAll()

                    for (row in rows) {
                        if (row.size < 7) continue // Skip invalid rows
                        
                        val russianTitle = row.getOrNull(1)?.trim() ?: ""
                        val originalTitle = row.getOrNull(2)?.trim() ?: ""
                        val alternativeTitle = row.getOrNull(3)?.trim() ?: ""

                        val matchedAnime = findBestMatch(
                            russianTitle = russianTitle,
                            originalTitle = originalTitle,
                            alternativeTitle = alternativeTitle,
                            priority = _matchPriority.value
                        )

                        if (matchedAnime != null) {
                            importRecordDirect(matchedAnime.anilistId, row)
                            successList.add(
                                SuccessfulImport(
                                    animeId = matchedAnime.anilistId,
                                    animeTitle = matchedAnime.titleUk ?: matchedAnime.titleRomaji,
                                    russianTitle = russianTitle,
                                    originalTitle = originalTitle,
                                    alternativeTitle = alternativeTitle
                                )
                            )
                        } else {
                            failedList.add(
                                FailedImport(
                                    id = java.util.UUID.randomUUID().toString(),
                                    russianTitle = russianTitle,
                                    originalTitle = originalTitle,
                                    alternativeTitle = alternativeTitle,
                                    rawRow = row
                                )
                            )
                        }

                        _state.value = _state.value.copy(
                            processedSuccessCount = successList.size,
                            failedCount = failedList.size
                        )
                    }

                    // Save failed list to DB
                    if (failedList.isNotEmpty()) {
                        val pendingEntities = failedList.map { failed ->
                            val favoritesVal = failed.rawRow.getOrNull(4)?.trim() ?: ""
                            val statusVal = failed.rawRow.getOrNull(5)?.trim() ?: ""
                            val ratingVal = failed.rawRow.getOrNull(6)?.trim() ?: ""

                            val isFavorite = (favoritesVal == "Добавлено")
                            val targetStatus = mapRussianStatusToEnum(statusVal)
                            val targetScore = if (ratingVal == "Не оценено") null else ratingVal.toDoubleOrNull()
                            
                            PendingImportEntity(
                                rawRowText = org.json.JSONArray(failed.rawRow).toString(),
                                russianTitle = failed.russianTitle,
                                originalTitle = failed.originalTitle,
                                alternativeTitles = failed.alternativeTitle,
                                targetStatus = targetStatus,
                                targetScore = targetScore,
                                isFavorite = isFavorite
                            )
                        }
                        pendingImportDao.insertAll(pendingEntities)
                    }

                    userTrackingRepository.recalculateTotalWatchTime()
                }

                _state.value = _state.value.copy(
                    isProcessing = false,
                    successfulImports = successList,
                    failedImports = emptyList(), // we keep failedImports empty in volatile state as they are observed from DB
                    showSummary = true
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = ImportState(error = e.localizedMessage ?: "Unknown error occurred")
            }
        }
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

    private suspend fun findBestMatch(
        russianTitle: String,
        originalTitle: String,
        alternativeTitle: String,
        priority: MatchPriority
    ): Anime? {
        val searchSequence = if (priority == MatchPriority.ORIGINAL_TITLE) {
            listOf(originalTitle, alternativeTitle, russianTitle)
        } else {
            listOf(alternativeTitle, originalTitle, russianTitle)
        }

        // 1. Exact Match Stage
        for (title in searchSequence) {
            if (title.isBlank()) continue
            val exact = animeRepository.findExactAnimeMatch(title)
            if (exact != null) return exact
        }

        // 2. FTS Match Stage
        for (title in searchSequence) {
            if (title.isBlank()) continue
            val ftsResults = animeRepository.queryAnime(
                SearchFilterQuery(textQuery = title, sortBy = SortOption.RELEVANCE)
            )
            val first = ftsResults.firstOrNull()
            if (first != null) return first
        }

        return null
    }

    private suspend fun importRecordDirect(anilistId: Long, row: List<String>) {
        val favoritesVal = row.getOrNull(4)?.trim() ?: ""
        val statusVal = row.getOrNull(5)?.trim() ?: ""
        val ratingVal = row.getOrNull(6)?.trim() ?: ""

        // 1. Favorites Handling
        if (favoritesVal == "Добавлено") {
            val strings = localizationService.activeLocaleStrings.value
            val collectionTitle = strings.settingsScreen.anixartImportCollectionTitle
            val collectionDesc = strings.settingsScreen.anixartImportCollectionDesc
            
            val collection = collectionDao.getCollectionByTitle(collectionTitle)
            val collectionId = if (collection != null) {
                collection.id
            } else {
                collectionDao.insertCollection(
                    CollectionEntity(
                        title = collectionTitle,
                        description = collectionDesc,
                        createdAt = System.currentTimeMillis()
                    )
                ).toInt()
            }

            val existingRefs = collectionDao.getCrossRefsForCollectionSync(collectionId)
            val isAlreadyLinked = existingRefs.any { it.animeId == anilistId }
            if (!isAlreadyLinked) {
                val maxIndex = existingRefs.maxOfOrNull { it.orderIndex } ?: -1
                collectionDao.insertCrossRef(
                    CollectionAnimeCrossRef(
                        collectionId = collectionId,
                        animeId = anilistId,
                        orderIndex = maxIndex + 1
                    )
                )
            }
        }

        // 2. Status & Score Mapping
        val syncStatus = _syncStatus.value
        val syncRating = _syncRating.value

        val mappedStatus = when (statusVal) {
            "Смотрю" -> "CURRENT"
            "В планах" -> "PLANNING"
            "Просмотрено" -> "COMPLETED"
            "Отложено" -> "PAUSED"
            "Брошено" -> "DROPPED"
            else -> ""
        }

        val mappedScore: Double? = if (ratingVal == "Не оценено") {
            null
        } else {
            ratingVal.toDoubleOrNull()
        }

        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val finalStatus = if (syncStatus) mappedStatus else (currentTracking?.watchStatus ?: "")
        val finalScore = if (syncRating) mappedScore else currentTracking?.score

        val progress = when {
            finalStatus == "COMPLETED" -> {
                val anime = animeRepository.getAnimeById(anilistId)
                anime?.episodes ?: (currentTracking?.episodeProgress ?: 0)
            }
            else -> currentTracking?.episodeProgress ?: 0
        }

        val shouldInsert = finalStatus.isNotEmpty() || finalScore != null || currentTracking != null
        if (shouldInsert) {
            val tracking = currentTracking?.copy(
                watchStatus = finalStatus,
                episodeProgress = progress,
                score = finalScore,
                lastModified = System.currentTimeMillis()
            ) ?: UserTrackingEntity(
                anilistId = anilistId,
                watchStatus = finalStatus,
                episodeProgress = progress,
                score = finalScore,
                notes = null,
                lastModified = System.currentTimeMillis()
            )
            userTrackingDao.insertOrUpdate(tracking)
        }
    }

    fun toggleResolving(failedImportId: String) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[failedImportId] ?: FailedImportState()
        val nextResolving = !state.isResolving

        val item = failedImports.value.find { it.id == failedImportId }
        val defaultQuery = if (nextResolving) {
            item?.russianTitle?.ifEmpty { item.originalTitle } ?: ""
        } else {
            ""
        }

        currentStates[failedImportId] = state.copy(
            isResolving = nextResolving,
            searchQuery = defaultQuery,
            searchResults = emptyList()
        )
        _resolvingStates.value = currentStates

        if (nextResolving) {
            searchManual(failedImportId, defaultQuery)
        }
    }

    fun updateSearchQuery(failedImportId: String, query: String) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[failedImportId] ?: FailedImportState()
        currentStates[failedImportId] = state.copy(searchQuery = query)
        _resolvingStates.value = currentStates

        searchManual(failedImportId, query)
    }

    private fun searchManual(failedImportId: String, query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                updateItemSearchResults(failedImportId, emptyList())
                return@launch
            }
            updateItemSearching(failedImportId, true)
            val results = withContext(Dispatchers.IO) {
                animeRepository.queryAnime(
                    SearchFilterQuery(textQuery = query, sortBy = SortOption.RELEVANCE)
                )
            }
            updateItemSearchResults(failedImportId, results.take(10))
        }
    }

    private fun updateItemSearching(id: String, isSearching: Boolean) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[id] ?: FailedImportState()
        currentStates[id] = state.copy(isSearching = isSearching)
        _resolvingStates.value = currentStates
    }

    private fun updateItemSearchResults(id: String, results: List<Anime>) {
        val currentStates = _resolvingStates.value.toMutableMap()
        val state = currentStates[id] ?: FailedImportState()
        currentStates[id] = state.copy(searchResults = results, isSearching = false)
        _resolvingStates.value = currentStates
    }

    fun resolveManualBind(failedImportId: String, anime: Anime) {
        viewModelScope.launch {
            val failedIdLong = failedImportId.toLongOrNull() ?: return@launch
            val item = failedImports.value.find { it.id == failedImportId } ?: return@launch
            
            withContext(Dispatchers.IO) {
                importRecordDirect(anime.anilistId, item.rawRow)
                pendingImportDao.deleteById(failedIdLong)
                userTrackingRepository.recalculateTotalWatchTime()
            }

            // Clean up resolving state for this item
            val currentStates = _resolvingStates.value.toMutableMap()
            currentStates.remove(failedImportId)
            _resolvingStates.value = currentStates

            val updatedSuccess = _state.value.successfulImports + SuccessfulImport(
                animeId = anime.anilistId,
                animeTitle = anime.titleUk ?: anime.titleRomaji,
                russianTitle = item.russianTitle,
                originalTitle = item.originalTitle,
                alternativeTitle = item.alternativeTitle
            )

            val currentFailedCount = pendingImportsCount.value
            _state.value = _state.value.copy(
                successfulImports = updatedSuccess,
                processedSuccessCount = updatedSuccess.size,
                failedCount = currentFailedCount
            )
        }
    }

    fun clearSummary() {
        _state.value = ImportState()
    }

    private fun parseCsv(text: String): List<List<String>> {
        val cleanText = text.replace("\uFEFF", "")
        val result = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        var currentField = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        val len = cleanText.length
        while (i < len) {
            val c = cleanText[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && cleanText[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                if (c == '"') {
                    inQuotes = true
                } else if (c == ',') {
                    currentRow.add(currentField.toString())
                    currentField = java.lang.StringBuilder()
                } else if (c == '\n') {
                    currentRow.add(currentField.toString())
                    currentField = java.lang.StringBuilder()
                    result.add(currentRow.toList())
                    currentRow.clear()
                } else if (c == '\r') {
                    if (i + 1 < len && cleanText[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString())
                    currentField = java.lang.StringBuilder()
                    result.add(currentRow.toList())
                    currentRow.clear()
                } else {
                    currentField.append(c)
                }
            }
            i++
        }
        if (currentRow.isNotEmpty() || currentField.isNotEmpty()) {
            currentRow.add(currentField.toString())
            result.add(currentRow.toList())
        }
        if (result.isNotEmpty()) {
            val firstCell = result[0].firstOrNull()?.trim()
            if (firstCell == "#" || firstCell == "№" || result[0].getOrNull(1)?.contains("название") == true) {
                return result.drop(1)
            }
        }
        return result
    }
}
