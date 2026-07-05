package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_tracking")
data class UserTrackingEntity(
    @PrimaryKey
    @ColumnInfo(name = "anilist_id")
    val anilistId: Long,
    
    @ColumnInfo(name = "watch_status")
    val watchStatus: String, // CURRENT, COMPLETED, PLANNING, DROPPED, PAUSED, REWATCHING
    
    @ColumnInfo(name = "episode_progress")
    val episodeProgress: Int,
    
    @ColumnInfo(name = "score")
    val score: Double?,
    
    @ColumnInfo(name = "notes")
    val notes: String?,
    
    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false
)
