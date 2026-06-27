package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.PendingImportEntity

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
}
