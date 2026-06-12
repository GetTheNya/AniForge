package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "collection_anime_cross_ref",
    primaryKeys = ["collectionId", "animeId"]
)
data class CollectionAnimeCrossRef(
    @ColumnInfo(name = "collectionId")
    val collectionId: Int,
    @ColumnInfo(name = "animeId")
    val animeId: Long,
    @ColumnInfo(name = "orderIndex")
    val orderIndex: Int
)
