package moe.GetTheNya.AniForge.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import moe.GetTheNya.AniForge.core.model.Anime

@Entity(tableName = "anime")
data class AnimeEntity(
    @PrimaryKey
    @ColumnInfo(name = "anilist_id")
    val anilistId: Long,
    
    @ColumnInfo(name = "mal_id")
    val malId: Long?,
    
    @ColumnInfo(name = "title_uk")
    val titleUk: String?,
    
    @ColumnInfo(name = "title_romaji")
    val titleRomaji: String,
    
    @ColumnInfo(name = "title_en")
    val titleEn: String?,
    
    @ColumnInfo(name = "description_uk")
    val descriptionUk: String?,
    
    @ColumnInfo(name = "description_en")
    val descriptionEn: String?,
    
    @ColumnInfo(name = "format")
    val format: String?,
    
    @ColumnInfo(name = "status")
    val status: String?,
    
    @ColumnInfo(name = "episodes")
    val episodes: Int?,
    
    @ColumnInfo(name = "duration")
    val duration: Int?,
    
    @ColumnInfo(name = "season_year")
    val seasonYear: Int?,
    
    @ColumnInfo(name = "season")
    val season: String?,
    
    @ColumnInfo(name = "is_adult")
    val isAdult: Boolean,
    
    @ColumnInfo(name = "score_mal")
    val scoreMal: Double?,
    
    @ColumnInfo(name = "cover_extra_large")
    val coverExtraLarge: String?,
    
    @ColumnInfo(name = "cover_large")
    val coverLarge: String?,
    
    @ColumnInfo(name = "cover_medium")
    val coverMedium: String?,
    
    @ColumnInfo(name = "cover_color")
    val coverColor: String?,
    
    @ColumnInfo(name = "banner_image")
    val bannerImage: String?,
    
    @ColumnInfo(name = "has_uk_translation")
    val hasUkTranslation: Boolean,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    fun toDomain(): Anime {
        return Anime(
            anilistId = anilistId,
            malId = malId,
            titleUk = titleUk,
            titleRomaji = titleRomaji,
            titleEn = titleEn,
            descriptionUk = descriptionUk,
            descriptionEn = descriptionEn,
            format = format,
            status = status,
            episodes = episodes,
            duration = duration,
            seasonYear = seasonYear,
            season = season,
            isAdult = isAdult,
            scoreMal = scoreMal,
            coverExtraLarge = coverExtraLarge,
            coverLarge = coverLarge,
            coverMedium = coverMedium,
            coverColor = coverColor,
            bannerImage = bannerImage,
            hasUkTranslation = hasUkTranslation,
            updatedAt = updatedAt
        )
    }
}
