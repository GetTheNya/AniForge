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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTrackingRepository @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider,
    private val userStatsDao: UserStatsDao
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

    suspend fun incrementChaosMeter() = withContext(Dispatchers.IO) {
        userStatsDao.incrementChaosMeter(1)
    }

    suspend fun updateWatchStatus(anilistId: Long, status: String) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val anime = animeRepository.getAnimeById(anilistId)
        val duration = (anime?.duration ?: 0).toLong()

        if (currentTracking != null && currentTracking.watchStatus == status) {
            val oldProgress = currentTracking.episodeProgress
            val watchTimeDelta = -oldProgress.toLong() * duration
            if (watchTimeDelta != 0L) {
                userStatsDao.incrementWatchTime(watchTimeDelta)
            }

            val updated = currentTracking.copy(
                watchStatus = "",
                episodeProgress = 0,
                lastModified = System.currentTimeMillis()
            )
            if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
                userTrackingDao.delete(currentTracking)
            } else {
                userTrackingDao.insertOrUpdate(updated)
            }
        } else {
            val maxEpisodes = anime?.episodes ?: 0
            val progress = if (status == "COMPLETED") {
                maxEpisodes
            } else {
                currentTracking?.episodeProgress ?: 0
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
                lastModified = System.currentTimeMillis()
            ) ?: UserTrackingEntity(
                anilistId = anilistId,
                watchStatus = status,
                episodeProgress = progress,
                score = null,
                notes = null,
                lastModified = System.currentTimeMillis()
            )
            userTrackingDao.insertOrUpdate(updated)
        }
    }

    suspend fun updateScore(anilistId: Long, score: Double?) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val updated = currentTracking?.copy(
            score = score,
            lastModified = System.currentTimeMillis()
        ) ?: UserTrackingEntity(
            anilistId = anilistId,
            watchStatus = "",
            episodeProgress = 0,
            score = score,
            notes = null,
            lastModified = System.currentTimeMillis()
        )
        if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
            if (currentTracking != null) {
                userTrackingDao.delete(currentTracking)
            }
        } else {
            userTrackingDao.insertOrUpdate(updated)
        }
    }

    suspend fun saveNotes(anilistId: Long, notes: String?) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val updated = currentTracking?.copy(
            notes = notes,
            lastModified = System.currentTimeMillis()
        ) ?: UserTrackingEntity(
            anilistId = anilistId,
            watchStatus = "",
            episodeProgress = 0,
            score = null,
            notes = notes,
            lastModified = System.currentTimeMillis()
        )
        if (updated.watchStatus.isEmpty() && updated.episodeProgress == 0 && updated.score == null && updated.notes.isNullOrEmpty()) {
            if (currentTracking != null) {
                userTrackingDao.delete(currentTracking)
            }
        } else {
            userTrackingDao.insertOrUpdate(updated)
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
            userTrackingDao.delete(currentTracking)
        }
    }

    suspend fun updateEpisodeProgress(anilistId: Long, progress: Int) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val anime = animeRepository.getAnimeById(anilistId)
        val maxEpisodes = anime?.episodes ?: 0
        val status = if (maxEpisodes in 1..progress) "COMPLETED" else currentTracking?.watchStatus ?: "CURRENT"

        val oldProgress = currentTracking?.episodeProgress ?: 0
        val deltaEpisodes = progress - oldProgress
        if (deltaEpisodes != 0) {
            val duration = (anime?.duration ?: 0).toLong()
            val watchTimeDelta = deltaEpisodes.toLong() * duration
            userStatsDao.incrementWatchTime(watchTimeDelta)
        }

        val updated = currentTracking?.let {
            val statusChanged = it.watchStatus != status
            it.copy(
                watchStatus = status,
                episodeProgress = progress,
                lastModified = if (statusChanged) System.currentTimeMillis() else it.lastModified
            )
        } ?: UserTrackingEntity(
            anilistId = anilistId,
            watchStatus = status,
            episodeProgress = progress,
            score = null,
            notes = null,
            lastModified = System.currentTimeMillis()
        )
        userTrackingDao.insertOrUpdate(updated)
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
