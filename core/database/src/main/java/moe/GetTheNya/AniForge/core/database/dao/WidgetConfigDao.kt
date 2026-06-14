package moe.GetTheNya.AniForge.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import moe.GetTheNya.AniForge.core.database.entity.WidgetConfigEntity

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_config ORDER BY order_index ASC")
    fun observeWidgetConfigs(): Flow<List<WidgetConfigEntity>>

    @Query("SELECT * FROM widget_config ORDER BY order_index ASC")
    suspend fun getWidgetConfigsSync(): List<WidgetConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(configs: List<WidgetConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: WidgetConfigEntity)
}
