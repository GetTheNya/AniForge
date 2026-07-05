package moe.GetTheNya.AniForge.ui.dashboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.UserStatsDao
import moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.sync.SyncEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTrackingRepository @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider,
    private val userStatsDao: UserStatsDao,
    private val syncEngine: SyncEngine
) {
    val gestureCenter: Flow<QuickGestureAction> = settingsProvider.gestureCenterStr
        .map { QuickGestureAction.fromString(it) }

    val gestureUp: Flow<QuickGestureAction> = settingsProvider.gestureUpStr
        .map { QuickGestureAction.fromString(it) }

    val gestureDown: Flow<QuickGestureAction> = settingsProvider.gestureDownStr
        .map { QuickGestureAction.fromString(it) }

    val gestureLeft: Flow<QuickGestureAction> = settingsProvider.gestureLeftStr
        .map { QuickGestureAction.fromString(it) }

    val gestureRight: Flow<QuickGestureAction> = settingsProvider.gestureRightStr
        .map { QuickGestureAction.fromString(it) }

    fun triggerSync() {
        syncEngine.triggerSync()
    }

    suspend fun incrementChaosMeter() = withContext(Dispatchers.IO) {
        userStatsDao.incrementChaosMeter(1)
    }

    suspend fun updateWatchStatus(anilistId: Long, status: String) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val anime = animeRepository.getAnimeById(anilistId)
        val duration = (anime?.duration ?: 0).toLong()

        // Hard-block: unreleased anime can only be PLANNING. Any other status
        // transition is physically impossible and is silently rejected.
        if (anime?.isNotYetReleased() == true && status != "PLANNING") {
            return@withContext
        }

        if (currentTracking != null && currentTracking.watchStatus == status) {
            // Toggle off: clicking the already-active status soft-deletes the tracking entry.
            val oldProgress = currentTracking.episodeProgress
            val watchTimeDelta = -oldProgress.toLong() * duration
            if (watchTimeDelta != 0L) {
                userStatsDao.incrementWatchTime(watchTimeDelta)
            }

            val softDeleted = currentTracking.copy(
                watchStatus = "",
                episodeProgress = 0,
                lastModified = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = true
            )
            userTrackingDao.insertOrUpdate(softDeleted)
            syncEngine.triggerSync()
        } else {
            // Sync-lag override: when the user manually marks a still-RELEASING
            // anime as COMPLETED, trust their intent and auto-bump episode progress
            // to the total planned count (or to the max known aired count as fallback).
            val progress = when {
                status == "COMPLETED" && anime?.isReleasing() == true -> {
                    // Prefer total planned episodes; fall back to max currently aired.
                    anime.episodes ?: anime.getMaxAllowedIncrement().takeIf { it != Int.MAX_VALUE } ?: (currentTracking?.episodeProgress ?: 0)
                }
                status == "COMPLETED" -> {
                    anime?.episodes ?: (currentTracking?.episodeProgress ?: 0)
                }
                else -> currentTracking?.episodeProgress ?: 0
            }

            val oldProgress = currentTracking?.episodeProgress ?: 0
            val deltaEpisodes = progress - oldProgress
            if (deltaEpisodes != 0) {
                val watchTimeDelta = deltaEpisodes.toLong() * duration
                userStatsDao.incrementWatchTime(watchTimeDelta)
            }

            val updated = currentTracking?.copy(
                watchStatus = status,
                episodeProgress = progress,
                lastModified = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false
            ) ?: UserTrackingEntity(
                anilistId = anilistId,
                watchStatus = status,
                episodeProgress = progress,
                score = null,
                notes = null,
                lastModified = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false
            )
            userTrackingDao.insertOrUpdate(updated)
            syncEngine.triggerSync()
        }
    }

    suspend fun updateScore(anilistId: Long, score: Double?) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val updated = currentTracking?.copy(
            score = score,
            lastModified = System.currentTimeMillis(),
            isSynced = false,
            isDeleted = false
        ) ?: UserTrackingEntity(
            anilistId = anilistId,
            watchStatus = "",
            episodeProgress = 0,
            score = score,
            notes = null,
            lastModified = System.currentTimeMillis(),
            isSynced = false,
            isDeleted = false
        )
        if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
            if (currentTracking != null) {
                val softDeleted = currentTracking.copy(
                    isDeleted = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                userTrackingDao.insertOrUpdate(softDeleted)
                syncEngine.triggerSync()
            }
        } else {
            userTrackingDao.insertOrUpdate(updated)
            syncEngine.triggerSync()
        }
    }

    suspend fun saveNotes(anilistId: Long, notes: String?) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val updated = currentTracking?.copy(
            notes = notes,
            lastModified = System.currentTimeMillis(),
            isSynced = false,
            isDeleted = false
        ) ?: UserTrackingEntity(
            anilistId = anilistId,
            watchStatus = "",
            episodeProgress = 0,
            score = null,
            notes = notes,
            lastModified = System.currentTimeMillis(),
            isSynced = false,
            isDeleted = false
        )
        if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
            if (currentTracking != null) {
                val softDeleted = currentTracking.copy(
                    isDeleted = true,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                userTrackingDao.insertOrUpdate(softDeleted)
                syncEngine.triggerSync()
            }
        } else {
            userTrackingDao.insertOrUpdate(updated)
            syncEngine.triggerSync()
        }
    }

    suspend fun deleteTracking(anilistId: Long) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        if (currentTracking != null) {
            val anime = animeRepository.getAnimeById(anilistId)
            val duration = (anime?.duration ?: 0).toLong()
            val watchTimeDelta = -currentTracking.episodeProgress.toLong() * duration
            if (watchTimeDelta != 0L) {
                userStatsDao.incrementWatchTime(watchTimeDelta)
            }
            val softDeleted = currentTracking.copy(
                isDeleted = true,
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
            userTrackingDao.insertOrUpdate(softDeleted)
            syncEngine.triggerSync()
        }
    }

    suspend fun updateEpisodeProgress(anilistId: Long, progress: Int) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val anime = animeRepository.getAnimeById(anilistId)
        val totalPlanned = anime?.episodes
        val currentStatus = currentTracking?.watchStatus ?: ""

        // Determine the new auto-status based on progress:
        // • progress reaches the total planned count  → COMPLETED
        // • For RELEASING anime without a known total, we don't auto-complete
        //   (the user must do that manually via the status picker).
        // • progress 0→1+ and current status is empty or PLANNING → CURRENT (Watching)
        // • otherwise, keep the existing status unchanged
        val status = when {
            totalPlanned != null && totalPlanned > 0 && progress >= totalPlanned -> "COMPLETED"
            progress >= 1 && (currentStatus.isEmpty() || currentStatus == "PLANNING") -> "CURRENT"
            progress >= 1 -> currentStatus  // keep existing status (e.g., PAUSED, DROPPED)
            else -> currentStatus
        }

        val oldProgress = currentTracking?.episodeProgress ?: 0
        val deltaEpisodes = progress - oldProgress
        if (deltaEpisodes != 0) {
            val duration = (anime?.duration ?: 0).toLong()
            val watchTimeDelta = deltaEpisodes.toLong() * duration
            userStatsDao.incrementWatchTime(watchTimeDelta)
        }

        val statusChanged = currentTracking?.let { it.watchStatus != status } ?: true
        val progressChanged = currentTracking?.let { it.episodeProgress != progress } ?: true
        val isModified = statusChanged || progressChanged

        val updated = currentTracking?.copy(
            watchStatus = status,
            episodeProgress = progress,
            lastModified = if (isModified) System.currentTimeMillis() else currentTracking.lastModified,
            isSynced = if (isModified) false else currentTracking.isSynced,
            isDeleted = false
        ) ?: UserTrackingEntity(
            anilistId = anilistId,
            watchStatus = status,
            episodeProgress = progress,
            score = null,
            notes = null,
            lastModified = System.currentTimeMillis(),
            isSynced = false,
            isDeleted = false
        )
        userTrackingDao.insertOrUpdate(updated)
        if (isModified) {
            syncEngine.triggerSync()
        }
    }

    suspend fun recalculateTotalWatchTime() = withContext(Dispatchers.IO) {
        val allTracking = userTrackingDao.getAllTrackingSync()
        val animeIds = allTracking.map { it.anilistId }
        val animeList = animeRepository.getAnimeByIds(animeIds)
        val animeDurationMap = animeList.associate { it.anilistId to (it.duration ?: 0) }

        var totalMinutes = 0L
        for (tracking in allTracking) {
            val duration = animeDurationMap[tracking.anilistId] ?: 0
            totalMinutes += tracking.episodeProgress.toLong() * duration
        }

        val currentStats = userStatsDao.getUserStatsSync() ?: UserStatsEntity(id = 0)
        userStatsDao.insertOrUpdate(currentStats.copy(totalWatchTimeMinutes = totalMinutes))
    }
}
