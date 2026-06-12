package moe.GetTheNya.AniForge.ui.dashboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTrackingRepository @Inject constructor(
    private val userTrackingDao: UserTrackingDao,
    private val animeRepository: AnimeRepository,
    private val settingsProvider: SettingsProvider
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

    suspend fun updateWatchStatus(anilistId: Long, status: String) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        if (currentTracking != null && currentTracking.watchStatus == status) {
            userTrackingDao.delete(currentTracking)
        } else {
            val anime = animeRepository.getAnimeById(anilistId)
            val maxEpisodes = anime?.episodes ?: 0
            val progress = if (status == "COMPLETED") {
                maxEpisodes
            } else {
                currentTracking?.episodeProgress ?: 0
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

    suspend fun updateScore(anilistId: Long, score: Double) = withContext(Dispatchers.IO) {
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
        userTrackingDao.insertOrUpdate(updated)
    }

    suspend fun updateEpisodeProgress(anilistId: Long, progress: Int) = withContext(Dispatchers.IO) {
        val currentTracking = userTrackingDao.getTrackingForAnimeSync(anilistId)
        val anime = animeRepository.getAnimeById(anilistId)
        val maxEpisodes = anime?.episodes ?: 0
        val status = if (maxEpisodes in 1..progress) "COMPLETED" else currentTracking?.watchStatus ?: "CURRENT"

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
