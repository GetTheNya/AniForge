package moe.GetTheNya.AniForge.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import moe.GetTheNya.AniForge.core.database.dao.UserTrackingDao
import moe.GetTheNya.AniForge.core.database.entity.UserTrackingEntity

@Database(
    entities = [UserTrackingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userTrackingDao(): UserTrackingDao
}
