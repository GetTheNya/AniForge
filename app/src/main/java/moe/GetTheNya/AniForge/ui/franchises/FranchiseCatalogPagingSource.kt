package moe.GetTheNya.AniForge.ui.franchises

import androidx.paging.PagingSource
import androidx.paging.PagingState
import moe.GetTheNya.AniForge.core.database.repository.AnimeRepository

class FranchiseCatalogPagingSource(
    private val animeRepository: AnimeRepository,
    private val query: String,
    private val pageSize: Int
) : PagingSource<Int, FranchiseItem>() {

    override fun getRefreshKey(state: PagingState<Int, FranchiseItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val pageSize = state.config.pageSize
            (anchorPosition / pageSize) * pageSize
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FranchiseItem> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        
        return try {
            val rawList = animeRepository.queryFranchisesPaged(query, limit, offset)
            val mappedList = rawList.map { (franchise, releases) ->
                FranchiseItem(franchise, releases)
            }
            
            LoadResult.Page(
                data = mappedList,
                prevKey = if (offset == 0) null else maxOf(0, offset - pageSize),
                nextKey = if (mappedList.size < limit) null else offset + mappedList.size
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
