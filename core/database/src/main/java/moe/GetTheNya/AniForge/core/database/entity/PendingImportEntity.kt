package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class TargetStatus {
    CURRENT,
    PLANNING,
    COMPLETED,
    PAUSED,
    DROPPED,
    UNKNOWN
}

class TargetStatusConverter {
    @TypeConverter
    fun fromTargetStatus(status: TargetStatus): String {
        return status.name
    }

    @TypeConverter
    fun toTargetStatus(value: String): TargetStatus {
        return try {
            TargetStatus.valueOf(value)
        } catch (e: Exception) {
            TargetStatus.UNKNOWN
        }
    }
}

enum class ImportStatus {
    PENDING,
    SUCCESS,
    AMBIGUOUS,
    NOT_FOUND,
    RESOLVED,
    ERROR
}

class ImportStatusConverter {
    @TypeConverter
    fun fromImportStatus(status: ImportStatus): String {
        return status.name
    }

    @TypeConverter
    fun toImportStatus(value: String): ImportStatus {
        return try {
            ImportStatus.valueOf(value)
        } catch (e: Exception) {
            ImportStatus.PENDING
        }
    }
}

@Entity(tableName = "pending_imports")
data class PendingImportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "raw_row_text")
    val rawRowText: String,
    
    @ColumnInfo(name = "russian_title")
    val russianTitle: String,
    
    @ColumnInfo(name = "original_title")
    val originalTitle: String,
    
    @ColumnInfo(name = "alternative_titles")
    val alternativeTitles: String,
    
    @ColumnInfo(name = "target_status")
    val targetStatus: TargetStatus,
    
    @ColumnInfo(name = "target_score")
    val targetScore: Double?,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean,

    @ColumnInfo(name = "matched_anime_id")
    val matchedAnimeId: Long? = null,

    @ColumnInfo(name = "import_status")
    val importStatus: ImportStatus = ImportStatus.PENDING
)
