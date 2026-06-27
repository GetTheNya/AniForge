package moe.GetTheNya.AniForge.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.UserSettingDao
import moe.GetTheNya.AniForge.core.database.dao.PendingImportDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.entity.UserSettingEntity
import moe.GetTheNya.AniForge.core.database.entity.PendingImportEntity
import moe.GetTheNya.AniForge.core.database.entity.TargetStatusConverter

@Database(
    entities = [
        UserTrackingEntity::class,
        UserSettingEntity::class,
        moe.GetTheNya.AniForge.core.database.entity.CollectionEntity::class,
        moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef::class,
        moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity::class,
        moe.GetTheNya.AniForge.core.database.entity.WidgetConfigEntity::class,
        PendingImportEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(TargetStatusConverter::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userTrackingDao(): UserTrackingDao
    abstract fun userSettingDao(): UserSettingDao
    abstract fun collectionDao(): moe.GetTheNya.AniForge.core.database.dao.CollectionDao
    abstract fun userStatsDao(): moe.GetTheNya.AniForge.core.database.dao.UserStatsDao
    abstract fun widgetConfigDao(): moe.GetTheNya.AniForge.core.database.dao.WidgetConfigDao
    abstract fun pendingImportDao(): PendingImportDao
}
