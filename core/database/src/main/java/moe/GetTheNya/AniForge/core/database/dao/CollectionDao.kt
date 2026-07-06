package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.CollectionEntity
import moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollectionById(id: String)

    @Query("DELETE FROM collection_anime_cross_ref WHERE collectionId IN (:collectionIds)")
    suspend fun deleteCrossRefsForCollections(collectionIds: List<String>)

    @Query("DELETE FROM collections WHERE id IN (:ids)")
    suspend fun deleteCollectionsByIds(ids: List<String>)

    @Transaction
    suspend fun deleteCollectionsWithRefs(ids: List<String>) {
        deleteCrossRefsForCollections(ids)
        deleteCollectionsByIds(ids)
    }

    @Query("SELECT * FROM collections WHERE is_deleted = 0 ORDER BY createdAt DESC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id AND is_deleted = 0")
    suspend fun getCollectionById(id: String): CollectionEntity?

    @Query("SELECT * FROM collections")
    suspend fun getAllCollectionsIncludingDeleted(): List<CollectionEntity>

    @Query("SELECT * FROM collection_anime_cross_ref")
    suspend fun getAllCrossRefsIncludingDeleted(): List<CollectionAnimeCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<CollectionAnimeCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: CollectionAnimeCrossRef)

    @Query("DELETE FROM collection_anime_cross_ref WHERE collectionId = :collectionId AND animeId = :animeId")
    suspend fun deleteCrossRef(collectionId: String, animeId: Long)

    @Query("DELETE FROM collection_anime_cross_ref WHERE collectionId = :collectionId")
    suspend fun deleteCrossRefsForCollection(collectionId: String)

    @Query("SELECT * FROM collection_anime_cross_ref WHERE collectionId = :collectionId AND is_deleted = 0 ORDER BY orderIndex ASC")
    fun observeCrossRefsForCollection(collectionId: String): Flow<List<CollectionAnimeCrossRef>>

    @Query("SELECT * FROM collection_anime_cross_ref WHERE collectionId = :collectionId AND is_deleted = 0 ORDER BY orderIndex ASC")
    suspend fun getCrossRefsForCollectionSync(collectionId: String): List<CollectionAnimeCrossRef>

    @Query("SELECT MAX(orderIndex) FROM collection_anime_cross_ref WHERE collectionId = :collectionId AND is_deleted = 0")
    suspend fun getMaxOrderIndex(collectionId: String): Int?

    @Query("SELECT collectionId FROM collection_anime_cross_ref WHERE animeId = :animeId AND is_deleted = 0")
    fun observeCollectionIdsForAnime(animeId: Long): Flow<List<String>>

    @Query("SELECT * FROM collection_anime_cross_ref WHERE is_deleted = 0")
    fun observeAllCrossRefs(): Flow<List<CollectionAnimeCrossRef>>

    @Query("SELECT * FROM collections WHERE is_synced = 0")
    suspend fun getUnsyncedCollections(): List<CollectionEntity>

    @Query("SELECT * FROM collection_anime_cross_ref WHERE is_synced = 0")
    suspend fun getUnsyncedCrossRefs(): List<CollectionAnimeCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCollectionsBatch(collections: List<CollectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCrossRefsBatch(crossRefs: List<CollectionAnimeCrossRef>)

    @Delete
    suspend fun deleteCollectionsBatch(collections: List<CollectionEntity>)

    @Delete
    suspend fun deleteCrossRefsBatch(crossRefs: List<CollectionAnimeCrossRef>)

    @Query("UPDATE collections SET is_deleted = 1, is_synced = 0, last_modified = :timestamp WHERE id = :id")
    suspend fun softDeleteCollectionById(id: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE collection_anime_cross_ref SET is_deleted = 1, is_synced = 0, last_modified = :timestamp WHERE collectionId = :collectionId AND animeId = :animeId")
    suspend fun softDeleteCrossRef(collectionId: String, animeId: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE collection_anime_cross_ref SET is_deleted = 1, is_synced = 0, last_modified = :timestamp WHERE collectionId = :collectionId")
    suspend fun softDeleteCrossRefsForCollection(collectionId: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE collections SET is_synced = 1 WHERE id = :id AND last_modified = :lastModified")
    suspend fun markCollectionSynced(id: String, lastModified: Long): Int

    @Query("UPDATE collection_anime_cross_ref SET is_synced = 1 WHERE collectionId = :collectionId AND animeId = :animeId AND last_modified = :lastModified")
    suspend fun markCrossRefSynced(collectionId: String, animeId: Long, lastModified: Long): Int

    @Transaction
    suspend fun markCollectionsSynced(ids: List<String>, timestamps: List<Long>) {
        for (i in ids.indices) {
            markCollectionSynced(ids[i], timestamps[i])
        }
    }

    @Transaction
    suspend fun markCrossRefsSynced(collectionIds: List<String>, animeIds: List<Long>, timestamps: List<Long>) {
        for (i in collectionIds.indices) {
            markCrossRefSynced(collectionIds[i], animeIds[i], timestamps[i])
        }
    }

    @Transaction
    suspend fun applyCollectionsMerge(toInsertOrUpdate: List<CollectionEntity>, toDelete: List<CollectionEntity>) {
        if (toInsertOrUpdate.isNotEmpty()) {
            insertOrUpdateCollectionsBatch(toInsertOrUpdate)
        }
        if (toDelete.isNotEmpty()) {
            deleteCollectionsBatch(toDelete)
        }
    }

    @Transaction
    suspend fun applyCrossRefsMerge(toInsertOrUpdate: List<CollectionAnimeCrossRef>, toDelete: List<CollectionAnimeCrossRef>) {
        if (toInsertOrUpdate.isNotEmpty()) {
            insertOrUpdateCrossRefsBatch(toInsertOrUpdate)
        }
        if (toDelete.isNotEmpty()) {
            deleteCrossRefsBatch(toDelete)
        }
    }

    @Transaction
    suspend fun softDeleteCollectionsWithRefs(ids: List<String>) {
        val timestamp = System.currentTimeMillis()
        for (id in ids) {
            softDeleteCollectionById(id, timestamp)
            softDeleteCrossRefsForCollection(id, timestamp)
        }
    }

    @Transaction
    suspend fun createNewCollectionWithAnime(title: String, description: String, animeId: Long): String {
        val collectionId = java.util.UUID.randomUUID().toString()
        insertCollection(
            CollectionEntity(
                id = collectionId,
                title = title,
                description = description,
                createdAt = System.currentTimeMillis(),
                isSynced = false,
                isDeleted = false,
                lastModified = System.currentTimeMillis()
            )
        )
        val maxIndex = getMaxOrderIndex(collectionId) ?: -1
        val ref = CollectionAnimeCrossRef(
            collectionId = collectionId,
            animeId = animeId,
            orderIndex = maxIndex + 1,
            isSynced = false,
            isDeleted = false,
            lastModified = System.currentTimeMillis()
        )
        insertCrossRef(ref)
        return collectionId
    }

    @Transaction
    suspend fun cloneCollection(title: String, description: String, animeIds: List<Long>): String {
        val newCollectionId = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        insertCollection(
            CollectionEntity(
                id = newCollectionId,
                title = title,
                description = description,
                createdAt = now,
                isSynced = false,
                isDeleted = false,
                lastModified = now
            )
        )
        val refs = animeIds.mapIndexed { index, animeId ->
            CollectionAnimeCrossRef(
                collectionId = newCollectionId,
                animeId = animeId,
                orderIndex = index,
                isSynced = false,
                isDeleted = false,
                lastModified = now
            )
        }
        insertCrossRefs(refs)
        return newCollectionId
    }

    @Query("SELECT * FROM collections WHERE title = :title AND is_deleted = 0 LIMIT 1")
    suspend fun getCollectionByTitle(title: String): CollectionEntity?
}
