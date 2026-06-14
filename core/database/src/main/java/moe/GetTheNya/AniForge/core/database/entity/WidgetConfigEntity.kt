package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_config")
data class WidgetConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "widget_id")
    val widgetId: String,
    
    @ColumnInfo(name = "is_visible")
    val isVisible: Boolean,
    
    @ColumnInfo(name = "order_index")
    val orderIndex: Int
)
