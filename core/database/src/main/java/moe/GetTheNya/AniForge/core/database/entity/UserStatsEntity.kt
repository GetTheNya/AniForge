package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 0,
    
    @ColumnInfo(name = "total_watch_time_minutes")
    val totalWatchTimeMinutes: Long = 0L,
    
    @ColumnInfo(name = "chaos_meter_count")
    val chaosMeterCount: Int = 0
)
