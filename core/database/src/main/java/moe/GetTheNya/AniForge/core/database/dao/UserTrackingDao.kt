package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity

@Dao
abstract class UserTrackingDao(val db: RoomDatabase) {
    @Query("SELECT * FROM user_tracking WHERE anilist_id = :anilistId")
    abstract fun observeTrackingForAnime(anilistId: Long): Flow<UserTrackingEntity?>

    @Query("SELECT * FROM user_tracking WHERE anilist_id = :anilistId")
    abstract suspend fun getTrackingForAnimeSync(anilistId: Long): UserTrackingEntity?

    @Query("SELECT * FROM user_tracking ORDER BY last_modified DESC")
    abstract fun observeAllTracking(): Flow<List<UserTrackingEntity>>

    @Query("SELECT * FROM user_tracking WHERE watch_status = 'CURRENT' ORDER BY last_modified DESC LIMIT 10")
    abstract fun observeContinueWatching(): Flow<List<UserTrackingEntity>>

    @Query("SELECT * FROM user_tracking WHERE watch_status = 'PLANNING' ORDER BY last_modified DESC LIMIT 10")
    abstract fun observeNextUp(): Flow<List<UserTrackingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdate(tracking: UserTrackingEntity)

    @Delete
    abstract suspend fun delete(tracking: UserTrackingEntity)
    
    @Query("DELETE FROM user_tracking WHERE anilist_id = :anilistId")
    abstract suspend fun deleteByAnimeId(anilistId: Long)

    @Query("SELECT anilist_id FROM user_tracking WHERE watch_status = :statusId")
    abstract suspend fun getAnimeIdsByStatus(statusId: String): List<Long>

    @Query("SELECT * FROM user_tracking")
    abstract suspend fun getAllTrackingSync(): List<UserTrackingEntity>

    @Query("SELECT * FROM user_tracking WHERE anilist_id IN (:anilistIds)")
    abstract suspend fun getTrackingForAnimeIds(anilistIds: List<Long>): List<UserTrackingEntity>
}
