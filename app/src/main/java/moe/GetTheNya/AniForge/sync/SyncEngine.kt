package moe.GetTheNya.AniForge.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.util.AppLogger
import moe.GetTheNya.AniForge.core.network.AuthRepository
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class SyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userTrackingDao: UserTrackingDao,
    private val authRepository: AuthRepository,
    private val supabaseClient: SupabaseClient
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
                        triggerSync()
                    } else {
                        AppLogger.d(TAG, "Auth state emitted with same user ID $currentId. Skipping duplicate trigger.")
                    }
                } else {
                    lastUserId = null
                }
            }
        }
    }

    fun triggerSync() {
        AppLogger.d(TAG, "triggerSync() called")
        syncScope.launch {
            runSyncInternal()
        }
    }

    private suspend fun runSyncInternal() {
        val user = authRepository.currentSessionUser
        if (user == null) {
            AppLogger.d(TAG, "runSyncInternal: No current authenticated user session. Sync aborted.")
            return
        }
        val userId = user.id
        AppLogger.d(TAG, "runSyncInternal starting for user: $userId")

        syncMutex.withLock {
            try {
                val initialSyncKey = "initial_sync_done_$userId"
                val hasCompletedInitialSync = prefs.getBoolean(initialSyncKey, false)
                AppLogger.d(TAG, "runSyncInternal: hasCompletedInitialSync = $hasCompletedInitialSync")

                if (!hasCompletedInitialSync) {
                    AppLogger.d(TAG, "Starting initial full synchronization (two-way merge) for user: $userId")
                    performFullSync(userId)
                    prefs.edit().putBoolean(initialSyncKey, true).apply()
                    AppLogger.d(TAG, "Initial full synchronization completed successfully")
                }

                performIncrementalSync(userId)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Sync failed with exception: ${e.message}", e)
            }
        }
    }

    private suspend fun performFullSync(userId: String) {
        val remoteList = mutableListOf<SupabaseUserTrackingDto>()
        var page = 0
        var hasMore = true

        AppLogger.d(TAG, "performFullSync: Starting paginated remote fetching...")
        // Paginated remote fetching
        while (hasMore) {
            val start = page * PAGE_SIZE
            val end = start + PAGE_SIZE - 1
            AppLogger.d(TAG, "performFullSync: Fetching remote page $page (range $start to $end)")
            val pageItems = supabaseClient.from("user_tracking")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    range(start.toLong(), end.toLong())
                }
                .decodeList<SupabaseUserTrackingDto>()
            AppLogger.d(TAG, "performFullSync: Retrieved ${pageItems.size} items from remote on page $page")
            remoteList.addAll(pageItems)
            if (pageItems.size < PAGE_SIZE) {
                hasMore = false
            } else {
                page++
            }
        }

        AppLogger.d(TAG, "performFullSync: Total remote records downloaded = ${remoteList.size}")

        val localList = userTrackingDao.getAllTrackingIncludingDeleted()
        AppLogger.d(TAG, "performFullSync: Total local records fetched (including deleted) = ${localList.size}")
        
        val localMap = localList.associateBy { it.anilistId }
        val remoteMap = remoteList.associateBy { it.anilistId }

        val toInsertOrUpdate = mutableListOf<UserTrackingEntity>()
        val toDeleteLocally = mutableListOf<UserTrackingEntity>()

        // 1. Process remote items
        for (remoteItem in remoteList) {
            val localItem = localMap[remoteItem.anilistId]
            val remoteMilli = parseTimestamptz(remoteItem.lastModified)

            if (localItem == null) {
                // Remote-only record
                AppLogger.d(TAG, "performFullSync: Remote-only record found: anilistId=${remoteItem.anilistId}, watchStatus=${remoteItem.watchStatus}, isDeleted=${remoteItem.isDeleted}")
                if (!remoteItem.isDeleted) {
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
                }
            } else {
                // Mutual record: newest timestamp wins
                AppLogger.d(TAG, "performFullSync: Mutual record found: anilistId=${remoteItem.anilistId}. Remote lastModified=${remoteItem.lastModified} ($remoteMilli ms), Local lastModified=${localItem.lastModified} ms")
                if (remoteMilli > localItem.lastModified) {
                    AppLogger.d(TAG, "performFullSync: Remote record is newer for anilistId=${remoteItem.anilistId}. Remote isDeleted=${remoteItem.isDeleted}")
                    if (remoteItem.isDeleted) {
                        toDeleteLocally.add(localItem)
                    } else {
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
                    }
                } else {
                    AppLogger.d(TAG, "performFullSync: Local record is newer or equal for anilistId=${remoteItem.anilistId}. Keeping local, marking unsynced.")
                    // Local is newer or equal: keep local and mark isSynced = false so it gets pushed
                    toInsertOrUpdate.add(
                        localItem.copy(
                            isSynced = false
                        )
                    )
                }
            }
        }

        // 2. Process local-only items (not in remote list at all)
        for (localItem in localList) {
            if (!remoteMap.containsKey(localItem.anilistId)) {
                if (localItem.isSynced && !localItem.isDeleted) {
                    // Ghost Data: synced previously but missing from remote (indicates deletion elsewhere)
                    AppLogger.d(TAG, "performFullSync: Ghost Data found (local marked synced but missing on remote): anilistId=${localItem.anilistId}. Queueing physical deletion locally.")
                    toDeleteLocally.add(localItem)
                } else {
                    // Local-only, unsynced or soft deleted: keep locally and mark isSynced = false so it gets pushed
                    AppLogger.d(TAG, "performFullSync: Local-only dirty/deleted record found: anilistId=${localItem.anilistId}, isDeleted=${localItem.isDeleted}. Queueing local status reset to push.")
                    toInsertOrUpdate.add(
                        localItem.copy(
                            isSynced = false
                        )
                    )
                }
            }
        }

        // 3. Atomically apply changes
        AppLogger.d(TAG, "performFullSync: Merge processing complete. Queueing DB Transaction. toInsertOrUpdate count: ${toInsertOrUpdate.size}, toDeleteLocally count: ${toDeleteLocally.size}")
        if (toInsertOrUpdate.isNotEmpty() || toDeleteLocally.isNotEmpty()) {
            userTrackingDao.applyMergeResults(toInsertOrUpdate, toDeleteLocally)
            AppLogger.d(TAG, "performFullSync: DB Transaction committed successfully")
        } else {
            AppLogger.d(TAG, "performFullSync: No database changes required for merge.")
        }
    }

    private suspend fun performIncrementalSync(userId: String) {
        val unsynced = userTrackingDao.getUnsyncedTracking()
        if (unsynced.isEmpty()) {
            AppLogger.d(TAG, "performIncrementalSync: No unsynced (dirty) tracking records found. Skipping incremental sync.")
            return
        }

        AppLogger.d(TAG, "performIncrementalSync: Found ${unsynced.size} unsynced tracking records. Starting incremental batch sync...")

        // Separate active and soft-deleted records
        val activeUnsynced = unsynced.filter { !it.isDeleted }
        val deletedUnsynced = unsynced.filter { it.isDeleted }

        // 1. Process soft-deleted records (Upsert to remote with is_deleted = true)
        if (deletedUnsynced.isNotEmpty()) {
            AppLogger.d(TAG, "performIncrementalSync: Uploading ${deletedUnsynced.size} tombstone deletions to remote...")
            val deleteDtos = deletedUnsynced.map { entity ->
                SupabaseUserTrackingDto(
                    userId = userId,
                    anilistId = entity.anilistId,
                    watchStatus = entity.watchStatus,
                    episodeProgress = entity.episodeProgress,
                    score = entity.score,
                    notes = entity.notes,
                    lastModified = formatEpochMilliToTimestamptz(entity.lastModified),
                    isDeleted = true
                )
            }
            try {
                supabaseClient.from("user_tracking").upsert(deleteDtos)
                AppLogger.d(TAG, "performIncrementalSync: Remote deletion successful for ${deletedUnsynced.size} items. Proceeding with local SQLite physical deletions...")
                
                // Delete locally from SQLite only after successful server confirmation
                userTrackingDao.deleteBatch(deletedUnsynced)
                AppLogger.d(TAG, "performIncrementalSync: Successfully deleted ${deletedUnsynced.size} items from local SQLite.")
            } catch (e: Exception) {
                AppLogger.e(TAG, "performIncrementalSync: Failed to upload tombstones to remote: ${e.message}", e)
                throw e
            }
        }

        // 2. Process active dirty records (Batch Upsert to remote)
        if (activeUnsynced.isNotEmpty()) {
            AppLogger.d(TAG, "performIncrementalSync: Uploading ${activeUnsynced.size} active records to remote...")
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
                AppLogger.d(TAG, "performIncrementalSync: Remote upsert successful for ${activeUnsynced.size} items. Proceeding with atomic local state update...")

                // Atomic state update with concurrency check
                val ids = activeUnsynced.map { it.anilistId }
                val timestamps = activeUnsynced.map { it.lastModified }
                val rowsUpdated = userTrackingDao.markRecordsSynced(ids, timestamps)
                AppLogger.d(TAG, "performIncrementalSync: Successfully marked records synced locally. markRecordsSynced executed. IDs: $ids")
            } catch (e: Exception) {
                AppLogger.e(TAG, "performIncrementalSync: Failed to upload active records to remote: ${e.message}", e)
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
}
