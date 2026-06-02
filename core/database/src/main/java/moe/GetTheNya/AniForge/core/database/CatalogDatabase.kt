package moe.GetTheNya.AniForge.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import moe.GetTheNya.AniForge.core.database.dao.AnimeDao
import moe.GetTheNya.AniForge.core.database.entity.AnimeEntity


@Database(
    entities = [
        AnimeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao
}
