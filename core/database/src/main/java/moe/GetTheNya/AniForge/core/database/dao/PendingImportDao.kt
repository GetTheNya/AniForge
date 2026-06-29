package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction
import moe.GetTheNya.AniForge.core.database.entity.PendingImportEntity
import moe.GetTheNya.AniForge.core.database.entity.ImportStatus
import moe.GetTheNya.AniForge.core.database.entity.TargetStatus

@Dao
abstract class PendingImportDao(val db: RoomDatabase) {
    @Query("SELECT COUNT(*) FROM pending_imports")
    abstract fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pending_imports")
    abstract fun observeAll(): Flow<List<PendingImportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(rows: List<PendingImportEntity>)

    @Query("DELETE FROM pending_imports WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_imports")
    abstract suspend fun deleteAll()

    @Query("SELECT * FROM pending_imports WHERE import_status = :status")
    abstract suspend fun getByStatus(status: ImportStatus): List<PendingImportEntity>

    @Query("SELECT * FROM pending_imports WHERE import_status IN ('SUCCESS', 'RESOLVED')")
    abstract suspend fun getReadyToImport(): List<PendingImportEntity>

    @Query("UPDATE pending_imports SET matched_anime_id = :matchedAnimeId, import_status = :status WHERE id = :id")
    abstract suspend fun updateMatchedAnime(id: Long, matchedAnimeId: Long?, status: ImportStatus)

    @Query("UPDATE pending_imports SET import_status = :status WHERE id = :id")
    abstract suspend fun updateStatus(id: Long, status: ImportStatus)

    @Query("UPDATE pending_imports SET matched_anime_id = NULL, import_status = 'AMBIGUOUS' WHERE id IN (:ids)")
    abstract suspend fun demoteToAmbiguous(ids: List<Long>)


    @Transaction
    open suspend fun commitImportTransaction(
        syncStatus: Boolean,
        syncRating: Boolean,
        collectionTitle: String,
        collectionDesc: String,
        animeEpisodesMap: Map<Long, Int?>
    ) {
        val userDb = db as moe.GetTheNya.AniForge.core.database.UserDatabase
        val readyList = getReadyToImport()
        val userTrackingDao = userDb.userTrackingDao()
        val collectionDao = userDb.collectionDao()
        
        for (entity in readyList) {
            val matchedId = entity.matchedAnimeId ?: continue
            
            val currentTracking = userTrackingDao.getTrackingForAnimeSync(matchedId)
            val finalStatus = if (syncStatus) {
                when (entity.targetStatus) {
                    TargetStatus.CURRENT -> "CURRENT"
                    TargetStatus.PLANNING -> "PLANNING"
                    TargetStatus.COMPLETED -> "COMPLETED"
                    TargetStatus.PAUSED -> "PAUSED"
                    TargetStatus.DROPPED -> "DROPPED"
                    else -> ""
                }
            } else {
                currentTracking?.watchStatus ?: ""
            }
            
            val finalScore = if (syncRating) entity.targetScore else currentTracking?.score
            
            val progress = when {
                finalStatus == "COMPLETED" -> {
                    animeEpisodesMap[matchedId] ?: (currentTracking?.episodeProgress ?: 0)
                }
                else -> currentTracking?.episodeProgress ?: 0
            }
            
            val shouldInsert = finalStatus.isNotEmpty() || finalScore != null || currentTracking != null
            if (shouldInsert) {
                val tracking = currentTracking?.copy(
                    watchStatus = finalStatus,
                    episodeProgress = progress,
                    score = finalScore,
                    lastModified = System.currentTimeMillis()
                ) ?: moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity(
                    anilistId = matchedId,
                    watchStatus = finalStatus,
                    episodeProgress = progress,
                    score = finalScore,
                    notes = null,
                    lastModified = System.currentTimeMillis()
                )
                userTrackingDao.insertOrUpdate(tracking)
            }
            
            if (entity.isFavorite) {
                val collection = collectionDao.getCollectionByTitle(collectionTitle)
                val collectionId = if (collection != null) {
                    collection.id
                } else {
                    collectionDao.insertCollection(
                        moe.GetTheNya.AniForge.core.database.entity.CollectionEntity(
                            title = collectionTitle,
                            description = collectionDesc,
                            createdAt = System.currentTimeMillis()
                        )
                    ).toInt()
                }
                
                val existingRefs = collectionDao.getCrossRefsForCollectionSync(collectionId)
                val isAlreadyLinked = existingRefs.any { it.animeId == matchedId }
                if (!isAlreadyLinked) {
                    val maxIndex = existingRefs.maxOfOrNull { it.orderIndex } ?: -1
                    collectionDao.insertCrossRef(
                        moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef(
                            collectionId = collectionId,
                            animeId = matchedId,
                            orderIndex = maxIndex + 1
                        )
                    )
                }
            }
        }
        
        deleteAll()
    }
}
