package moe.GetTheNya.AniForge.core.database.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import moe.GetTheNya.AniForge.core.database.CatalogDatabaseProvider
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import moe.GetTheNya.AniForge.core.model.Anime
import moe.GetTheNya.AniForge.core.model.AnimeStaff
import moe.GetTheNya.AniForge.core.model.Ranking
import moe.GetTheNya.AniForge.core.model.Franchise
import moe.GetTheNya.AniForge.core.model.SearchFilterQuery
import moe.GetTheNya.AniForge.core.model.SortOption
import javax.inject.Inject
import javax.inject.Singleton

data class CatalogMetadata(
    val version: Long,
    val activeSlot: String
)

@Singleton
class AnimeRepository @Inject constructor(
    private val databaseProvider: CatalogDatabaseProvider,
    private val settingsProvider: SettingsProvider
) {
    /**
     * Exposes the catalog database metadata as a reactive Flow.
     */
    fun getCatalogMetadataFlow(): Flow<CatalogMetadata> = flow {
        emit(CatalogMetadata(settingsProvider.getCatalogVersion(), settingsProvider.getActiveCatalogFileName()))
        databaseProvider.swapSignal.collect {
            emit(CatalogMetadata(settingsProvider.getCatalogVersion(), settingsProvider.getActiveCatalogFileName()))
        }
    }

    /**
     * Exposes the database swap signal.
     */
    val swapSignal: Flow<Unit> get() = databaseProvider.swapSignal

    /**
     * Exposes the query result as a reactive Flow that emits new lists whenever the database is hot-swapped.
     */
    fun queryAnimeFlow(filter: SearchFilterQuery): Flow<List<Anime>> = flow {
        emit(queryAnime(filter))
        databaseProvider.swapSignal.collect {
            emit(queryAnime(filter))
        }
    }

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
     * Helper to fetch list of anime by their AniList IDs in a single batch query.
     */
    suspend fun getAnimeByIds(ids: List<Long>): List<Anime> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Anime>()
        try {
            val placeholders = ids.joinToString(",") { "?" }
            val args = ids.map { it.toString() }.toTypedArray()
            db.query("SELECT * FROM anime WHERE anilist_id IN ($placeholders)", args).use { cursor ->
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
        val coverColor = cursor.getString(cursor.getColumnIndexOrThrow("cover_color"))
        val bannerImage = cursor.getString(cursor.getColumnIndexOrThrow("banner_image"))
        val hasUkTranslation = cursor.getInt(cursor.getColumnIndexOrThrow("has_uk_translation")) != 0
        val updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))

        val airingAt = if (cursor.isNull(cursor.getColumnIndexOrThrow("airing_at"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("airing_at"))
        val airingEpisode = if (cursor.isNull(cursor.getColumnIndexOrThrow("airing_episode"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("airing_episode"))
        val trailerId = cursor.getString(cursor.getColumnIndexOrThrow("trailer_id"))
        val trailerSite = cursor.getString(cursor.getColumnIndexOrThrow("trailer_site"))
        val trailerThumbnail = cursor.getString(cursor.getColumnIndexOrThrow("trailer_thumbnail"))
        val startDateYear = if (cursor.isNull(cursor.getColumnIndexOrThrow("start_date_year"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("start_date_year"))
        val startDateMonth = if (cursor.isNull(cursor.getColumnIndexOrThrow("start_date_month"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("start_date_month"))
        val startDateDay = if (cursor.isNull(cursor.getColumnIndexOrThrow("start_date_day"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("start_date_day"))
        val popularity = if (cursor.isNull(cursor.getColumnIndexOrThrow("popularity"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("popularity"))
        val source = cursor.getString(cursor.getColumnIndexOrThrow("source"))

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
            coverColor = coverColor,
            bannerImage = bannerImage,
            hasUkTranslation = hasUkTranslation,
            updatedAt = updatedAt,
            airingAt = airingAt,
            airingEpisode = airingEpisode,
            trailerId = trailerId,
            trailerSite = trailerSite,
            trailerThumbnail = trailerThumbnail,
            startDateYear = startDateYear,
            startDateMonth = startDateMonth,
            startDateDay = startDateDay,
            popularity = popularity,
            source = source
        )
    }

    /**
     * Fetches the staff members and their roles for the given anime ID.
     */
    suspend fun getStaffForAnime(anilistId: Long): List<AnimeStaff> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<AnimeStaff>()
        try {
            db.query(
                "SELECT s.staff_id, s.full_name, s.image_large, ast.role FROM staff s " +
                "JOIN anime_staff ast ON s.staff_id = ast.staff_id " +
                "WHERE ast.anilist_id = ? " +
                "ORDER BY s.staff_id ASC, ast.role ASC",
                arrayOf(anilistId.toString())
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("staff_id")
                val nameIdx = cursor.getColumnIndexOrThrow("full_name")
                val imgIdx = cursor.getColumnIndexOrThrow("image_large")
                val roleIdx = cursor.getColumnIndexOrThrow("role")
                while (cursor.moveToNext()) {
                    list.add(
                        AnimeStaff(
                            staffId = cursor.getLong(idIdx),
                            fullName = cursor.getString(nameIdx),
                            imageLarge = if (cursor.isNull(imgIdx)) null else cursor.getString(imgIdx),
                            role = cursor.getString(roleIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Fetches the ranking charts for the given anime ID.
     */
    suspend fun getRankingsForAnime(anilistId: Long): List<Ranking> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Ranking>()
        try {
            db.query(
                "SELECT id, anilist_id, rank, context, all_time, season, year FROM rankings WHERE anilist_id = ?",
                arrayOf(anilistId.toString())
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val anilistIdIdx = cursor.getColumnIndexOrThrow("anilist_id")
                val rankIdx = cursor.getColumnIndexOrThrow("rank")
                val contextIdx = cursor.getColumnIndexOrThrow("context")
                val allTimeIdx = cursor.getColumnIndexOrThrow("all_time")
                val seasonIdx = cursor.getColumnIndexOrThrow("season")
                val yearIdx = cursor.getColumnIndexOrThrow("year")
                while (cursor.moveToNext()) {
                    list.add(
                        Ranking(
                            id = cursor.getLong(idIdx),
                            anilistId = cursor.getLong(anilistIdIdx),
                            rank = cursor.getInt(rankIdx),
                            context = cursor.getString(contextIdx),
                            allTime = cursor.getInt(allTimeIdx) != 0,
                            season = if (cursor.isNull(seasonIdx)) null else cursor.getString(seasonIdx),
                            year = if (cursor.isNull(yearIdx)) null else cursor.getInt(yearIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    /**
     * Fetches recommended anime details for the given anime ID.
     */
    suspend fun getRecommendationsForAnime(anilistId: Long): List<Anime> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Anime>()
        try {
            db.query(
                "SELECT target.* FROM anime target " +
                "JOIN anime_recommendations rec ON target.anilist_id = rec.recommended_anilist_id " +
                "WHERE rec.source_anilist_id = ? " +
                "ORDER BY target.score_mal DESC, target.updated_at DESC",
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

    /**
     * Fetches the franchise details for the given anime ID.
     */
    suspend fun getFranchiseForAnime(anilistId: Long): Franchise? = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        var franchise: Franchise? = null
        try {
            db.query(
                "SELECT f.franchise_id, f.main_anilist_id, f.name_en, f.name_uk FROM franchises f " +
                "JOIN anime_franchises af ON f.franchise_id = af.franchise_id " +
                "WHERE af.anilist_id = ?",
                arrayOf(anilistId.toString())
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("franchise_id")
                val mainIdx = cursor.getColumnIndexOrThrow("main_anilist_id")
                val enIdx = cursor.getColumnIndexOrThrow("name_en")
                val ukIdx = cursor.getColumnIndexOrThrow("name_uk")
                if (cursor.moveToNext()) {
                    franchise = Franchise(
                        franchiseId = cursor.getLong(idIdx),
                        mainAnilistId = cursor.getLong(mainIdx),
                        nameEn = if (cursor.isNull(enIdx)) null else cursor.getString(enIdx),
                        nameUk = if (cursor.isNull(ukIdx)) null else cursor.getString(ukIdx)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        franchise
    }

    /**
     * Fetches all anime belonging to a specific franchise.
     */
    suspend fun getFranchiseAnime(franchiseId: Long): List<Anime> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Anime>()
        try {
            db.query(
                "SELECT a.* FROM anime a " +
                "JOIN anime_franchises af ON a.anilist_id = af.anilist_id " +
                "WHERE af.franchise_id = ? " +
                "ORDER BY a.season_year ASC, a.updated_at ASC",
                arrayOf(franchiseId.toString())
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
