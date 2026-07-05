package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity

@Dao
abstract class UserTrackingDao(val db: RoomDatabase) {
    @Query("SELECT * FROM user_tracking WHERE anilist_id = :anilistId AND is_deleted = 0")
    abstract fun observeTrackingForAnime(anilistId: Long): Flow<UserTrackingEntity?>

    @Query("SELECT * FROM user_tracking WHERE anilist_id = :anilistId AND is_deleted = 0")
    abstract suspend fun getTrackingForAnimeSync(anilistId: Long): UserTrackingEntity?

    @Query("SELECT * FROM user_tracking WHERE is_deleted = 0 ORDER BY last_modified DESC")
    abstract fun observeAllTracking(): Flow<List<UserTrackingEntity>>

    @Query("SELECT * FROM user_tracking WHERE watch_status = 'CURRENT' AND is_deleted = 0 ORDER BY last_modified DESC LIMIT 10")
    abstract fun observeContinueWatching(): Flow<List<UserTrackingEntity>>

    @Query("SELECT * FROM user_tracking WHERE watch_status = 'PLANNING' AND is_deleted = 0 ORDER BY last_modified DESC LIMIT 10")
    abstract fun observeNextUp(): Flow<List<UserTrackingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdate(tracking: UserTrackingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdateBatch(trackingList: List<UserTrackingEntity>)

    @Delete
    abstract suspend fun delete(tracking: UserTrackingEntity)

    @Delete
    abstract suspend fun deleteBatch(trackingList: List<UserTrackingEntity>)
    
    @Query("DELETE FROM user_tracking WHERE anilist_id = :anilistId")
    abstract suspend fun deleteByAnimeId(anilistId: Long)

    @Query("SELECT anilist_id FROM user_tracking WHERE watch_status = :statusId AND is_deleted = 0")
    abstract suspend fun getAnimeIdsByStatus(statusId: String): List<Long>

    @Query("SELECT * FROM user_tracking WHERE is_deleted = 0")
    abstract suspend fun getAllTrackingSync(): List<UserTrackingEntity>

    @Query("SELECT * FROM user_tracking")
    abstract suspend fun getAllTrackingIncludingDeleted(): List<UserTrackingEntity>

    @Query("SELECT * FROM user_tracking WHERE anilist_id IN (:anilistIds) AND is_deleted = 0")
    abstract suspend fun getTrackingForAnimeIds(anilistIds: List<Long>): List<UserTrackingEntity>

    @Query("SELECT * FROM user_tracking WHERE is_synced = 0")
    abstract suspend fun getUnsyncedTracking(): List<UserTrackingEntity>

    @Query("UPDATE user_tracking SET is_synced = 1 WHERE anilist_id = :anilistId AND last_modified = :lastModified")
    abstract suspend fun markRecordSynced(anilistId: Long, lastModified: Long): Int

    @Transaction
    open suspend fun markRecordsSynced(ids: List<Long>, timestamps: List<Long>) {
        for (i in ids.indices) {
            markRecordSynced(ids[i], timestamps[i])
        }
    }

    @Query("DELETE FROM user_tracking WHERE anilist_id IN (:anilistIds)")
    abstract suspend fun deleteByAnimeIds(anilistIds: List<Long>)

    @Transaction
    open suspend fun applyMergeResults(
        toInsertOrUpdate: List<UserTrackingEntity>,
        toDelete: List<UserTrackingEntity> = emptyList(),
        toDeleteIds: List<Long> = emptyList()
    ) {
        if (toInsertOrUpdate.isNotEmpty()) {
            insertOrUpdateBatch(toInsertOrUpdate)
        }
        if (toDelete.isNotEmpty()) {
            deleteBatch(toDelete)
        }
        if (toDeleteIds.isNotEmpty()) {
            deleteByAnimeIds(toDeleteIds)
        }
    }
}

