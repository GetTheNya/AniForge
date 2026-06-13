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
    suspend fun deleteCollectionById(id: Int)

    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun observeCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: Int): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<CollectionAnimeCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: CollectionAnimeCrossRef)

    @Query("DELETE FROM collection_anime_cross_ref WHERE collectionId = :collectionId AND animeId = :animeId")
    suspend fun deleteCrossRef(collectionId: Int, animeId: Long)

    @Query("DELETE FROM collection_anime_cross_ref WHERE collectionId = :collectionId")
    suspend fun deleteCrossRefsForCollection(collectionId: Int)

    @Query("SELECT * FROM collection_anime_cross_ref WHERE collectionId = :collectionId ORDER BY orderIndex ASC")
    fun observeCrossRefsForCollection(collectionId: Int): Flow<List<CollectionAnimeCrossRef>>

    @Query("SELECT * FROM collection_anime_cross_ref WHERE collectionId = :collectionId ORDER BY orderIndex ASC")
    suspend fun getCrossRefsForCollectionSync(collectionId: Int): List<CollectionAnimeCrossRef>

    @Query("SELECT MAX(orderIndex) FROM collection_anime_cross_ref WHERE collectionId = :collectionId")
    suspend fun getMaxOrderIndex(collectionId: Int): Int?

    @Query("SELECT collectionId FROM collection_anime_cross_ref WHERE animeId = :animeId")
    fun observeCollectionIdsForAnime(animeId: Long): Flow<List<Int>>

    @Query("SELECT * FROM collection_anime_cross_ref")
    fun observeAllCrossRefs(): Flow<List<CollectionAnimeCrossRef>>

    @Transaction
    suspend fun createNewCollectionWithAnime(title: String, description: String, animeId: Long): Long {
        val collectionId = insertCollection(
            CollectionEntity(
                title = title,
                description = description,
                createdAt = System.currentTimeMillis()
            )
        ).toInt()
        val maxIndex = getMaxOrderIndex(collectionId) ?: -1
        val ref = CollectionAnimeCrossRef(
            collectionId = collectionId,
            animeId = animeId,
            orderIndex = maxIndex + 1
        )
        insertCrossRef(ref)
        return collectionId.toLong()
    }
}
