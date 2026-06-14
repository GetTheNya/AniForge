package moe.GetTheNya.AniForge.core.database.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.CollectionDao
import moe.GetTheNya.AniForge.core.database.dao.UserStatsDao
import moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity
import moe.GetTheNya.AniForge.core.database.entity.WidgetConfigEntity
import moe.GetTheNya.AniForge.core.model.BentoStatsData
import moe.GetTheNya.AniForge.core.model.FranchiseGiantInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BentoWidgetRepository @Inject constructor(
    private val userStatsDao: UserStatsDao,
    private val userTrackingDao: UserTrackingDao,
    private val collectionDao: CollectionDao,
    private val animeRepository: AnimeRepository,
    private val widgetConfigDao: moe.GetTheNya.AniForge.core.database.dao.WidgetConfigDao
) {
    val observeWidgetConfigs: Flow<List<WidgetConfigEntity>> = widgetConfigDao.observeWidgetConfigs()
        .onStart {
            val current = widgetConfigDao.getWidgetConfigsSync()
            if (current.isEmpty()) {
                val defaults = listOf(
                     WidgetConfigEntity("watch_time", false, 0),
                     WidgetConfigEntity("watch_status_chart", false, 1),
                     WidgetConfigEntity("chaos_meter", false, 2),
                     WidgetConfigEntity("personal_collections", false, 3),
                     WidgetConfigEntity("top_studios", false, 4),
                     WidgetConfigEntity("top_genres", false, 5),
                     WidgetConfigEntity("franchise_giant", false, 6)
                )
                widgetConfigDao.insertOrUpdate(defaults)
            }
        }
        .flowOn(Dispatchers.IO)

    suspend fun updateWidgetConfigs(configs: List<WidgetConfigEntity>) {
        widgetConfigDao.insertOrUpdate(configs)
    }

    val observeUserStats: Flow<UserStatsEntity> = userStatsDao.observeUserStats()
        .onStart {
            val current = userStatsDao.getUserStatsSync()
            if (current == null) {
                val allTracking = userTrackingDao.getAllTrackingSync()
                var totalWatchTime = 0L
                val ids = allTracking.map { it.anilistId }
                if (ids.isNotEmpty()) {
                    val animeList = animeRepository.getAnimeByIds(ids)
                    val animeMap = animeList.associateBy { it.anilistId }
                    for (tracking in allTracking) {
                        val anime = animeMap[tracking.anilistId]
                        if (anime != null) {
                            totalWatchTime += tracking.episodeProgress.toLong() * (anime.duration ?: 0)
                        }
                    }
                }
                userStatsDao.insertOrUpdate(UserStatsEntity(id = 0, totalWatchTimeMinutes = totalWatchTime, chaosMeterCount = 0))
            }
        }
        .map { it ?: UserStatsEntity() }
        .flowOn(Dispatchers.IO)

    val bentoStatsFlow: Flow<BentoStatsData> = combine(
        userTrackingDao.observeAllTracking(),
        collectionDao.observeCollections(),
        collectionDao.observeAllCrossRefs(),
        animeRepository.swapSignal.onStart { emit(Unit) }
    ) { trackingList, collections, crossRefs, _ ->
        val filteredTracking = trackingList.filter {
            it.watchStatus == "COMPLETED" || (it.watchStatus == "CURRENT" && it.episodeProgress > 0)
        }
        val ids = filteredTracking.map { it.anilistId }
        val progressMap = filteredTracking.associate { it.anilistId to it.episodeProgress }

        val genreDist = if (ids.isNotEmpty()) {
            animeRepository.getGenreDistributions(ids)
        } else emptyList()

        val studioDist = if (ids.isNotEmpty()) {
            animeRepository.getStudioDistributions(ids)
        } else emptyList()

        val franchiseGiant = if (ids.isNotEmpty()) {
            val animeFranchises = animeRepository.getFranchisesForAnimeIds(ids)
            val franchiseProgress = mutableMapOf<Long, Int>()
            for ((animeId, franchiseId) in animeFranchises) {
                val progress = progressMap[animeId] ?: 0
                franchiseProgress[franchiseId] = (franchiseProgress[franchiseId] ?: 0) + progress
            }
            val topFranchiseId = franchiseProgress.maxByOrNull { it.value }?.key
            if (topFranchiseId != null) {
                val franchise = animeRepository.getFranchiseById(topFranchiseId)
                val totalEps = franchiseProgress[topFranchiseId] ?: 0
                if (franchise != null && totalEps > 0) {
                    FranchiseGiantInfo(franchise, totalEps)
                } else null
            } else null
        } else null

        val collectionAnimeIds = crossRefs.map { it.animeId }.distinct()
        val collectionCovers = if (collectionAnimeIds.isNotEmpty()) {
            animeRepository.getAnimeByIds(collectionAnimeIds.take(4)).mapNotNull { it.coverLarge }
        } else emptyList()

        BentoStatsData(
            genreDistributions = genreDist,
            studioDistributions = studioDist,
            franchiseGiant = franchiseGiant,
            activeCollectionsCount = collections.size,
            collectionCovers = collectionCovers
        )
    }.flowOn(Dispatchers.IO)
}
