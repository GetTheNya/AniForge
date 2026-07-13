package moe.GetTheNya.AniForge.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.CollectionDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.core.network.AuthRepository
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import moe.GetTheNya.AniForge.ui.dashboard.UserTrackingRepository

@Serializable
data class SupabaseUserTrackingDto(
    @SerialName("user_id") val userId: String,
    @SerialName("anilist_id") val anilistId: Long,
    @SerialName("watch_status") val watchStatus: String,
    @SerialName("episode_progress") val episodeProgress: Int,
    @SerialName("score") val score: Double? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("last_modified") val lastModified: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class SupabaseCollectionDto(
    @SerialName("user_id") val userId: String,
    @SerialName("collection_id") val collectionId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_modified") val lastModified: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Serializable
data class SupabaseCollectionAnimeCrossRefDto(
    @SerialName("user_id") val userId: String,
    @SerialName("collection_id") val collectionId: String,
    @SerialName("anime_id") val animeId: Long,
    @SerialName("order_index") val orderIndex: Int,
    @SerialName("last_modified") val lastModified: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false
)

@Singleton
class SyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userTrackingDao: UserTrackingDao,
    private val collectionDao: CollectionDao,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient,
    private val userTrackingRepositoryProvider: Provider<UserTrackingRepository>
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SyncEngine"
        private const val PAGE_SIZE = 1000
    }

    private var lastUserId: String? = null

    init {
        syncScope.launch {
            authRepository.currentUser.collect { userInfo ->
                AppLogger.d(TAG, "authRepository.currentUser emitted: $userInfo")
                if (userInfo != null) {
                    val currentId = userInfo.id
                    if (currentId != lastUserId) {
                        AppLogger.d(TAG, "Auth state changed to logged in for user $currentId (previous: $lastUserId). Triggering sync.")
                        lastUserId = currentId
                        synchronizeAll()
                    } else {
                        AppLogger.d(TAG, "Auth state emitted with same user ID $currentId. Skipping duplicate trigger.")
                    }
                } else {
                    lastUserId = null
                }
            }
        }
    }

    suspend fun synchronizeAll() = withContext(NonCancellable) {
        val user = authRepository.currentSessionUser
        if (user == null) {
            AppLogger.d(TAG, "synchronizeAll: No current authenticated user session. Sync aborted.")
            return@withContext
        }
        val userId = user.id
        AppLogger.d(TAG, "synchronizeAll starting for user: $userId")

        syncMutex.withLock {
            try {
                val lastSyncTimeKey = "last_successful_sync_time_$userId"
                val lastSyncTime = prefs.getString(lastSyncTimeKey, null)
                val isDeltaSync = !lastSyncTime.isNullOrEmpty()
                AppLogger.d(TAG, "synchronizeAll: lastSyncTime = $lastSyncTime, isDeltaSync = $isDeltaSync")

                if (isDeltaSync) {
                    AppLogger.d(TAG, "Starting Delta Sync Mode for user: $userId")
                } else {
                    AppLogger.d(TAG, "Starting Initial Full Sync Mode for user: $userId")
                }

                performPullSync(userId, lastSyncTime)
                performPushSync(userId)

                performCollectionsSync(userId)

                val currentUtcTime = Instant.now().toString()
                prefs.edit().putString(lastSyncTimeKey, currentUtcTime).apply()
                AppLogger.d(TAG, "Sync cycle completed successfully. Saved last_successful_sync_time: $currentUtcTime")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Sync failed with exception: ${e.message}", e)
            }
        }
    }

    suspend fun pushDirtyAnimeOnly() = withContext(NonCancellable) {
        val user = authRepository.currentSessionUser
        if (user == null) {
            AppLogger.d(TAG, "pushDirtyAnimeOnly: No current authenticated user session. Push aborted.")
            return@withContext
        }
        val userId = user.id
        AppLogger.d(TAG, "pushDirtyAnimeOnly starting for user: $userId")

        syncMutex.withLock {
            try {
                performPushSync(userId)
            } catch (e: Exception) {
                AppLogger.w(TAG, "pushDirtyAnimeOnly failed silently: ${e.message}", e)
            }
        }
    }

    suspend fun pushDirtyCollectionsOnly() = withContext(NonCancellable) {
        val user = authRepository.currentSessionUser
        if (user == null) {
            AppLogger.d(TAG, "pushDirtyCollectionsOnly: No current authenticated user session. Push aborted.")
            return@withContext
        }
        val userId = user.id
        AppLogger.d(TAG, "pushDirtyCollectionsOnly starting for user: $userId")

        syncMutex.withLock {
            try {
                performCollectionsPushSync(userId)
            } catch (e: Exception) {
                AppLogger.w(TAG, "pushDirtyCollectionsOnly failed silently: ${e.message}", e)
            }
        }
    }

    private suspend fun performPullSync(userId: String, lastSyncTime: String?) {
        val isDeltaSync = !lastSyncTime.isNullOrEmpty()
        val querySyncTime = if (isDeltaSync) getBufferSyncTime(lastSyncTime!!) else ""
        val remoteList = mutableListOf<SupabaseUserTrackingDto>()
        var page = 0
        var hasMore = true

        AppLogger.d(TAG, "performPullSync: Starting remote fetching...")
        while (hasMore) {
            val start = page * PAGE_SIZE
            val end = start + PAGE_SIZE - 1
            AppLogger.d(TAG, "performPullSync: Fetching page $page (range $start to $end), delta=$isDeltaSync")
            val pageItems = supabaseClient.from("user_tracking")
                .select {
                    filter {
                        eq("user_id", userId)
                        if (isDeltaSync) {
                            gt("synced_at", querySyncTime)
                        }
                    }
                    range(start.toLong(), end.toLong())
                }
                .decodeList<SupabaseUserTrackingDto>()
            AppLogger.d(TAG, "performPullSync: Retrieved ${pageItems.size} items from remote on page $page")
            remoteList.addAll(pageItems)
            if (pageItems.size < PAGE_SIZE) {
                hasMore = false
            } else {
                page++
            }
        }

        AppLogger.d(TAG, "performPullSync: Total remote records fetched = ${remoteList.size}")

        val localList = userTrackingDao.getAllTrackingIncludingDeleted()
        AppLogger.d(TAG, "performPullSync: Total local records fetched (including deleted) = ${localList.size}")

        val localMap = localList.associateBy { it.anilistId }
        val remoteMap = remoteList.associateBy { it.anilistId }

        val toInsertOrUpdate = mutableListOf<UserTrackingEntity>()
        val toDeleteIds = mutableListOf<Long>()

        // 1. Process remote items
        for (remoteItem in remoteList) {
            val remoteMilli = parseTimestamptz(remoteItem.lastModified)
            val localItem = localMap[remoteItem.anilistId]

            if (isDeltaSync) {
                // A. During Delta Sync Mode (Standard Operation)
                if (remoteItem.isDeleted) {
                    // Remote Deletion Processing (Tombstones): physical delete
                    AppLogger.d(TAG, "performPullSync (Delta): Tombstone found for anilistId=${remoteItem.anilistId}. Queueing physical deletion.")
                    toDeleteIds.add(remoteItem.anilistId)
                } else {
                    // Normal Updates
                    if (localItem == null) {
                        AppLogger.d(TAG, "performPullSync (Delta): Remote-only record found: anilistId=${remoteItem.anilistId}. Queueing insertion.")
                        toInsertOrUpdate.add(
                            UserTrackingEntity(
                                anilistId = remoteItem.anilistId,
                                watchStatus = remoteItem.watchStatus,
                                episodeProgress = remoteItem.episodeProgress,
                                score = remoteItem.score,
                                notes = remoteItem.notes,
                                lastModified = remoteMilli,
                                isSynced = true,
                                isDeleted = false
                            )
                        )
                    } else {
                        // Standard timestamp comparison
                        AppLogger.d(TAG, "performPullSync (Delta): Mutual record found: anilistId=${remoteItem.anilistId}. Remote lastModified=${remoteItem.lastModified} ($remoteMilli ms), Local lastModified=${localItem.lastModified} ms")
                        if (remoteMilli > localItem.lastModified) {
                            AppLogger.d(TAG, "performPullSync (Delta): Remote record is newer. Queueing update.")
                            toInsertOrUpdate.add(
                                localItem.copy(
                                    watchStatus = remoteItem.watchStatus,
                                    episodeProgress = remoteItem.episodeProgress,
                                    score = remoteItem.score,
                                    notes = remoteItem.notes,
                                    lastModified = remoteMilli,
                                    isSynced = true,
                                    isDeleted = false
                                )
                            )
                        } else {
                            AppLogger.d(TAG, "performPullSync (Delta): Local record is newer or equal. Keeping local.")
                        }
                    }
                }
            } else {
                // B. During Initial Full Sync Mode (Only on First Login)
                if (remoteItem.isDeleted) {
                    if (localItem != null) {
                        AppLogger.d(TAG, "performPullSync (Full): Remote deleted record exists locally for anilistId=${remoteItem.anilistId}. Queueing physical deletion.")
                        toDeleteIds.add(remoteItem.anilistId)
                    }
                } else {
                    if (localItem == null) {
                        AppLogger.d(TAG, "performPullSync (Full): Remote-only record found: anilistId=${remoteItem.anilistId}. Queueing insertion.")
                        toInsertOrUpdate.add(
                            UserTrackingEntity(
                                anilistId = remoteItem.anilistId,
                                watchStatus = remoteItem.watchStatus,
                                episodeProgress = remoteItem.episodeProgress,
                                score = remoteItem.score,
                                notes = remoteItem.notes,
                                lastModified = remoteMilli,
                                isSynced = true,
                                isDeleted = false
                            )
                        )
                    } else {
                        AppLogger.d(TAG, "performPullSync (Full): Mutual record found: anilistId=${remoteItem.anilistId}. Remote lastModified=${remoteItem.lastModified} ($remoteMilli ms), Local lastModified=${localItem.lastModified} ms")
                        if (remoteMilli > localItem.lastModified) {
                            AppLogger.d(TAG, "performPullSync (Full): Remote record is newer. Queueing update.")
                            toInsertOrUpdate.add(
                                localItem.copy(
                                    watchStatus = remoteItem.watchStatus,
                                    episodeProgress = remoteItem.episodeProgress,
                                    score = remoteItem.score,
                                    notes = remoteItem.notes,
                                    lastModified = remoteMilli,
                                    isSynced = true,
                                    isDeleted = false
                                )
                            )
                        } else {
                            AppLogger.d(TAG, "performPullSync (Full): Local record is newer or equal. Keeping local, marking unsynced.")
                            toInsertOrUpdate.add(
                                localItem.copy(
                                    isSynced = false
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Process local-only items (not in remote list at all)
        if (!isDeltaSync) {
            // Ghost Data Cleanup: Only in Initial Full Sync Mode
            for (localItem in localList) {
                if (!remoteMap.containsKey(localItem.anilistId)) {
                    if (localItem.isSynced) {
                        // Ghost: synced previously but missing from full download
                        AppLogger.d(TAG, "performPullSync (Full): Ghost Data found (local marked synced but missing on remote): anilistId=${localItem.anilistId}. Queueing physical deletion.")
                        toDeleteIds.add(localItem.anilistId)
                    } else {
                        // Local-only dirty/deleted record: keep locally and mark unsynced to push
                        AppLogger.d(TAG, "performPullSync (Full): Local-only dirty record found: anilistId=${localItem.anilistId}. Keeping and marking unsynced.")
                        toInsertOrUpdate.add(
                            localItem.copy(
                                isSynced = false
                            )
                        )
                    }
                }
            }
        }

        // 3. Apply DB updates
        AppLogger.d(TAG, "performPullSync: Merge processing complete. toInsertOrUpdate count: ${toInsertOrUpdate.size}, toDeleteIds count: ${toDeleteIds.size}")
        if (toInsertOrUpdate.isNotEmpty() || toDeleteIds.isNotEmpty()) {
            userTrackingDao.applyMergeResults(toInsertOrUpdate = toInsertOrUpdate, toDeleteIds = toDeleteIds)
            AppLogger.d(TAG, "performPullSync: DB Transaction committed successfully")
            try {
                userTrackingRepositoryProvider.get().recalculateTotalWatchTime()
                AppLogger.d(TAG, "performPullSync: Recalculated watch timer after sync updates.")
            } catch (e: Exception) {
                AppLogger.e(TAG, "performPullSync: Failed to recalculate watch timer after sync updates", e)
            }
        } else {
            AppLogger.d(TAG, "performPullSync: No database changes required for pull.")
        }
    }

    private suspend fun performPushSync(userId: String) {
        val unsynced = userTrackingDao.getUnsyncedTracking()
        if (unsynced.isEmpty()) {
            AppLogger.d(TAG, "performPushSync: No unsynced (dirty) tracking records found. Skipping push.")
            return
        }

        AppLogger.d(TAG, "performPushSync: Found ${unsynced.size} unsynced tracking records. Starting push phase...")

        // Separate active and soft-deleted records
        val activeUnsynced = unsynced.filter { !it.isDeleted }
        val deletedUnsynced = unsynced.filter { it.isDeleted }

        // 1. Process soft-deleted records (Upsert to remote with is_deleted = true and updated last_modified)
        if (deletedUnsynced.isNotEmpty()) {
            AppLogger.d(TAG, "performPushSync: Uploading ${deletedUnsynced.size} tombstone deletions to remote...")
            val currentPushTime = Instant.now().toString()
            val deleteDtos = deletedUnsynced.map { entity ->
                SupabaseUserTrackingDto(
                    userId = userId,
                    anilistId = entity.anilistId,
                    watchStatus = entity.watchStatus,
                    episodeProgress = entity.episodeProgress,
                    score = entity.score,
                    notes = entity.notes,
                    lastModified = currentPushTime,
                    isDeleted = true
                )
            }
            try {
                supabaseClient.from("user_tracking").upsert(deleteDtos)
                AppLogger.d(TAG, "performPushSync: Remote deletion successful. Proceeding with local physical eviction...")

                // Evict locally: physical delete in SQLite
                userTrackingDao.deleteBatch(deletedUnsynced)
                AppLogger.d(TAG, "performPushSync: Successfully physically deleted ${deletedUnsynced.size} items from local SQLite.")
            } catch (e: Exception) {
                AppLogger.e(TAG, "performPushSync: Failed to upload tombstones to remote: ${e.message}", e)
                throw e
            }
        }

        // 2. Process active dirty records (Batch Upsert to remote)
        if (activeUnsynced.isNotEmpty()) {
            AppLogger.d(TAG, "performPushSync: Uploading ${activeUnsynced.size} active records to remote...")
            val dtos = activeUnsynced.map { entity ->
                SupabaseUserTrackingDto(
                    userId = userId,
                    anilistId = entity.anilistId,
                    watchStatus = entity.watchStatus,
                    episodeProgress = entity.episodeProgress,
                    score = entity.score,
                    notes = entity.notes,
                    lastModified = formatEpochMilliToTimestamptz(entity.lastModified),
                    isDeleted = false
                )
            }
            try {
                supabaseClient.from("user_tracking").upsert(dtos)
                AppLogger.d(TAG, "performPushSync: Remote upsert successful. Marking local records synced...")

                val ids = activeUnsynced.map { it.anilistId }
                val timestamps = activeUnsynced.map { it.lastModified }
                userTrackingDao.markRecordsSynced(ids, timestamps)
                AppLogger.d(TAG, "performPushSync: Successfully marked active records synced locally.")
            } catch (e: Exception) {
                AppLogger.e(TAG, "performPushSync: Failed to upload active records to remote: ${e.message}", e)
                throw e
            }
        }
    }

    private fun parseTimestamptz(timestamptz: String): Long {
        return try {
            Instant.parse(timestamptz).toEpochMilli()
        } catch (e: Exception) {
            try {
                OffsetDateTime.parse(timestamptz).toInstant().toEpochMilli()
            } catch (ex: Exception) {
                AppLogger.w(TAG, "parseTimestamptz: Failed to parse timestamp '$timestamptz', falling back to 0")
                0L
            }
        }
    }

    private fun formatEpochMilliToTimestamptz(epochMilli: Long): String {
        return Instant.ofEpochMilli(epochMilli).toString()
    }

    private fun getBufferSyncTime(lastSyncTime: String): String {
        return try {
            Instant.parse(lastSyncTime).minusSeconds(120).toString()
        } catch (e: Exception) {
            lastSyncTime
        }
    }

    private suspend fun performCollectionsSync(userId: String) {
        val lastSyncTimeKey = "last_successful_collections_sync_time_$userId"
        val lastSyncTime = prefs.getString(lastSyncTimeKey, null)
        val isDeltaSync = !lastSyncTime.isNullOrEmpty()
        AppLogger.d(TAG, "performCollectionsSync starting: lastSyncTime = $lastSyncTime, isDeltaSync = $isDeltaSync")

        if (isDeltaSync) {
            AppLogger.d(TAG, "Starting Collections Delta Sync Mode for user: $userId")
        } else {
            AppLogger.d(TAG, "Starting Collections Initial Full Sync Mode for user: $userId")
        }

        // Pull Order: save collections first, then cross-references to satisfy Foreign Key constraints
        performCollectionsPullSync(userId, lastSyncTime)

        // Push Order: upload collections first, then cross-references
        performCollectionsPushSync(userId)

        val currentUtcTime = Instant.now().toString()
        prefs.edit().putString(lastSyncTimeKey, currentUtcTime).apply()
        AppLogger.d(TAG, "Collections sync completed successfully. Saved collections sync time: $currentUtcTime")
    }

    private suspend fun performCollectionsPullSync(userId: String, lastSyncTime: String?) {
        val isDeltaSync = !lastSyncTime.isNullOrEmpty()
        val querySyncTime = if (isDeltaSync) getBufferSyncTime(lastSyncTime!!) else ""

        // 1. Pull Collections from remote
        val remoteCollections = mutableListOf<SupabaseCollectionDto>()
        var page = 0
        var hasMore = true
        while (hasMore) {
            val start = page * PAGE_SIZE
            val end = start + PAGE_SIZE - 1
            val pageItems = supabaseClient.from("collections")
                .select {
                    filter {
                        eq("user_id", userId)
                        if (isDeltaSync) {
                            gt("synced_at", querySyncTime)
                        }
                    }
                    range(start.toLong(), end.toLong())
                }
                .decodeList<SupabaseCollectionDto>()
            remoteCollections.addAll(pageItems)
            if (pageItems.size < PAGE_SIZE) {
                hasMore = false
            } else {
                page++
            }
        }
        AppLogger.d(TAG, "performCollectionsPullSync: Fetched ${remoteCollections.size} collections from remote.")

        // 2. Pull Cross-references from remote
        val remoteCrossRefs = mutableListOf<SupabaseCollectionAnimeCrossRefDto>()
        page = 0
        hasMore = true
        while (hasMore) {
            val start = page * PAGE_SIZE
            val end = start + PAGE_SIZE - 1
            val pageItems = supabaseClient.from("collection_anime_cross_ref")
                .select {
                    filter {
                        eq("user_id", userId)
                        if (isDeltaSync) {
                            gt("synced_at", querySyncTime)
                        }
                    }
                    range(start.toLong(), end.toLong())
                }
                .decodeList<SupabaseCollectionAnimeCrossRefDto>()
            remoteCrossRefs.addAll(pageItems)
            if (pageItems.size < PAGE_SIZE) {
                hasMore = false
            } else {
                page++
            }
        }
        AppLogger.d(TAG, "performCollectionsPullSync: Fetched ${remoteCrossRefs.size} cross references from remote.")

        // 3. Process Collections pull (Parent Table)
        val localCollections = collectionDao.getAllCollectionsIncludingDeleted()
        val localCollMap = localCollections.associateBy { it.id }
        val remoteCollMap = remoteCollections.associateBy { it.collectionId }

        val toInsertOrUpdateCollections = mutableListOf<CollectionEntity>()
        val toDeleteCollections = mutableListOf<CollectionEntity>()

        for (remoteItem in remoteCollections) {
            val remoteMilli = parseTimestamptz(remoteItem.lastModified)
            val localItem = localCollMap[remoteItem.collectionId]

            if (isDeltaSync) {
                if (remoteItem.isDeleted) {
                    if (localItem != null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Delta): Tombstone found for collection ID=${remoteItem.collectionId}. Queueing physical deletion.")
                        toDeleteCollections.add(localItem)
                    }
                } else {
                    if (localItem == null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Delta): Remote-only collection found: ID=${remoteItem.collectionId}. Queueing insertion.")
                        toInsertOrUpdateCollections.add(
                            CollectionEntity(
                                id = remoteItem.collectionId,
                                title = remoteItem.title,
                                description = remoteItem.description,
                                createdAt = parseTimestamptz(remoteItem.createdAt),
                                isSynced = true,
                                isDeleted = false,
                                lastModified = remoteMilli
                            )
                        )
                    } else {
                        if (remoteMilli > localItem.lastModified) {
                            AppLogger.d(TAG, "performCollectionsPullSync (Delta): Remote collection is newer for ID=${remoteItem.collectionId}. Queueing update.")
                            toInsertOrUpdateCollections.add(
                                localItem.copy(
                                    title = remoteItem.title,
                                    description = remoteItem.description,
                                    lastModified = remoteMilli,
                                    isSynced = true,
                                    isDeleted = false
                                )
                            )
                        } else {
                            AppLogger.d(TAG, "performCollectionsPullSync (Delta): Local collection is newer/equal for ID=${remoteItem.collectionId}. Keeping local.")
                        }
                    }
                }
            } else {
                // Initial Sync Mode
                if (remoteItem.isDeleted) {
                    if (localItem != null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Full): Remote deleted collection exists locally for ID=${remoteItem.collectionId}. Queueing physical deletion.")
                        toDeleteCollections.add(localItem)
                    }
                } else {
                    if (localItem == null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Full): Remote-only collection: ID=${remoteItem.collectionId}. Queueing insertion.")
                        toInsertOrUpdateCollections.add(
                            CollectionEntity(
                                id = remoteItem.collectionId,
                                title = remoteItem.title,
                                description = remoteItem.description,
                                createdAt = parseTimestamptz(remoteItem.createdAt),
                                isSynced = true,
                                isDeleted = false,
                                lastModified = remoteMilli
                            )
                        )
                    } else {
                        if (remoteMilli > localItem.lastModified) {
                            toInsertOrUpdateCollections.add(
                                localItem.copy(
                                    title = remoteItem.title,
                                    description = remoteItem.description,
                                    lastModified = remoteMilli,
                                    isSynced = true,
                                    isDeleted = false
                                )
                            )
                        } else {
                            toInsertOrUpdateCollections.add(
                                localItem.copy(isSynced = false)
                            )
                        }
                    }
                }
            }
        }

        if (!isDeltaSync) {
            // Ghost Cleanup
            for (localItem in localCollections) {
                if (!remoteCollMap.containsKey(localItem.id)) {
                    if (localItem.isSynced) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Full): Ghost collection found (missing on remote): ID=${localItem.id}. Queueing physical deletion.")
                        toDeleteCollections.add(localItem)
                    } else {
                        toInsertOrUpdateCollections.add(
                            localItem.copy(isSynced = false)
                        )
                    }
                }
            }
        }

        // Apply Collections pulls (First in Pull Order)
        if (toInsertOrUpdateCollections.isNotEmpty() || toDeleteCollections.isNotEmpty()) {
            collectionDao.applyCollectionsMerge(toInsertOrUpdateCollections, toDeleteCollections)
            AppLogger.d(TAG, "performCollectionsPullSync: Applied collections merge. Insert/update count = ${toInsertOrUpdateCollections.size}, delete count = ${toDeleteCollections.size}")
        }

        // 4. Process Cross-references pull (Child Table)
        val localCrossRefs = collectionDao.getAllCrossRefsIncludingDeleted()
        val localCrossRefMap = localCrossRefs.associateBy { it.collectionId to it.animeId }
        val remoteCrossRefMap = remoteCrossRefs.associateBy { it.collectionId to it.animeId }

        val toInsertOrUpdateCrossRefs = mutableListOf<CollectionAnimeCrossRef>()
        val toDeleteCrossRefs = mutableListOf<CollectionAnimeCrossRef>()

        for (remoteItem in remoteCrossRefs) {
            val remoteMilli = parseTimestamptz(remoteItem.lastModified)
            val key = remoteItem.collectionId to remoteItem.animeId
            val localItem = localCrossRefMap[key]

            if (isDeltaSync) {
                if (remoteItem.isDeleted) {
                    if (localItem != null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Delta): Tombstone found for cross-ref CollectionId=${remoteItem.collectionId}, AnimeId=${remoteItem.animeId}. Queueing physical deletion.")
                        toDeleteCrossRefs.add(localItem)
                    }
                } else {
                    if (localItem == null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Delta): Remote-only cross-ref: CollectionId=${remoteItem.collectionId}, AnimeId=${remoteItem.animeId}. Queueing insertion.")
                        toInsertOrUpdateCrossRefs.add(
                            CollectionAnimeCrossRef(
                                collectionId = remoteItem.collectionId,
                                animeId = remoteItem.animeId,
                                orderIndex = remoteItem.orderIndex,
                                isSynced = true,
                                isDeleted = false,
                                lastModified = remoteMilli
                            )
                        )
                    } else {
                        if (remoteMilli > localItem.lastModified) {
                            AppLogger.d(TAG, "performCollectionsPullSync (Delta): Remote cross-ref is newer. Queueing update.")
                            toInsertOrUpdateCrossRefs.add(
                                localItem.copy(
                                    orderIndex = remoteItem.orderIndex,
                                    lastModified = remoteMilli,
                                    isSynced = true,
                                    isDeleted = false
                                )
                            )
                        } else {
                            AppLogger.d(TAG, "performCollectionsPullSync (Delta): Local cross-ref is newer/equal. Keeping local.")
                        }
                    }
                }
            } else {
                // Initial Sync Mode
                if (remoteItem.isDeleted) {
                    if (localItem != null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Full): Remote deleted cross-ref exists locally. Queueing physical deletion.")
                        toDeleteCrossRefs.add(localItem)
                    }
                } else {
                    if (localItem == null) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Full): Remote-only cross-ref: Queueing insertion.")
                        toInsertOrUpdateCrossRefs.add(
                            CollectionAnimeCrossRef(
                                collectionId = remoteItem.collectionId,
                                animeId = remoteItem.animeId,
                                orderIndex = remoteItem.orderIndex,
                                isSynced = true,
                                isDeleted = false,
                                lastModified = remoteMilli
                            )
                        )
                    } else {
                        if (remoteMilli > localItem.lastModified) {
                            toInsertOrUpdateCrossRefs.add(
                                localItem.copy(
                                    orderIndex = remoteItem.orderIndex,
                                    lastModified = remoteMilli,
                                    isSynced = true,
                                    isDeleted = false
                                )
                            )
                        } else {
                            toInsertOrUpdateCrossRefs.add(
                                localItem.copy(isSynced = false)
                            )
                        }
                    }
                }
            }
        }

        if (!isDeltaSync) {
            // Ghost Cleanup
            for (localItem in localCrossRefs) {
                val key = localItem.collectionId to localItem.animeId
                if (!remoteCrossRefMap.containsKey(key)) {
                    if (localItem.isSynced) {
                        AppLogger.d(TAG, "performCollectionsPullSync (Full): Ghost cross-ref found (missing on remote): CollectionId=${localItem.collectionId}, AnimeId=${localItem.animeId}. Queueing physical deletion.")
                        toDeleteCrossRefs.add(localItem)
                    } else {
                        toInsertOrUpdateCrossRefs.add(
                            localItem.copy(isSynced = false)
                        )
                    }
                }
            }
        }

        // Apply Cross-references pulls (Second in Pull Order)
        if (toInsertOrUpdateCrossRefs.isNotEmpty() || toDeleteCrossRefs.isNotEmpty()) {
            collectionDao.applyCrossRefsMerge(toInsertOrUpdateCrossRefs, toDeleteCrossRefs)
            AppLogger.d(TAG, "performCollectionsPullSync: Applied cross-references merge. Insert/update count = ${toInsertOrUpdateCrossRefs.size}, delete count = ${toDeleteCrossRefs.size}")
        }
    }

    private suspend fun performCollectionsPushSync(userId: String) {
        // --- 1. Push Collections ---
        val unsyncedCollections = collectionDao.getUnsyncedCollections()
        if (unsyncedCollections.isNotEmpty()) {
            AppLogger.d(TAG, "performCollectionsPushSync: Found ${unsyncedCollections.size} unsynced collections.")
            val activeCollections = unsyncedCollections.filter { !it.isDeleted }
            val deletedCollections = unsyncedCollections.filter { it.isDeleted }

            // A. Process soft-deleted collections (Upsert to remote with is_deleted = true)
            if (deletedCollections.isNotEmpty()) {
                AppLogger.d(TAG, "performCollectionsPushSync: Uploading ${deletedCollections.size} deleted collection tombstones...")
                val currentPushTime = Instant.now().toString()
                val deleteDtos = deletedCollections.map { entity ->
                    SupabaseCollectionDto(
                        userId = userId,
                        collectionId = entity.id,
                        title = entity.title,
                        description = entity.description,
                        createdAt = formatEpochMilliToTimestamptz(entity.createdAt),
                        lastModified = currentPushTime,
                        isDeleted = true
                    )
                }
                try {
                    supabaseClient.from("collections").upsert(deleteDtos)
                    // Evict physically locally
                    collectionDao.deleteCollectionsBatch(deletedCollections)
                    AppLogger.d(TAG, "performCollectionsPushSync: Successfully deleted ${deletedCollections.size} collections from local DB.")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "performCollectionsPushSync: Failed to upload deleted collections: ${e.message}", e)
                    throw e
                }
            }

            // B. Process active collections (Upsert to remote)
            if (activeCollections.isNotEmpty()) {
                AppLogger.d(TAG, "performCollectionsPushSync: Uploading ${activeCollections.size} active collections...")
                val activeDtos = activeCollections.map { entity ->
                    SupabaseCollectionDto(
                        userId = userId,
                        collectionId = entity.id,
                        title = entity.title,
                        description = entity.description,
                        createdAt = formatEpochMilliToTimestamptz(entity.createdAt),
                        lastModified = formatEpochMilliToTimestamptz(entity.lastModified),
                        isDeleted = false
                    )
                }
                try {
                    supabaseClient.from("collections").upsert(activeDtos)
                    val ids = activeCollections.map { it.id }
                    val timestamps = activeCollections.map { it.lastModified }
                    collectionDao.markCollectionsSynced(ids, timestamps)
                    AppLogger.d(TAG, "performCollectionsPushSync: Successfully marked active collections synced.")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "performCollectionsPushSync: Failed to upload active collections: ${e.message}", e)
                    throw e
                }
            }
        }

        // --- 2. Push Cross-references ---
        val unsyncedCrossRefs = collectionDao.getUnsyncedCrossRefs()
        if (unsyncedCrossRefs.isNotEmpty()) {
            AppLogger.d(TAG, "performCollectionsPushSync: Found ${unsyncedCrossRefs.size} unsynced cross-references.")
            val activeCrossRefs = unsyncedCrossRefs.filter { !it.isDeleted }
            val deletedCrossRefs = unsyncedCrossRefs.filter { it.isDeleted }

            // A. Process soft-deleted cross-references
            if (deletedCrossRefs.isNotEmpty()) {
                AppLogger.d(TAG, "performCollectionsPushSync: Uploading ${deletedCrossRefs.size} deleted cross-reference tombstones...")
                val currentPushTime = Instant.now().toString()
                val deleteDtos = deletedCrossRefs.map { entity ->
                    SupabaseCollectionAnimeCrossRefDto(
                        userId = userId,
                        collectionId = entity.collectionId,
                        animeId = entity.animeId,
                        orderIndex = entity.orderIndex,
                        lastModified = currentPushTime,
                        isDeleted = true
                    )
                }
                try {
                    supabaseClient.from("collection_anime_cross_ref").upsert(deleteDtos)
                    collectionDao.deleteCrossRefsBatch(deletedCrossRefs)
                    AppLogger.d(TAG, "performCollectionsPushSync: Successfully deleted ${deletedCrossRefs.size} cross-references from local DB.")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "performCollectionsPushSync: Failed to upload deleted cross-references: ${e.message}", e)
                    throw e
                }
            }

            // B. Process active cross-references
            if (activeCrossRefs.isNotEmpty()) {
                AppLogger.d(TAG, "performCollectionsPushSync: Uploading ${activeCrossRefs.size} active cross-references...")
                val activeDtos = activeCrossRefs.map { entity ->
                    SupabaseCollectionAnimeCrossRefDto(
                        userId = userId,
                        collectionId = entity.collectionId,
                        animeId = entity.animeId,
                        orderIndex = entity.orderIndex,
                        lastModified = formatEpochMilliToTimestamptz(entity.lastModified),
                        isDeleted = false
                    )
                }
                try {
                    supabaseClient.from("collection_anime_cross_ref").upsert(activeDtos)
                    val collectionIds = activeCrossRefs.map { it.collectionId }
                    val animeIds = activeCrossRefs.map { it.animeId }
                    val timestamps = activeCrossRefs.map { it.lastModified }
                    collectionDao.markCrossRefsSynced(collectionIds, animeIds, timestamps)
                    AppLogger.d(TAG, "performCollectionsPushSync: Successfully marked active cross-references synced.")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "performCollectionsPushSync: Failed to upload active cross-references: ${e.message}", e)
                    throw e
                }
            }
        }
    }
}
