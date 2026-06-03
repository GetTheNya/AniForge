package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.UserSettingEntity

@Dao
interface UserSettingDao {
    @Query("SELECT value FROM user_settings WHERE `key` = :key")
    fun observeSetting(key: String): Flow<String?>

    @Query("SELECT value FROM user_settings WHERE `key` = :key")
    suspend fun getSettingSync(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: UserSettingEntity)
}
