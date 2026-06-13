package moe.GetTheNya.AniForge.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.dao.UserSettingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity
import moe.GetTheNya.AniForge.core.database.entity.UserSettingEntity

@Database(
    entities = [
        UserTrackingEntity::class,
        UserSettingEntity::class,
        moe.GetTheNya.AniForge.core.database.entity.CollectionEntity::class,
        moe.GetTheNya.AniForge.core.database.entity.CollectionAnimeCrossRef::class,
        moe.GetTheNya.AniForge.core.database.entity.UserStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userTrackingDao(): UserTrackingDao
    abstract fun userSettingDao(): UserSettingDao
    abstract fun collectionDao(): moe.GetTheNya.AniForge.core.database.dao.CollectionDao
    abstract fun userStatsDao(): moe.GetTheNya.AniForge.core.database.dao.UserStatsDao
}
