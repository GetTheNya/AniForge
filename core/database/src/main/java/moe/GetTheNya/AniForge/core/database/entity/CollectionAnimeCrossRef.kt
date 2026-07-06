package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "collection_anime_cross_ref",
    primaryKeys = ["collectionId", "animeId"]
)
data class CollectionAnimeCrossRef(
    @ColumnInfo(name = "collectionId")
    val collectionId: String,
    @ColumnInfo(name = "animeId")
    val animeId: Long,
    @ColumnInfo(name = "orderIndex")
    val orderIndex: Int,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "last_modified", defaultValue = "0")
    val lastModified: Long = System.currentTimeMillis()
)
