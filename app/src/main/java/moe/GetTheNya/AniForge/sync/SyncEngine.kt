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
                val lastSyncTimeKey = "last_successful_sync_time_$userId"
                val lastSyncTime = prefs.getString(lastSyncTimeKey, null)
                val isDeltaSync = !lastSyncTime.isNullOrEmpty()
                AppLogger.d(TAG, "runSyncInternal: lastSyncTime = $lastSyncTime, isDeltaSync = $isDeltaSync")

                if (isDeltaSync) {
                    AppLogger.d(TAG, "Starting Delta Sync Mode for user: $userId")
                } else {
                    AppLogger.d(TAG, "Starting Initial Full Sync Mode for user: $userId")
                }

                performPullSync(userId, lastSyncTime)
                performPushSync(userId)

                val currentUtcTime = Instant.now().toString()
                prefs.edit().putString(lastSyncTimeKey, currentUtcTime).apply()
                AppLogger.d(TAG, "Sync cycle completed successfully. Saved last_successful_sync_time: $currentUtcTime")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Sync failed with exception: ${e.message}", e)
            }
        }
    }

    private suspend fun performPullSync(userId: String, lastSyncTime: String?) {
        val isDeltaSync = !lastSyncTime.isNullOrEmpty()
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
                            gt("last_modified", lastSyncTime!!)
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
}
