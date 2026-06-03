package moe.GetTheNya.AniForge.core.database.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.CatalogDatabaseProvider
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimeRepository @Inject constructor(
    private val databaseProvider: CatalogDatabaseProvider
) {
    /**
     * Executes a dynamic FTS5 search and multi-layered filter query on a dedicated background thread.
     */
    suspend fun queryAnime(filter: SearchFilterQuery): List<Anime> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val rawQuery = buildSqlFilterQuery(filter)
        val list = ArrayList<Anime>()
        try {
            db.query(rawQuery).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursorToAnime(cursor))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Helper to fetch an anime by its AniList ID.
     */
    suspend fun getAnimeById(id: Long): Anime? = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        var anime: Anime? = null
        try {
            db.query("SELECT * FROM anime WHERE anilist_id = ?", arrayOf(id.toString())).use { cursor ->
                if (cursor.moveToNext()) {
                    anime = cursorToAnime(cursor)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        anime
    }

    /**
     * Fetches screenshot URLs for the given anime ID.
     */
    suspend fun getScreenshots(anilistId: Long): List<String> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<String>()
        try {
            db.query(
                "SELECT image_url FROM screenshots WHERE anilist_id = ?",
                arrayOf(anilistId.toString())
            ).use { cursor ->
                val index = cursor.getColumnIndex("image_url")
                if (index >= 0) {
                    while (cursor.moveToNext()) {
                        list.add(cursor.getString(index))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Fetches related anime details sorted by release date.
     */
    suspend fun getRelations(anilistId: Long): List<Anime> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Anime>()
        try {
            db.query(
                "SELECT target.* FROM anime target " +
                "JOIN relations ON target.anilist_id = relations.target_anilist_id " +
                "WHERE relations.source_anilist_id = ? " +
                "ORDER BY target.season_year ASC, target.updated_at ASC",
                arrayOf(anilistId.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursorToAnime(cursor))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    private fun cursorToAnime(cursor: android.database.Cursor): Anime {
        val anilistId = cursor.getLong(cursor.getColumnIndexOrThrow("anilist_id"))
        val malId = if (cursor.isNull(cursor.getColumnIndexOrThrow("mal_id"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("mal_id"))
        val titleUk = cursor.getString(cursor.getColumnIndexOrThrow("title_uk"))
        val titleRomaji = cursor.getString(cursor.getColumnIndexOrThrow("title_romaji"))
        val titleEn = cursor.getString(cursor.getColumnIndexOrThrow("title_en"))
        val descriptionUk = cursor.getString(cursor.getColumnIndexOrThrow("description_uk"))
        val descriptionEn = cursor.getString(cursor.getColumnIndexOrThrow("description_en"))
        val format = cursor.getString(cursor.getColumnIndexOrThrow("format"))
        val status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
        val episodes = if (cursor.isNull(cursor.getColumnIndexOrThrow("episodes"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("episodes"))
        val duration = if (cursor.isNull(cursor.getColumnIndexOrThrow("duration"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
        val seasonYear = if (cursor.isNull(cursor.getColumnIndexOrThrow("season_year"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("season_year"))
        val season = cursor.getString(cursor.getColumnIndexOrThrow("season"))
        val isAdult = cursor.getInt(cursor.getColumnIndexOrThrow("is_adult")) != 0
        val scoreMal = if (cursor.isNull(cursor.getColumnIndexOrThrow("score_mal"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("score_mal"))
        val coverExtraLarge = cursor.getString(cursor.getColumnIndexOrThrow("cover_extra_large"))
        val coverLarge = cursor.getString(cursor.getColumnIndexOrThrow("cover_large"))
        val coverMedium = cursor.getString(cursor.getColumnIndexOrThrow("cover_medium"))
        val coverColor = cursor.getString(cursor.getColumnIndexOrThrow("cover_color"))
        val bannerImage = cursor.getString(cursor.getColumnIndexOrThrow("banner_image"))
        val hasUkTranslation = cursor.getInt(cursor.getColumnIndexOrThrow("has_uk_translation")) != 0
        val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))

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

    /**
     * Core dynamic SQLite statement builder. Intersects multiple criteria dynamically.
     */
    private fun buildSqlFilterQuery(filter: SearchFilterQuery): SimpleSQLiteQuery {
        val queryBuilder = StringBuilder()
        val args = ArrayList<Any>()
        
        queryBuilder.append("SELECT anime.* FROM anime")
        
        // FTS5 MATCH join
        val hasText = filter.textQuery.isNotBlank()
        if (hasText) {
            queryBuilder.append(" JOIN anime_search ON anime.anilist_id = anime_search.rowid")
        }
        
        val whereClauses = ArrayList<String>()
        
        if (hasText) {
            whereClauses.add("anime_search MATCH ?")
            // Transform query for SQLite FTS prefix match: e.g. "shingeki no" -> "shingeki* no*"
            val sanitizedQuery = filter.textQuery.trim()
                .replace("'", "''")
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
            args.add(sanitizedQuery)
        }
        
        // Genre Intersections
        if (filter.genres.isNotEmpty()) {
            for (genre in filter.genres) {
                whereClauses.add("anime.anilist_id IN (SELECT anilist_id FROM anime_genres WHERE genre_slug = ?)")
                args.add(genre)
            }
        }
        
        // Studio Intersections
        if (filter.studios.isNotEmpty()) {
            for (studioId in filter.studios) {
                whereClauses.add("anime.anilist_id IN (SELECT anilist_id FROM anime_studios WHERE studio_id = ?)")
                args.add(studioId)
            }
        }
        
        // Tag Intersections
        if (filter.tags.isNotEmpty()) {
            for (tagId in filter.tags) {
                whereClauses.add("anime.anilist_id IN (SELECT anilist_id FROM anime_tags WHERE tag_id = ?)")
                args.add(tagId)
            }
        }
        
        if (whereClauses.isNotEmpty()) {
            queryBuilder.append(" WHERE ")
            queryBuilder.append(whereClauses.joinToString(" AND "))
        }
        
        // Ordering
        when (filter.sortBy) {
            SortOption.SCORE -> queryBuilder.append(" ORDER BY score_mal DESC")
            SortOption.YEAR_DESC -> queryBuilder.append(" ORDER BY season_year DESC, updated_at DESC")
            SortOption.YEAR_ASC -> queryBuilder.append(" ORDER BY season_year ASC, updated_at ASC")
            SortOption.TITLE -> queryBuilder.append(" ORDER BY title_romaji ASC")
        }
        
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toArray())
    }
}
