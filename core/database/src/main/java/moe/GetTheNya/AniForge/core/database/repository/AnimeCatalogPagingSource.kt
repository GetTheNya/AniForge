package moe.GetTheNya.AniForge.core.database.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.model.AnimeWithTracking
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery

class AnimeCatalogPagingSource(
    private val animeRepository: AnimeRepository,
    private val userTrackingDao: UserTrackingDao,
    private val filter: SearchFilterQuery,
    private val pageSize: Int
) : PagingSource<Int, AnimeWithTracking>() {

    override fun getRefreshKey(state: PagingState<Int, AnimeWithTracking>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val pageSize = state.config.pageSize
            (anchorPosition / pageSize) * pageSize
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AnimeWithTracking> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        
        return try {
            // Resolve tracking status IDs dynamically to keep Pager stream stable
            val enrichedFilter = if (filter.trackingStatuses.isNotEmpty() || filter.excludedTrackingStatuses.isNotEmpty()) {
                val trackingList = userTrackingDao.getAllTrackingSync()
                val trackingStatusIds = trackingList
                    .filter { it.watchStatus in filter.trackingStatuses }
                    .map { it.anilistId }
                val excludedTrackingStatusIds = trackingList
                    .filter { it.watchStatus in filter.excludedTrackingStatuses }
                    .map { it.anilistId }
                filter.copy(
                    trackingStatusIds = trackingStatusIds,
                    excludedTrackingStatusIds = excludedTrackingStatusIds
                )
            } else {
                filter
            }

            // Raw SQL Pagination Fetch
            val rawAnimeList = animeRepository.queryAnimePaged(
                filter = enrichedFilter,
                limit = limit,
                offset = offset
            )

            // Room Metadata Enrichment
            val animeIds = rawAnimeList.map { it.anilistId }
            val trackingEntities = if (animeIds.isNotEmpty()) {
                userTrackingDao.getTrackingForAnimeIds(animeIds)
            } else {
                emptyList()
            }
            val trackingMap = trackingEntities.associateBy { it.anilistId }

            // Data Merging
            val mergedList = rawAnimeList.map { anime ->
                val tracking = trackingMap[anime.anilistId]
                AnimeWithTracking(
                    anime = anime,
                    watchStatus = tracking?.watchStatus,
                    episodeProgress = tracking?.episodeProgress ?: 0,
                    score = tracking?.score
                )
            }

            LoadResult.Page(
                data = mergedList,
                prevKey = if (offset == 0) null else maxOf(0, offset - pageSize),
                nextKey = if (rawAnimeList.size < limit) null else offset + rawAnimeList.size
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
