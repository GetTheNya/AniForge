package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 0")
    fun observeUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 0")
    suspend fun getUserStatsSync(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity)

    @Transaction
    suspend fun incrementWatchTime(delta: Long) {
        val current = getUserStatsSync() ?: UserStatsEntity(id = 0)
        val newWatchTime = (current.totalWatchTimeMinutes + delta).coerceAtLeast(0L)
        insertOrUpdate(current.copy(totalWatchTimeMinutes = newWatchTime))
    }

    @Transaction
    suspend fun incrementChaosMeter(delta: Int) {
        val current = getUserStatsSync() ?: UserStatsEntity(id = 0)
        insertOrUpdate(current.copy(chaosMeterCount = current.chaosMeterCount + delta))
    }
}
