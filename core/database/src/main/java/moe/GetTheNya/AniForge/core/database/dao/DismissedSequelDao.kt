package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.DismissedSequelEntity

@Dao
interface DismissedSequelDao {
    @Query("SELECT * FROM dismissed_sequels WHERE userId = :userId")
    fun observeAllDismissed(userId: String = "local_user"): Flow<List<DismissedSequelEntity>>

    @Query("SELECT animeId FROM dismissed_sequels WHERE userId = :userId")
    suspend fun getDismissedAnimeIdsSync(userId: String = "local_user"): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dismissedSequel: DismissedSequelEntity)

    @Delete
    suspend fun delete(dismissedSequel: DismissedSequelEntity)

    @Query("DELETE FROM dismissed_sequels WHERE userId = :userId AND animeId = :animeId")
    suspend fun deleteByAnimeId(userId: String = "local_user", animeId: Long)
}
