package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "dismissed_sequels", primaryKeys = ["userId", "animeId"])
data class DismissedSequelEntity(
    @ColumnInfo(name = "userId")
    val userId: String = "local_user",
    
    @ColumnInfo(name = "animeId")
    val animeId: Long
)
