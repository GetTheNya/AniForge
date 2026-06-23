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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.dao.CollectionDao
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
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

    fun setMatchPriority(priority: MatchPriority) {
        _matchPriority.value = priority
    }

    fun setSyncStatus(enabled: Boolean) {
        _syncStatus.value = enabled
    }

    fun setSyncRating(enabled: Boolean) {
        _syncRating.value = enabled
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

                    userTrackingRepository.recalculateTotalWatchTime()
                }

                _state.value = _state.value.copy(
                    isProcessing = false,
                    successfulImports = successList,
                    failedImports = failedList,
                    showSummary = true
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = ImportState(error = e.localizedMessage ?: "Unknown error occurred")
            }
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
            val collectionTitle = strings.settingsScreen.csvImportCollectionTitle
            val collectionDesc = strings.settingsScreen.csvImportCollectionDesc
            
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
        val currentImports = _state.value.failedImports.map { item ->
            if (item.id == failedImportId) {
                val nextResolving = !item.isResolving
                item.copy(
                    isResolving = nextResolving,
                    searchQuery = if (nextResolving) item.russianTitle.ifEmpty { item.originalTitle } else "",
                    searchResults = emptyList()
                ).apply {
                    if (nextResolving) {
                        searchManual(this)
                    }
                }
            } else {
                item
            }
        }
        _state.value = _state.value.copy(failedImports = currentImports)
    }

    fun updateSearchQuery(failedImportId: String, query: String) {
        val currentImports = _state.value.failedImports.map { item ->
            if (item.id == failedImportId) {
                item.copy(searchQuery = query).apply {
                    searchManual(this)
                }
            } else {
                item
            }
        }
        _state.value = _state.value.copy(failedImports = currentImports)
    }

    private fun searchManual(item: FailedImport) {
        viewModelScope.launch {
            val query = item.searchQuery
            if (query.isBlank()) {
                updateItemSearchResults(item.id, emptyList())
                return@launch
            }
            updateItemSearching(item.id, true)
            val results = withContext(Dispatchers.IO) {
                animeRepository.queryAnime(
                    SearchFilterQuery(textQuery = query, sortBy = SortOption.RELEVANCE)
                )
            }
            updateItemSearchResults(item.id, results.take(10))
        }
    }

    private fun updateItemSearching(id: String, isSearching: Boolean) {
        val currentImports = _state.value.failedImports.map { item ->
            if (item.id == id) {
                item.copy(isSearching = isSearching)
            } else {
                item
            }
        }
        _state.value = _state.value.copy(failedImports = currentImports)
    }

    private fun updateItemSearchResults(id: String, results: List<Anime>) {
        val currentImports = _state.value.failedImports.map { item ->
            if (item.id == id) {
                item.copy(searchResults = results, isSearching = false)
            } else {
                item
            }
        }
        _state.value = _state.value.copy(failedImports = currentImports)
    }

    fun resolveManualBind(failedImportId: String, anime: Anime) {
        viewModelScope.launch {
            val item = _state.value.failedImports.find { it.id == failedImportId } ?: return@launch
            
            withContext(Dispatchers.IO) {
                importRecordDirect(anime.anilistId, item.rawRow)
                userTrackingRepository.recalculateTotalWatchTime()
            }

            val updatedFailed = _state.value.failedImports.filter { it.id != failedImportId }
            val updatedSuccess = _state.value.successfulImports + SuccessfulImport(
                animeId = anime.anilistId,
                animeTitle = anime.titleUk ?: anime.titleRomaji,
                russianTitle = item.russianTitle,
                originalTitle = item.originalTitle,
                alternativeTitle = item.alternativeTitle
            )

            _state.value = _state.value.copy(
                failedImports = updatedFailed,
                successfulImports = updatedSuccess,
                processedSuccessCount = updatedSuccess.size,
                failedCount = updatedFailed.size
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
