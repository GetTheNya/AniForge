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
import moe.GetTheNya.AniForge.core.model.AnimeFormat
import moe.GetTheNya.AniForge.core.model.Genre
import moe.GetTheNya.AniForge.core.model.Tag
import moe.GetTheNya.AniForge.core.model.Studio
import moe.GetTheNya.AniForge.core.model.EpisodeGroup
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
        val synonymsFlat = if (cursor.isNull(cursor.getColumnIndexOrThrow("synonyms_flat"))) null else cursor.getString(cursor.getColumnIndexOrThrow("synonyms_flat"))

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
            source = source,
            synonymsFlat = synonymsFlat
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

    suspend fun getAllGenres(): List<Genre> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Genre>()
        
        var genresTableExists = false
        try {
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='genres'").use { cursor ->
                if (cursor.moveToNext()) {
                    genresTableExists = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (genresTableExists) {
            try {
                db.query("SELECT g.slug, g.name_en, g.name_uk, COUNT(ag.anilist_id) as cnt FROM genres g LEFT JOIN anime_genres ag ON g.slug = ag.genre_slug GROUP BY g.slug ORDER BY cnt DESC, g.name_en ASC").use { cursor ->
                    val slugIdx = cursor.getColumnIndexOrThrow("slug")
                    val enIdx = cursor.getColumnIndexOrThrow("name_en")
                    val ukIdx = cursor.getColumnIndexOrThrow("name_uk")
                    while (cursor.moveToNext()) {
                        list.add(
                            Genre(
                                slug = cursor.getString(slugIdx),
                                nameEn = cursor.getString(enIdx),
                                nameUk = if (cursor.isNull(ukIdx)) null else cursor.getString(ukIdx)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                db.query("SELECT genre_slug, COUNT(anilist_id) as cnt FROM anime_genres GROUP BY genre_slug ORDER BY cnt DESC, genre_slug ASC").use { cursor ->
                    val slugIdx = cursor.getColumnIndexOrThrow("genre_slug")
                    while (cursor.moveToNext()) {
                        val slug = cursor.getString(slugIdx)
                        val displayName = slug.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        list.add(
                            Genre(
                                slug = slug,
                                nameEn = displayName,
                                nameUk = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        list
    }

    suspend fun getAllTags(): List<Tag> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Tag>()
        
        var tagsTableExists = false
        try {
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='tags'").use { cursor ->
                if (cursor.moveToNext()) {
                    tagsTableExists = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (tagsTableExists) {
            try {
                db.query("SELECT t.tag_id, t.name_en, t.name_uk, t.category, COUNT(at.anilist_id) as cnt FROM tags t LEFT JOIN anime_tags at ON t.tag_id = at.tag_id GROUP BY t.tag_id ORDER BY cnt DESC, t.name_en ASC").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("tag_id")
                    val enIdx = cursor.getColumnIndexOrThrow("name_en")
                    val ukIdx = cursor.getColumnIndexOrThrow("name_uk")
                    val catIdx = cursor.getColumnIndexOrThrow("category")
                    while (cursor.moveToNext()) {
                        list.add(
                            Tag(
                                tagId = cursor.getLong(idIdx),
                                nameEn = cursor.getString(enIdx),
                                nameUk = if (cursor.isNull(ukIdx)) null else cursor.getString(ukIdx),
                                category = if (cursor.isNull(catIdx)) null else cursor.getString(catIdx)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                db.query("SELECT tag_id, COUNT(anilist_id) as cnt FROM anime_tags GROUP BY tag_id ORDER BY cnt DESC, tag_id ASC").use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("tag_id")
                    while (cursor.moveToNext()) {
                        val tagId = cursor.getLong(idIdx)
                        list.add(
                            Tag(
                                tagId = tagId,
                                nameEn = "Tag #$tagId",
                                nameUk = null,
                                category = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        list
    }

    suspend fun getAllStudios(): List<Studio> = withContext(Dispatchers.IO) {
        val db = databaseProvider.getDatabase()
        val list = ArrayList<Studio>()
        try {
            db.query("SELECT s.studio_id, s.name, COUNT(ast.anilist_id) as cnt FROM studios s LEFT JOIN anime_studios ast ON s.studio_id = ast.studio_id GROUP BY s.studio_id ORDER BY cnt DESC, s.name ASC").use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("studio_id")
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    list.add(
                        Studio(
                            studioId = cursor.getLong(idIdx),
                            name = cursor.getString(nameIdx)
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
        
        // Score bounds selection
        val minScore = filter.minScore
        if (minScore != null) {
            whereClauses.add("anime.score_mal >= ?")
            args.add(minScore)
        }
        val maxScore = filter.maxScore
        if (maxScore != null) {
            whereClauses.add("anime.score_mal <= ?")
            args.add(maxScore)
        }

        // Episode groups
        if (filter.episodeGroups.isNotEmpty()) {
            val groupClauses = ArrayList<String>()
            for (group in filter.episodeGroups) {
                when (group) {
                    EpisodeGroup.LESS_THAN_12 -> groupClauses.add("anime.episodes < 12")
                    EpisodeGroup.BETWEEN_12_AND_18 -> groupClauses.add("(anime.episodes >= 12 AND anime.episodes <= 18)")
                    EpisodeGroup.BETWEEN_19_AND_24 -> groupClauses.add("(anime.episodes >= 19 AND anime.episodes <= 24)")
                    EpisodeGroup.GREATER_THAN_24 -> groupClauses.add("anime.episodes > 24")
                }
            }
            if (groupClauses.isNotEmpty()) {
                whereClauses.add("(" + groupClauses.joinToString(" OR ") + ")")
            }
        }
        if (filter.excludedEpisodeGroups.isNotEmpty()) {
            val groupClauses = ArrayList<String>()
            for (group in filter.excludedEpisodeGroups) {
                when (group) {
                    EpisodeGroup.LESS_THAN_12 -> groupClauses.add("anime.episodes < 12")
                    EpisodeGroup.BETWEEN_12_AND_18 -> groupClauses.add("(anime.episodes >= 12 AND anime.episodes <= 18)")
                    EpisodeGroup.BETWEEN_19_AND_24 -> groupClauses.add("(anime.episodes >= 19 AND anime.episodes <= 24)")
                    EpisodeGroup.GREATER_THAN_24 -> groupClauses.add("anime.episodes > 24")
                }
            }
            if (groupClauses.isNotEmpty()) {
                whereClauses.add("NOT (" + groupClauses.joinToString(" OR ") + ")")
            }
        }

        // Format multi-select
        if (filter.formats.isNotEmpty()) {
            val placeholders = filter.formats.joinToString(",") { "?" }
            whereClauses.add("anime.format IN ($placeholders)")
            args.addAll(filter.formats.map { it.rawValue })
        }
        if (filter.excludedFormats.isNotEmpty()) {
            val placeholders = filter.excludedFormats.joinToString(",") { "?" }
            whereClauses.add("anime.format NOT IN ($placeholders)")
            args.addAll(filter.excludedFormats.map { it.rawValue })
        }

        // Studio selection
        if (filter.studios.isNotEmpty()) {
            val placeholders = filter.studios.joinToString(",") { "?" }
            whereClauses.add("anime.anilist_id IN (SELECT anilist_id FROM anime_studios WHERE studio_id IN ($placeholders))")
            args.addAll(filter.studios)
        }
        if (filter.excludedStudios.isNotEmpty()) {
            val placeholders = filter.excludedStudios.joinToString(",") { "?" }
            whereClauses.add("anime.anilist_id NOT IN (SELECT anilist_id FROM anime_studios WHERE studio_id IN ($placeholders))")
            args.addAll(filter.excludedStudios)
        }

        // Localization toggle
        if (filter.hasUkTranslation == true) {
            whereClauses.add("anime.has_uk_translation = 1")
        }

        // Tracking status negation and inclusion (Direct ID injection as requested for primitives)
        if (filter.trackingStatuses.isNotEmpty()) {
            if (filter.trackingStatusIds.isNotEmpty()) {
                whereClauses.add("anime.anilist_id IN (${filter.trackingStatusIds.joinToString(",")})")
            } else {
                whereClauses.add("0 = 1")
            }
        }
        if (filter.excludedTrackingStatuses.isNotEmpty()) {
            if (filter.excludedTrackingStatusIds.isNotEmpty()) {
                whereClauses.add("anime.anilist_id NOT IN (${filter.excludedTrackingStatusIds.joinToString(",")})")
            }
        }

        // Genre inclusion (intersecting: must match ALL)
        if (filter.genres.isNotEmpty()) {
            for (genre in filter.genres) {
                whereClauses.add("anime.anilist_id IN (SELECT anilist_id FROM anime_genres WHERE genre_slug = ?)")
                args.add(genre)
            }
        }
        // Genre exclusion (negation: must NOT match any of them)
        if (filter.excludedGenres.isNotEmpty()) {
            val placeholders = filter.excludedGenres.joinToString(",") { "?" }
            whereClauses.add("anime.anilist_id NOT IN (SELECT anilist_id FROM anime_genres WHERE genre_slug IN ($placeholders))")
            args.addAll(filter.excludedGenres)
        }

        // Tag inclusion (intersecting: must match ALL)
        if (filter.tags.isNotEmpty()) {
            for (tagId in filter.tags) {
                whereClauses.add("anime.anilist_id IN (SELECT anilist_id FROM anime_tags WHERE tag_id = ?)")
                args.add(tagId)
            }
        }
        // Tag exclusion (negation: must NOT match any of them)
        if (filter.excludedTags.isNotEmpty()) {
            val placeholders = filter.excludedTags.joinToString(",") { "?" }
            whereClauses.add("anime.anilist_id NOT IN (SELECT anilist_id FROM anime_tags WHERE tag_id IN ($placeholders))")
            args.addAll(filter.excludedTags)
        }
        
        if (whereClauses.isNotEmpty()) {
            queryBuilder.append(" WHERE ")
            queryBuilder.append(whereClauses.joinToString(" AND "))
        }
        
        // Ordering
        when (filter.sortBy) {
            SortOption.SCORE -> queryBuilder.append(" ORDER BY score_mal DESC")
            SortOption.SCORE_ASC -> queryBuilder.append(" ORDER BY score_mal ASC")
            SortOption.YEAR_DESC -> queryBuilder.append(" ORDER BY season_year DESC, updated_at DESC")
            SortOption.YEAR_ASC -> queryBuilder.append(" ORDER BY season_year ASC, updated_at ASC")
            SortOption.TITLE -> queryBuilder.append(" ORDER BY title_romaji ASC")
            SortOption.TITLE_DESC -> queryBuilder.append(" ORDER BY title_romaji DESC")
            SortOption.POPULARITY -> queryBuilder.append(" ORDER BY popularity DESC")
            SortOption.POPULARITY_ASC -> queryBuilder.append(" ORDER BY popularity ASC")
            SortOption.START_DATE_DESC -> queryBuilder.append(" ORDER BY start_date_year DESC, start_date_month DESC, start_date_day DESC")
            SortOption.START_DATE_ASC -> queryBuilder.append(" ORDER BY start_date_year ASC, start_date_month ASC, start_date_day ASC")
            SortOption.EPISODES_DESC -> queryBuilder.append(" ORDER BY episodes DESC")
            SortOption.EPISODES_ASC -> queryBuilder.append(" ORDER BY episodes ASC")
        }
        
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toArray())
    }
}
