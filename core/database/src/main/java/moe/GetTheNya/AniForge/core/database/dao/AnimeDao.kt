package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.AnimeEntity

@Dao
interface AnimeDao {
    @Query("SELECT * FROM anime WHERE anilist_id = :id")
    suspend fun getAnimeById(id: Long): AnimeEntity?

    @Query("SELECT * FROM anime ORDER BY score_mal DESC")
    fun observeAllAnime(): Flow<List<AnimeEntity>>


    /**
     * Performs dynamic raw SQL filtering for runtime multi-layered queries.
     */
    @RawQuery
    suspend fun getFilteredAnime(query: SupportSQLiteQuery): List<AnimeEntity>
}
