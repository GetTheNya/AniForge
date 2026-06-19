package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity

@Dao
interface UserTrackingDao {
    @Query("SELECT * FROM user_tracking WHERE anilist_id = :anilistId")
    fun observeTrackingForAnime(anilistId: Long): Flow<UserTrackingEntity?>

    @Query("SELECT * FROM user_tracking WHERE anilist_id = :anilistId")
    suspend fun getTrackingForAnimeSync(anilistId: Long): UserTrackingEntity?

    @Query("SELECT * FROM user_tracking ORDER BY last_modified DESC")
    fun observeAllTracking(): Flow<List<UserTrackingEntity>>

    @Query("SELECT * FROM user_tracking WHERE watch_status = 'CURRENT' ORDER BY last_modified DESC LIMIT 10")
    fun observeContinueWatching(): Flow<List<UserTrackingEntity>>

    @Query("SELECT * FROM user_tracking WHERE watch_status = 'PLANNING' ORDER BY last_modified DESC LIMIT 10")
    fun observeNextUp(): Flow<List<UserTrackingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tracking: UserTrackingEntity)

    @Delete
    suspend fun delete(tracking: UserTrackingEntity)
    
    @Query("DELETE FROM user_tracking WHERE anilist_id = :anilistId")
    suspend fun deleteByAnimeId(anilistId: Long)

    @Query("SELECT anilist_id FROM user_tracking WHERE watch_status = :statusId")
    suspend fun getAnimeIdsByStatus(statusId: String): List<Long>

    @Query("SELECT * FROM user_tracking")
    suspend fun getAllTrackingSync(): List<UserTrackingEntity>
}
